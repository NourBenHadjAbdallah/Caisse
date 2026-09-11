package com.caisse.controller;

import com.caisse.model.User;
import com.caisse.service.AuthService;
import com.caisse.state.AppState;
import com.caisse.util.SceneManager;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressIndicator progressIndicator;

    private final AuthService authService = new AuthService();

    // ============================================================
    // REMEMBER LAST EMAIL
    // ============================================================

    private static final String APP_FOLDER = ".caisse-pos";
    private static final String SETTINGS_FILE = "settings.properties";
    private static final String LAST_EMAIL = "last.email";

    private final Path settingsPath =
            Paths.get(
                    System.getProperty("user.home"),
                    APP_FOLDER,
                    SETTINGS_FILE
            );

    // ============================================================
    // INITIALIZE
    // ============================================================

    @FXML
    public void initialize() {

        // Load the last email used
        loadLastEmail();

        // Put cursor in password field if email already exists
        Platform.runLater(() -> {

            if (!emailField.getText().isBlank()) {
                passwordField.requestFocus();
            } else {
                emailField.requestFocus();
            }

        });

        statusLabel.setText("");
    }

    // ============================================================
    // LOGIN
    // ============================================================

    @FXML
    public void onLogin() {

        if (progressIndicator.isVisible()) {
            return;
        }

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // --------------------------------------------------------
        // Validation
        // --------------------------------------------------------

        if (email.isBlank()) {
            showError("Veuillez saisir votre adresse e-mail.");
            emailField.requestFocus();
            return;
        }

        if (password.isBlank()) {
            showError("Veuillez saisir votre mot de passe.");
            passwordField.requestFocus();
            return;
        }

        // --------------------------------------------------------
        // Save email immediately
        // --------------------------------------------------------

        saveLastEmail(email);

        // --------------------------------------------------------
        // UI loading state
        // --------------------------------------------------------

        setLoading(true);

        statusLabel.setText("Connexion en cours...");

        // --------------------------------------------------------
        // Login in background thread
        // --------------------------------------------------------

        Task<User> loginTask = new Task<>() {

            @Override
            protected User call()
                    throws IOException, InterruptedException {

                return authService.login(
                        email,
                        password
                );
            }
        };

        // --------------------------------------------------------
        // SUCCESS
        // --------------------------------------------------------

        loginTask.setOnSucceeded(event -> {

            User user = loginTask.getValue();

            // Save authenticated user in application state
            AppState.getInstance().setCurrentUser(user);

            setLoading(false);

            statusLabel.setText("");

            // Open main menu
            SceneManager.show(
                    "/fxml/main_menu.fxml",
                    "Menu principal"
            );
        });

        // --------------------------------------------------------
        // ERROR
        // --------------------------------------------------------

        loginTask.setOnFailed(event -> {

            setLoading(false);

            Throwable exception =
                    loginTask.getException();

            String message =
                    exception != null
                            ? exception.getMessage()
                            : "Erreur de connexion.";

            showError(message);
        });

        Thread loginThread =
                new Thread(loginTask);

        loginThread.setDaemon(true);
        loginThread.start();
    }

    // ============================================================
    // LOAD LAST EMAIL
    // ============================================================

    private void loadLastEmail() {

        try {

            if (!Files.exists(settingsPath)) {
                return;
            }

            Properties properties =
                    new Properties();

            try (InputStream input =
                         Files.newInputStream(settingsPath)) {

                properties.load(input);
            }

            String lastEmail =
                    properties.getProperty(
                            LAST_EMAIL,
                            ""
                    );

            if (lastEmail != null &&
                    !lastEmail.isBlank()) {

                emailField.setText(
                        lastEmail.trim()
                );
            }

        } catch (Exception e) {

            // Do not prevent login if settings cannot be read.
            System.err.println(
                    "Impossible de charger les paramètres : "
                            + e.getMessage()
            );
        }
    }

    // ============================================================
    // SAVE LAST EMAIL
    // ============================================================

    private void saveLastEmail(String email) {

        try {

            Path directory =
                    settingsPath.getParent();

            if (directory != null) {
                Files.createDirectories(directory);
            }

            Properties properties =
                    new Properties();

            // Load existing settings first
            if (Files.exists(settingsPath)) {

                try (InputStream input =
                             Files.newInputStream(settingsPath)) {

                    properties.load(input);
                }
            }

            properties.setProperty(
                    LAST_EMAIL,
                    email.trim()
            );

            try (OutputStream output =
                         Files.newOutputStream(settingsPath)) {

                properties.store(
                        output,
                        "Caisse POS Settings"
                );
            }

        } catch (Exception e) {

            // Saving the email should never prevent login.
            System.err.println(
                    "Impossible de sauvegarder l'email : "
                            + e.getMessage()
            );
        }
    }

    // ============================================================
    // UI HELPERS
    // ============================================================

    private void setLoading(boolean loading) {

        progressIndicator.setVisible(loading);

        emailField.setDisable(loading);
        passwordField.setDisable(loading);
    }

    private void showError(String message) {

        statusLabel.setText(
                message != null
                        ? message
                        : "Erreur de connexion."
        );
    }
}