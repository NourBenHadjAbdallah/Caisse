package com.caisse.controller;

import com.caisse.model.Journey;
import com.caisse.model.User;
import com.caisse.service.AuthService;
import com.caisse.service.JourneyService;
import com.caisse.state.AppState;
import com.caisse.util.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final AuthService authService = new AuthService();
    private final JourneyService journeyService = new JourneyService();

    @FXML
    public void onLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        statusLabel.setText("");

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Veuillez saisir votre email et votre mot de passe.");
            return;
        }

        setBusy(true);
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                User user = authService.login(email, password);
                Journey open = journeyService.findOpenJourney(user.getId());
                AppState.getInstance().setCurrentUser(user);
                AppState.getInstance().setCurrentJourney(open);
                return user;
            }
        };
        loginTask.setOnSucceeded(e -> {
            setBusy(false);
            SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
        });
        loginTask.setOnFailed(e -> {
            setBusy(false);
            Throwable ex = loginTask.getException();
            statusLabel.setText(ex != null ? ex.getMessage() : "Échec de connexion.");
        });
        new Thread(loginTask, "login-task").start();
    }

    private void setBusy(boolean busy) {
        progressIndicator.setVisible(busy);
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
    }
}
