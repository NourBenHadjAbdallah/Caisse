package com.caisse.service;

import com.caisse.model.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class AuthService {

    private final SupabaseClient client = SupabaseClient.getInstance();

    /**
     * Signs in against Supabase Auth, then loads the matching row in the
     * app's own "users" table (which holds the cashier/vendor code).
     */
    public User login(String email, String password) throws IOException, InterruptedException {
        JSONObject authResp = client.signInWithPassword(email, password);
        String accessToken = authResp.getString("access_token");
        String userId = authResp.getJSONObject("user").getString("id");
        client.setSession(accessToken, userId);

        JSONArray rows = client.select("users", "select=*&id=eq." + userId);
        if (rows.isEmpty()) {
            // Auth succeeded but no app profile/cashier code exists yet.
            throw new IOException("Aucun profil caissier trouvé pour cet utilisateur. Contactez l'administrateur.");
        }
        JSONObject row = rows.getJSONObject(0);
        String cashierCode = row.optString("cashier_code", row.optString("email", email));
        return new User(userId, email, cashierCode);
    }

    public void logout() {
        client.clearSession();
    }
}
