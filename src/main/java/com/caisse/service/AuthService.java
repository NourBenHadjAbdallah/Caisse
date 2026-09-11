package com.caisse.service;

import com.caisse.model.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class AuthService {

    private final SupabaseClient client =
            SupabaseClient.getInstance();

    /**
     * Authenticate the user with Supabase Auth,
     * then load the corresponding cashier profile
     * from public.users.
     */
    public User login(
            String email,
            String password
    ) throws IOException, InterruptedException {

        // --------------------------------------------------------
        // 1. Authenticate with Supabase Auth
        // --------------------------------------------------------

        JSONObject authResponse =
                client.signInWithPassword(
                        email,
                        password
                );

        // --------------------------------------------------------
        // 2. Extract authentication information
        // --------------------------------------------------------

        String accessToken =
                authResponse.optString(
                        "access_token",
                        ""
                );

        if (accessToken.isBlank()) {
            throw new IOException(
                    "Connexion réussie mais aucun token reçu."
            );
        }

        JSONObject authUser =
                authResponse.optJSONObject("user");

        if (authUser == null) {
            throw new IOException(
                    "Connexion réussie mais aucun utilisateur reçu."
            );
        }

        String userId =
                authUser.optString(
                        "id",
                        ""
                );

        if (userId.isBlank()) {
            throw new IOException(
                    "Impossible de récupérer l'identifiant utilisateur."
            );
        }

        String userEmail =
                authUser.optString(
                        "email",
                        email
                );

        System.out.println("=================================");
        System.out.println("AUTHENTICATION SUCCESS");
        System.out.println("User ID: " + userId);
        System.out.println("Email: " + userEmail);
        System.out.println("=================================");

        // --------------------------------------------------------
        // 3. Save Supabase session
        // --------------------------------------------------------

        client.setSession(
                accessToken,
                userId
        );

        // --------------------------------------------------------
        // 4. Load cashier profile
        // --------------------------------------------------------

        JSONArray rows =
                client.select(
                        "users",
                        "select=*&id=eq." + userId
                );

        // --------------------------------------------------------
        // 5. Check cashier profile
        // --------------------------------------------------------

        if (rows.isEmpty()) {

            // Auth succeeded but application profile doesn't exist.

            client.clearSession();

            throw new IOException(
                    "Connexion réussie, mais aucun profil " +
                    "caissier n'a été trouvé.\n\n" +
                    "Utilisateur : " +
                    userEmail +
                    "\n\n" +
                    "ID : " +
                    userId
            );
        }

        // --------------------------------------------------------
        // 6. Get profile
        // --------------------------------------------------------

        JSONObject row =
                rows.getJSONObject(0);

        String cashierCode =
                row.optString(
                        "cashier_code",
                        ""
                );

        if (cashierCode.isBlank()) {

            cashierCode =
                    row.optString(
                            "email",
                            userEmail
                    );
        }

        // --------------------------------------------------------
        // 7. Create application user
        // --------------------------------------------------------

        User user =
                new User(
                        userId,
                        userEmail,
                        cashierCode
                );

        System.out.println(
                "Cashier profile loaded: "
                        + cashierCode
        );

        return user;
    }

    /**
     * Logout and remove the local Supabase session.
     */
    public void logout() {
        client.clearSession();
    }
}