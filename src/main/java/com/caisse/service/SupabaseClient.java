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

public class SupabaseClient {

    private static final SupabaseClient INSTANCE = new SupabaseClient();

    public static SupabaseClient getInstance() {
        return INSTANCE;
    }

    /**
     * Set this to true only for local debugging. When true, request/response
     * bodies are logged to stdout, which WILL include access tokens and
     * customer/sales data. Never enable this in a packaged/production build.
     */
    private static final boolean DEBUG_LOGGING = false;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String baseUrl;
    private final String anonKey;

    private String accessToken;
    private String refreshToken;
    private String currentUserId;

    private SupabaseClient() {
        baseUrl = AppConfig.supabaseUrl();
        anonKey = AppConfig.supabaseAnonKey();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Supabase URL is missing.");
        }

        if (anonKey == null || anonKey.isBlank()) {
            throw new IllegalStateException("Supabase anon key is missing.");
        }
    }

    // ============================================================
    // SESSION
    // ============================================================

    public void setSession(String accessToken, String refreshToken, String userId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.currentUserId = userId;
    }

    public void clearSession() {
        this.accessToken = null;
        this.refreshToken = null;
        this.currentUserId = null;
    }

    public String currentUserId() {
        return currentUserId;
    }

    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isBlank();
    }

    // ============================================================
    // AUTHENTICATION
    // ============================================================

    public JSONObject signInWithPassword(String email, String password)
            throws IOException, InterruptedException {

        if (email == null || email.isBlank()) {
            throw new IOException("Veuillez saisir votre adresse e-mail.");
        }

        if (password == null || password.isBlank()) {
            throw new IOException("Veuillez saisir votre mot de passe.");
        }

        email = email.trim();

        JSONObject body = new JSONObject()
                .put("email", email)
                .put("password", password);

        String url = baseUrl + "/auth/v1/token?grant_type=password";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();

        // NEVER log responseBody here: it contains the access_token and
        // refresh_token in plaintext. Only log the status code.
        debugLog("Supabase Auth status: " + response.statusCode());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {

            String message = extractSupabaseError(responseBody);

            throw new IOException(
                    "Échec de connexion : " + message
            );
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException(
                    "Supabase a retourné une réponse vide."
            );
        }

        JSONObject json;

        try {
            json = new JSONObject(responseBody);
        } catch (Exception e) {
            throw new IOException(
                    "Réponse Supabase invalide."
            );
        }

        if (!json.has("access_token")) {
            throw new IOException(
                    "Aucun access token reçu depuis Supabase."
            );
        }

        if (!json.has("user") || json.isNull("user")) {
            throw new IOException(
                    "Aucun utilisateur reçu depuis Supabase."
            );
        }

        return json;
    }

    /**
     * Exchanges the stored refresh token for a fresh access token, without
     * requiring the user to log in again. Call this proactively before the
     * access token expires (Supabase JWTs default to a 1-hour lifetime), or
     * reactively after receiving a 401 from a REST call.
     */
    public JSONObject refreshSession() throws IOException, InterruptedException {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IOException("Aucune session à rafraîchir.");
        }

        JSONObject body = new JSONObject().put("refresh_token", refreshToken);
        String url = baseUrl + "/auth/v1/token?grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        debugLog("Supabase token refresh status: " + response.statusCode());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            clearSession();
            throw new IOException("Session expirée. Veuillez vous reconnecter.");
        }

        JSONObject json = new JSONObject(response.body());
        String newAccessToken = json.optString("access_token", null);
        String newRefreshToken = json.optString("refresh_token", refreshToken);

        if (newAccessToken == null) {
            clearSession();
            throw new IOException("Échec du rafraîchissement de session.");
        }

        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;

        return json;
    }

    // ============================================================
    // REST SELECT
    // ============================================================

    public JSONArray select(String table, String query)
            throws IOException, InterruptedException {

        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name is required.");
        }

        String url = baseUrl + "/rest/v1/" + table;

        if (query != null && !query.isBlank()) {
            url += "?" + query;
        }

        HttpRequest request = restRequestBuilder(url)
                .GET()
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        debugLog("Supabase SELECT [" + table + "] status: " + response.statusCode());

        checkError(response);

        String body = response.body();

        if (body == null || body.isBlank()) {
            return new JSONArray();
        }

        try {
            return new JSONArray(body);
        } catch (Exception e) {
            throw new IOException("Réponse SELECT invalide.", e);
        }
    }

    // ============================================================
    // INSERT
    // ============================================================

    public JSONArray insert(String table, JSONObject row)
            throws IOException, InterruptedException {

        String url = baseUrl + "/rest/v1/" + table;

        HttpRequest request = restRequestBuilder(url)
                .header("Prefer", "return=representation")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                row.toString()
                        )
                )
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        debugLog("Supabase INSERT [" + table + "] status: " + response.statusCode());

        checkError(response);

        if (response.body() == null || response.body().isBlank()) {
            return new JSONArray();
        }

        return new JSONArray(response.body());
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public JSONArray update(
            String table,
            String query,
            JSONObject changes
    ) throws IOException, InterruptedException {

        String url =
                baseUrl +
                "/rest/v1/" +
                table +
                "?" +
                query;

        HttpRequest request = restRequestBuilder(url)
                .header("Prefer", "return=representation")
                .method(
                        "PATCH",
                        HttpRequest.BodyPublishers.ofString(
                                changes.toString()
                        )
                )
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        debugLog("Supabase UPDATE [" + table + "] status: " + response.statusCode());

        checkError(response);

        if (response.body() == null || response.body().isBlank()) {
            return new JSONArray();
        }

        return new JSONArray(response.body());
    }

    // ============================================================
    // RPC
    // ============================================================

    public JSONObject rpc(
            String functionName,
            Map<String, Object> params
    ) throws IOException, InterruptedException {

        String url =
                baseUrl +
                "/rest/v1/rpc/" +
                functionName;

        JSONObject body = new JSONObject(params);

        HttpRequest request = restRequestBuilder(url)
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                body.toString()
                        )
                )
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        debugLog("Supabase RPC [" + functionName + "] status: " + response.statusCode());

        checkError(response);

        String responseBody = response.body();

        if (responseBody == null || responseBody.isBlank()) {
            return new JSONObject();
        }

        responseBody = responseBody.trim();

        if (responseBody.startsWith("[")) {

            JSONArray array = new JSONArray(responseBody);

            if (array.isEmpty()) {
                return new JSONObject();
            }

            return array.getJSONObject(0);
        }

        return new JSONObject(responseBody);
    }

    // ============================================================
    // REQUEST BUILDER
    // ============================================================

    private HttpRequest.Builder restRequestBuilder(String url) {

        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("apikey", anonKey)
                        .header("Content-Type", "application/json");

        if (accessToken != null && !accessToken.isBlank()) {

            builder.header(
                    "Authorization",
                    "Bearer " + accessToken
            );

        } else {

            builder.header(
                    "Authorization",
                    "Bearer " + anonKey
            );
        }

        return builder;
    }

    // ============================================================
    // ERROR HANDLING
    // ============================================================

    private void checkError(HttpResponse<String> response)
            throws IOException {

        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return;
        }

        String body = response.body();

        String message = extractSupabaseError(body);

        throw new IOException(
                "Supabase [" +
                        status +
                        "]: " +
                        message
        );
    }

    private String extractSupabaseError(String body) {

        if (body == null || body.isBlank()) {
            return "Erreur inconnue.";
        }

        try {

            JSONObject json = new JSONObject(body);

            String message =
                    json.optString("error_description", "");

            if (!message.isBlank()) {
                return message;
            }

            message =
                    json.optString("message", "");

            if (!message.isBlank()) {
                return message;
            }

            message =
                    json.optString("msg", "");

            if (!message.isBlank()) {
                return message;
            }

            message =
                    json.optString("error", "");

            if (!message.isBlank()) {
                return message;
            }

        } catch (Exception ignored) {
        }

        return body;
    }

    // ============================================================
    // DEBUG LOGGING (status codes only, never bodies/tokens)
    // ============================================================

    private void debugLog(String message) {
        if (DEBUG_LOGGING) {
            System.out.println(message);
        }
    }
}