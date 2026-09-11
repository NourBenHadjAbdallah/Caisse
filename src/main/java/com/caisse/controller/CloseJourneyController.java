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

public class CloseJourneyController {

    @FXML private Label journeyLabel;
    @FXML private Label cashierLabel;
    @FXML private Label ticketsLabel;
    @FXML private Label grossLabel;
    @FXML private Label cashLabel;
    @FXML private Label cardLabel;
    @FXML private Label returnsLabel;
    @FXML private Label netLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final JourneyService journeyService = new JourneyService();
    private Journey journey;

    @FXML
    public void initialize() {
        journey = AppState.getInstance().getCurrentJourney(); // already carries the computed summary
        journeyLabel.setText("Journey: " + journey.getId());
        cashierLabel.setText("Cashier: " + journey.getCashierCode());
        ticketsLabel.setText(String.valueOf(journey.getTicketCount()));
        grossLabel.setText(money(journey.getTotalSales()));
        cashLabel.setText(money(journey.getTotalCash()));
        cardLabel.setText(money(journey.getTotalCard()));
        returnsLabel.setText(money(journey.getTotalReturns()));
        netLabel.setText(money(journey.getNetTotal()));
    }

    @FXML
    public void onConfirmClose() {
        if (!AlertUtil.confirm("Confirmer la fermeture", "Fermer définitivement la journée " + journey.getId() + " ?")) {
            return;
        }
        setBusy(true);
        Task<Journey> task = new Task<>() {
            @Override
            protected Journey call() throws Exception {
                return journeyService.closeJourney(journey);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            Journey closed = task.getValue();
            AppState.getInstance().setCurrentJourney(closed); // triggers NO_OPEN_JOURNEY -> re-enables Open Journey
            String ticketZ = buildTicketZ(closed);
            AlertUtil.info("Ticket Z / Liste des règlements", ticketZ);
            SceneManager.show("/fxml/journey_menu.fxml", "Journée");
        });
        task.setOnFailed(e -> {
            setBusy(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Échec de la fermeture.");
        });
        new Thread(task, "close-journey").start();
    }

    @FXML
    public void onCancel() {
        SceneManager.show("/fxml/journey_menu.fxml", "Journée");
    }

    private String buildTicketZ(Journey j) {
        return "TICKET Z\n" +
                "Journée: " + j.getId() + "\n" +
                "Caissier: " + j.getCashierCode() + "\n" +
                "-----------------------------\n" +
                "Tickets: " + j.getTicketCount() + "\n" +
                "Ventes brutes: " + money(j.getTotalSales()) + "\n\n" +
                "LISTE DES RÈGLEMENTS\n" +
                "Espèces: " + money(j.getTotalCash()) + "\n" +
                "Carte: " + money(j.getTotalCard()) + "\n" +
                "Retours: " + money(j.getTotalReturns()) + "\n" +
                "-----------------------------\n" +
                "Ventes nettes: " + money(j.getNetTotal());
    }

    private String money(java.math.BigDecimal v) {
        return v.setScale(3, java.math.RoundingMode.HALF_UP) + " DT";
    }

    private void setBusy(boolean busy) {
        progressIndicator.setVisible(busy);
    }
}
