package com.caisse.controller;

import com.caisse.service.ReportService;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class ReportController {

    @FXML private ComboBox<String> quickRangeCombo;
    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private TextField cashierField;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private Label totalSalesLabel;
    @FXML private Label ticketCountLabel;
    @FXML private Label cashSalesLabel;
    @FXML private Label cardSalesLabel;
    @FXML private Label returnsLabel;
    @FXML private Label netSalesLabel;
    @FXML private Label productsSoldLabel;
    @FXML private Label productsReturnedLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        quickRangeCombo.setItems(FXCollections.observableArrayList(
                "Aujourd'hui", "Hier", "Cette semaine", "Ce mois", "Personnalisé"));
        quickRangeCombo.setOnAction(e -> applyQuickRange());
        quickRangeCombo.getSelectionModel().select("Aujourd'hui");
        applyQuickRange();

        paymentCombo.setItems(FXCollections.observableArrayList("Tous", "CASH", "CARD"));
        paymentCombo.getSelectionModel().select("Tous");
    }

    private void applyQuickRange() {
        String choice = quickRangeCombo.getValue();
        LocalDate today = LocalDate.now();
        switch (choice == null ? "" : choice) {
            case "Aujourd'hui" -> { fromPicker.setValue(today); toPicker.setValue(today); }
            case "Hier" -> { LocalDate y = today.minusDays(1); fromPicker.setValue(y); toPicker.setValue(y); }
            case "Cette semaine" -> {
                fromPicker.setValue(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                toPicker.setValue(today);
            }
            case "Ce mois" -> {
                fromPicker.setValue(today.withDayOfMonth(1));
                toPicker.setValue(today);
            }
            default -> { /* Personnalisé: leave the pickers as the user set them */ }
        }
    }

    @FXML
    public void onGenerate() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null || from.isAfter(to)) {
            AlertUtil.error("Période invalide", "Veuillez choisir une période valide (du / au).");
            return;
        }
        String cashier = cashierField.getText() == null ? "" : cashierField.getText().trim();
        String payment = paymentCombo.getValue();
        String paymentFilter = (payment == null || payment.equals("Tous")) ? null : payment;

        progressIndicator.setVisible(true);
        Task<ReportService.ReportResult> task = new Task<>() {
            @Override protected ReportService.ReportResult call() throws Exception {
                return reportService.run(from, to, cashier.isEmpty() ? null : cashier, paymentFilter);
            }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            ReportService.ReportResult r = task.getValue();
            totalSalesLabel.setText(money(r.totalSales));
            ticketCountLabel.setText(String.valueOf(r.ticketCount));
            cashSalesLabel.setText(money(r.cashSales));
            cardSalesLabel.setText(money(r.cardSales));
            returnsLabel.setText(money(r.returnsTotal));
            netSalesLabel.setText(money(r.netSales));
            productsSoldLabel.setText(String.valueOf(r.productsSold));
            productsReturnedLabel.setText(String.valueOf(r.productsReturned));
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Échec de la génération du rapport.");
        });
        new Thread(task, "generate-report").start();
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }

    private String money(java.math.BigDecimal v) {
        return v.setScale(3, java.math.RoundingMode.HALF_UP) + " DT";
    }
}
