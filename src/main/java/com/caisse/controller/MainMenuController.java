package com.caisse.controller;

import com.caisse.model.Journey;
import com.caisse.service.AuthService;
import com.caisse.service.JourneyService;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class MainMenuController {

    @FXML private Label cashierLabel;
    @FXML private Label journeyBadge;

    @FXML private Button ticketsButton;
    @FXML private Button returnsButton;

    @FXML private Button journeyToggleButton;
    @FXML private VBox journeySubmenu;
    @FXML private Button openJourneyButton;
    @FXML private Button closeJourneyButton;
    @FXML private ProgressIndicator journeyProgress;

    private final AuthService authService = new AuthService();
    private final JourneyService journeyService = new JourneyService();

    private boolean journeySubmenuExpanded = false;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        AppState state = AppState.getInstance();

        if (state.getCurrentUser() != null) {
            cashierLabel.setText(state.getCurrentUser().getCashierCode());
        }

        journeySubmenu.setVisible(false);
        journeySubmenu.setManaged(false);

        refreshMenu();

        // Automatically refresh menu when journey state changes.
        state.journeyStateProperty().addListener((observable, oldValue, newValue) -> refreshMenu());
    }


    // =========================================================
    // JOURNEY SUBMENU TOGGLE
    // =========================================================

    @FXML
    public void onToggleJourneyMenu() {
        journeySubmenuExpanded = !journeySubmenuExpanded;

        journeySubmenu.setVisible(journeySubmenuExpanded);
        journeySubmenu.setManaged(journeySubmenuExpanded);

        journeyToggleButton.setText(journeySubmenuExpanded ? "Journée   ▴" : "Journée   ▾");
    }


    // =========================================================
    // REFRESH MENU STATE
    // =========================================================

    private void refreshMenu() {

        boolean open = AppState.getInstance().hasOpenJourney();

        journeyBadge.setText(open ? "Journée : OUVERTE" : "Journée : FERMÉE");

        journeyBadge.getStyleClass().removeAll("badge-open", "badge-closed");
        journeyBadge.getStyleClass().add(open ? "badge-open" : "badge-closed");

        // Tickets/Retours stay visible, just disabled when no journey is open.
        ticketsButton.setDisable(!open);
        returnsButton.setDisable(!open);

        boolean openEnabled = AppState.getInstance().getJourneyState().openJourneyEnabled();
        openJourneyButton.setDisable(openEnabled ? false : true);
        closeJourneyButton.setDisable(!open);
    }


    // =========================================================
    // TICKETS
    // =========================================================

    @FXML
    public void onTickets() {

        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "Veuillez ouvrir une journée avant de créer un ticket.");
            return;
        }

        SceneManager.show("/fxml/tickets.fxml", "Tickets");
    }


    // =========================================================
    // RETURNS
    // =========================================================

    @FXML
    public void onReturns() {

        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "Veuillez ouvrir une journée avant de traiter un retour.");
            return;
        }

        SceneManager.show("/fxml/returns.fxml", "Retours");
    }


    // =========================================================
    // STOCK
    // =========================================================

    @FXML
    public void onStock() {
        SceneManager.show("/fxml/stock.fxml", "Stock");
    }


    // =========================================================
    // REPORTS
    // =========================================================

    @FXML
    public void onReports() {
        SceneManager.show("/fxml/reports.fxml", "Rapports");
    }


    // =========================================================
    // JOURNEY — OPEN (quick action, inline in the menu)
    // =========================================================

    @FXML
    public void onOpenJourneyQuick() {

        setJourneyBusy(true);

        Task<Journey> task = new Task<>() {
            @Override
            protected Journey call() throws Exception {
                String userId = AppState.getInstance().getCurrentUser().getId();
                String cashierCode = AppState.getInstance().getCurrentUser().getCashierCode();
                // Re-check with the server before opening, in case another session already has one open.
                Journey existing = journeyService.findOpenJourney(userId);
                if (existing != null) return existing;
                return journeyService.openJourney(userId, cashierCode);
            }
        };

        task.setOnSucceeded(e -> {
            Journey j = task.getValue();
            AppState.getInstance().setCurrentJourney(j);
            setJourneyBusy(false);

            AlertUtil.info("Journée ouverte",
                    "Journey successfully opened.\n\nJourney ID: " + j.getId());
        });

        task.setOnFailed(e -> {
            setJourneyBusy(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Échec de l'ouverture.");
        });

        new Thread(task, "open-journey").start();
    }


    // =========================================================
    // JOURNEY — CLOSE (builds summary, then shows confirmation screen)
    // =========================================================

    @FXML
    public void onCloseJourneyQuick() {

        Journey current = AppState.getInstance().getCurrentJourney();

        if (current == null || !current.isOpen()) {
            AlertUtil.error("Aucune journée ouverte", "Il n'y a aucune journée à fermer.");
            return;
        }

        setJourneyBusy(true);

        Task<Journey> summaryTask = new Task<>() {
            @Override
            protected Journey call() throws Exception {
                return journeyService.buildSummary(current);
            }
        };

        summaryTask.setOnSucceeded(e -> {
            setJourneyBusy(false);
            SceneManager.show("/fxml/close_journey.fxml", "Fermeture de journée");
        });

        summaryTask.setOnFailed(e -> {
            setJourneyBusy(false);
            AlertUtil.error("Erreur", summaryTask.getException() != null ? summaryTask.getException().getMessage() : "Échec du calcul du résumé.");
        });

        new Thread(summaryTask, "journey-summary").start();
    }


    private void setJourneyBusy(boolean busy) {
        journeyProgress.setVisible(busy);
        openJourneyButton.setDisable(busy || !AppState.getInstance().getJourneyState().openJourneyEnabled());
        closeJourneyButton.setDisable(busy || !AppState.getInstance().hasOpenJourney());
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    public void onLogout() {

        authService.logout();

        AppState.getInstance().reset();

        SceneManager.show("/fxml/login.fxml", "Connexion");
    }
}