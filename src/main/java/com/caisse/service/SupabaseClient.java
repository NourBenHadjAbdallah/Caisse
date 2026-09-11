package com.caisse.service;

import com.caisse.config.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Thin wrapper around Supabase's REST (PostgREST) and Auth (GoTrue) HTTP APIs.
 * Every request carries the anon key as "apikey" and, once signed in, the
 * user's access token as the Bearer "Authorization" header so that Postgres
 * Row Level Security policies apply per-user.
 */
public class SupabaseClient {

    private static final SupabaseClient INSTANCE = new SupabaseClient();
    public static SupabaseClient getInstance() { return INSTANCE; }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String baseUrl = AppConfig.supabaseUrl();
    private final String anonKey = AppConfig.supabaseAnonKey();

    private String accessToken;   // set after successful login
    private String currentUserId; // Supabase auth user id (uuid)

    private SupabaseClient() {}

    public void setSession(String accessToken, String userId) {
        this.accessToken = accessToken;
        this.currentUserId = userId;
    }

    public void clearSession() {
        this.accessToken = null;
        this.currentUserId = null;
    }

    public String currentUserId() { return currentUserId; }
    public boolean isAuthenticated() { return accessToken != null; }

    // ---------------------------------------------------------------
    // Auth (GoTrue) — email/password sign in
    // ---------------------------------------------------------------

    /** Returns the raw auth response JSON (contains access_token, user, etc.). */
    public JSONObject signInWithPassword(String email, String password) throws IOException, InterruptedException {
        JSONObject body = new JSONObject().put("email", email).put("password", password);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/v1/token?grant_type=password"))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(resp.body());
        if (resp.statusCode() >= 400) {
            String msg = json.optString("error_description", json.optString("msg", "Échec de connexion"));
            throw new IOException(msg);
        }
        return json;
    }

    // ---------------------------------------------------------------
    // Generic PostgREST helpers
    // ---------------------------------------------------------------

    /** GET {baseUrl}/rest/v1/{table}?{query}  e.g. query = "select=*&status=eq.OPEN" */
    public JSONArray select(String table, String query) throws IOException, InterruptedException {
        String url = baseUrl + "/rest/v1/" + table + (query == null || query.isEmpty() ? "" : "?" + query);
        HttpRequest req = restRequestBuilder(url)
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkError(resp);
        String body = resp.body();
        return body.isBlank() ? new JSONArray() : new JSONArray(body);
    }

    /** POST (insert) a row; returns the inserted row(s). */
    public JSONArray insert(String table, JSONObject row) throws IOException, InterruptedException {
        HttpRequest req = restRequestBuilder(baseUrl + "/rest/v1/" + table)
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(row.toString()))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkError(resp);
        return new JSONArray(resp.body());
    }

    /** PATCH (update) rows matching query; returns the updated row(s). */
    public JSONArray update(String table, String query, JSONObject changes) throws IOException, InterruptedException {
        String url = baseUrl + "/rest/v1/" + table + "?" + query;
        HttpRequest req = restRequestBuilder(url)
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(changes.toString()))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkError(resp);
        return new JSONArray(resp.body());
    }

    /** Call a Postgres function exposed via PostgREST RPC (used for atomic, server-enforced operations). */
    public JSONObject rpc(String functionName, Map<String, Object> params) throws IOException, InterruptedException {
        JSONObject body = new JSONObject(params);
        HttpRequest req = restRequestBuilder(baseUrl + "/rest/v1/rpc/" + functionName)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkError(resp);
        String respBody = resp.body();
        if (respBody.isBlank()) return new JSONObject();
        // RPC may return an array or a single object depending on the function's return type
        return respBody.trim().startsWith("[")
                ? new JSONArray(respBody).optJSONObject(0) == null ? new JSONObject() : new JSONArray(respBody).getJSONObject(0)
                : new JSONObject(respBody);
    }

    private HttpRequest.Builder restRequestBuilder(String url) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json");
        if (accessToken != null) {
            b.header("Authorization", "Bearer " + accessToken);
        } else {
            b.header("Authorization", "Bearer " + anonKey);
        }
        return b;
    }

    private void checkError(HttpResponse<String> resp) throws IOException {
        if (resp.statusCode() >= 400) {
            String detail = resp.body();
            try {
                JSONObject err = new JSONObject(detail);
                detail = err.optString("message", detail);
            } catch (Exception ignored) { /* not JSON, keep raw body */ }
            throw new IOException("Supabase [" + resp.statusCode() + "]: " + detail);
        }
    }
}
