package com.caisse.controller;

import com.caisse.model.Journey;
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

import java.time.format.DateTimeFormatter;

public class JourneyMenuController {

    @FXML private Button openButton;
    @FXML private Button closeButton;
    @FXML private VBox currentJourneyPanel;
    @FXML private Label emptyLabel;
    @FXML private Label idLabel;
    @FXML private Label cashierLabel;
    @FXML private Label openedLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final JourneyService journeyService = new JourneyService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        boolean open = AppState.getInstance().hasOpenJourney();
        openButton.setDisable(!AppState.getInstance().getJourneyState().openJourneyEnabled());
        closeButton.setDisable(!open);
        currentJourneyPanel.setVisible(open);
        currentJourneyPanel.setManaged(open);
        emptyLabel.setVisible(!open);

        if (open) {
            Journey j = AppState.getInstance().getCurrentJourney();
            idLabel.setText("Journey ID: " + j.getId());
            cashierLabel.setText("Cashier: " + j.getCashierCode());
            openedLabel.setText("Opened: " + (j.getOpenedAt() != null ? j.getOpenedAt().format(FMT) : "-"));
            statusLabel.setText("Status: " + j.getStatus());
        }
    }

    @FXML
    public void onOpenJourney() {
        setBusy(true);
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
            setBusy(false);
            Journey j = task.getValue();
            AppState.getInstance().setCurrentJourney(j);
            refresh();
            AlertUtil.info("Journée ouverte",
                    "Journey successfully opened.\n\nJourney ID: " + j.getId() +
                    "\nDate: " + (j.getOpenedAt() != null ? j.getOpenedAt().format(FMT) : ""));
        });
        task.setOnFailed(e -> {
            setBusy(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Échec de l'ouverture.");
        });
        new Thread(task, "open-journey").start();
    }

    @FXML
    public void onCloseJourney() {
        Journey current = AppState.getInstance().getCurrentJourney();
        if (current == null || !current.isOpen()) {
            AlertUtil.error("Aucune journée ouverte", "Il n'y a aucune journée à fermer.");
            return;
        }
        setBusy(true);
        Task<Journey> summaryTask = new Task<>() {
            @Override
            protected Journey call() throws Exception {
                return journeyService.buildSummary(current);
            }
        };
        summaryTask.setOnSucceeded(e -> {
            setBusy(false);
            SceneManager.show("/fxml/close_journey.fxml", "Fermeture de journée");
        });
        summaryTask.setOnFailed(e -> {
            setBusy(false);
            AlertUtil.error("Erreur", summaryTask.getException() != null ? summaryTask.getException().getMessage() : "Échec du calcul du résumé.");
        });
        new Thread(summaryTask, "journey-summary").start();
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }

    private void setBusy(boolean busy) {
        progressIndicator.setVisible(busy);
        openButton.setDisable(busy || !AppState.getInstance().getJourneyState().openJourneyEnabled());
        closeButton.setDisable(busy || !AppState.getInstance().hasOpenJourney());
    }
}
