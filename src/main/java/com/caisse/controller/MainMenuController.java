package com.caisse.controller;

import com.caisse.service.AuthService;
import com.caisse.state.AppState;
import com.caisse.state.JourneyState;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainMenuController {

    @FXML private Label cashierLabel;
    @FXML private Label journeyBadge;
    @FXML private Button ticketsButton;
    @FXML private Button returnsButton;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        AppState state = AppState.getInstance();
        if (state.getCurrentUser() != null) {
            cashierLabel.setText(state.getCurrentUser().getCashierCode());
        }
        refreshBadge();
        // Keep buttons in sync with journey state wherever it changes (opened/closed elsewhere too).
        state.journeyStateProperty().addListener((obs, oldV, newV) -> refreshBadge());
    }

    private void refreshBadge() {
        boolean open = AppState.getInstance().hasOpenJourney();
        journeyBadge.setText(open ? "Journée: OUVERTE" : "Journée: FERMÉE");
        journeyBadge.getStyleClass().removeAll("badge-open", "badge-closed");
        journeyBadge.getStyleClass().add(open ? "badge-open" : "badge-closed");
        ticketsButton.setDisable(!open);
        returnsButton.setDisable(!open);
    }

    @FXML
    public void onJourney() {
        SceneManager.show("/fxml/journey_menu.fxml", "Journée");
    }

    @FXML
    public void onTickets() {
        // Defense in depth: verify again even though the button should already be disabled.
        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "Veuillez ouvrir une journée avant de créer un ticket.");
            return;
        }
        SceneManager.show("/fxml/tickets.fxml", "Tickets");
    }

    @FXML
    public void onReturns() {
        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "Veuillez ouvrir une journée avant de traiter un retour.");
            return;
        }
        SceneManager.show("/fxml/returns.fxml", "Retours");
    }

    @FXML
    public void onStock() {
        SceneManager.show("/fxml/stock.fxml", "Stock");
    }

    @FXML
    public void onReports() {
        SceneManager.show("/fxml/reports.fxml", "Rapports");
    }

    @FXML
    public void onLogout() {
        authService.logout();
        AppState.getInstance().reset();
        SceneManager.show("/fxml/login.fxml", "Connexion");
    }
}
