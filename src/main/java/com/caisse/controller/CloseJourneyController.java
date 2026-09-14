package com.caisse.controller;

import com.caisse.model.Journey;
import com.caisse.model.JourneyClosingResult;
import com.caisse.service.JourneyService;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CloseJourneyController {

    @FXML private Label journeyLabel;
    @FXML private Label cashierLabel;
    @FXML private Label ticketsLabel;
    @FXML private Label grossLabel;
    @FXML private Label cashLabel;
    @FXML private Label cardLabel;
    @FXML private Label returnsLabel;
    @FXML private Label netLabel;

    @FXML private TextField cashCountField;
    @FXML private TextField cardCountField;
    @FXML private Label mismatchLabel;

    @FXML private ProgressIndicator progressIndicator;

    private final JourneyService journeyService = new JourneyService();
    private Journey journey;

    @FXML
    public void initialize() {
        journey = AppState.getInstance().getCurrentJourney(); // already carries the computed summary

        journeyLabel.setText("Journée : " + journey.getId());
        cashierLabel.setText("Caissier : " + journey.getCashierCode());

        ticketsLabel.setText(String.valueOf(journey.getTicketCount()));
        grossLabel.setText(money(journey.getTotalSales()));
        cashLabel.setText(money(journey.getTotalCash()));
        cardLabel.setText(money(journey.getTotalCard()));
        returnsLabel.setText(money(journey.getTotalReturns()));
        netLabel.setText(money(journey.getNetTotal()));

        mismatchLabel.setVisible(false);
        mismatchLabel.setManaged(false);
    }

    @FXML
    public void onConfirmClose() {

        mismatchLabel.setVisible(false);
        mismatchLabel.setManaged(false);

        BigDecimal cashCounted = parseAmount(cashCountField.getText());
        BigDecimal cardCounted = parseAmount(cardCountField.getText());

        if (cashCounted == null || cardCounted == null) {
            showMismatch("Veuillez saisir des montants valides pour les espèces et la carte.");
            return;
        }

        BigDecimal expectedCash = journey.getTotalCash().setScale(3, RoundingMode.HALF_UP);
        BigDecimal expectedCard = journey.getTotalCard().setScale(3, RoundingMode.HALF_UP);

        boolean cashMatches = cashCounted.compareTo(expectedCash) == 0;
        boolean cardMatches = cardCounted.compareTo(expectedCard) == 0;

        if (!cashMatches || !cardMatches) {
            StringBuilder message = new StringBuilder("Les montants saisis ne correspondent pas aux montants attendus :\n");
            if (!cashMatches) {
                message.append("\n• Espèces — attendu : ").append(money(expectedCash))
                        .append(" / saisi : ").append(money(cashCounted));
            }
            if (!cardMatches) {
                message.append("\n• Carte — attendu : ").append(money(expectedCard))
                        .append(" / saisi : ").append(money(cardCounted));
            }
            message.append("\n\nVeuillez recompter et corriger avant de fermer la journée.");
            showMismatch(message.toString());
            return;
        }

        if (!AlertUtil.confirm("Confirmer la fermeture",
                "Les montants comptés correspondent. Fermer définitivement la journée " + journey.getId() + " ?")) {
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
            AppState.getInstance().setJourneyClosingResult(new JourneyClosingResult(closed, cashCounted, cardCounted));
            SceneManager.show("/fxml/ticket_z.fxml", "Ticket Z");
        });
        task.setOnFailed(e -> {
            setBusy(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Échec de la fermeture.");
        });
        new Thread(task, "close-journey").start();
    }

    @FXML
    public void onCancel() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }

    private void showMismatch(String message) {
        mismatchLabel.setText(message);
        mismatchLabel.setVisible(true);
        mismatchLabel.setManaged(true);
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return new BigDecimal(text.trim().replace(",", ".")).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return v.setScale(3, RoundingMode.HALF_UP) + " DT";
    }

    private void setBusy(boolean busy) {
        progressIndicator.setVisible(busy);
    }
}