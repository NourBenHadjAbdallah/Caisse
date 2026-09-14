package com.caisse.controller;

import com.caisse.model.ReceiptRow;
import com.caisse.service.TicketService;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.ReceiptContentFactory;
import com.caisse.util.ReceiptPdfBuilder;
import com.caisse.util.ReceiptViewBuilder;
import com.caisse.util.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TicketDetailController {

    @FXML private VBox receiptContainer;
    @FXML private ProgressIndicator progressIndicator;

    private final TicketService ticketService = new TicketService();

    private JSONObject ticket;
    private List<ReceiptRow> rows;

    @FXML
    public void initialize() {
        String ticketId = AppState.getInstance().getSelectedTicketId();
        if (ticketId == null) {
            AlertUtil.error("Erreur", "Aucun ticket sélectionné.");
            SceneManager.show("/fxml/ticket_history.fxml", "Historique des tickets");
            return;
        }

        loadTicket(ticketId);
    }

    private void loadTicket(String ticketId) {
        progressIndicator.setVisible(true);
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                return ticketService.findTicketWithItems(ticketId);
            }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            ticket = task.getValue();
            if (ticket == null) {
                AlertUtil.error("Erreur", "Ticket introuvable.");
                return;
            }

            rows = ReceiptContentFactory.buildTicketRows(ticket);
            renderReceipt();
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.error("Erreur",
                    task.getException() != null ? task.getException().getMessage() : "Échec du chargement du ticket.");
        });
        new Thread(task, "load-ticket-detail").start();
    }

    private void renderReceipt() {
        VBox built = ReceiptViewBuilder.build(rows);
        receiptContainer.getChildren().setAll(built.getChildren());
        receiptContainer.setAlignment(built.getAlignment());
        receiptContainer.setPadding(built.getPadding());
        receiptContainer.setSpacing(built.getSpacing());
    }

    @FXML
    public void onPrint() {
        if (rows == null) {
            AlertUtil.error("Impression", "Aucun ticket à imprimer.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            AlertUtil.error("Impression", "Aucune imprimante disponible.");
            return;
        }

        if (!job.showPrintDialog(SceneManager.getStage())) {
            return;
        }

        VBox receiptForPrint = ReceiptViewBuilder.build(rows);

        Printer printer = job.getPrinter();
        PageLayout layout = printer.createPageLayout(Paper.A5, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);

        boolean success = job.printPage(layout, receiptForPrint);
        if (success) {
            job.endJob();
            AlertUtil.info("Impression", "Ticket envoyé à l'imprimante.");
        } else {
            AlertUtil.error("Impression", "Échec de l'impression.");
        }
    }

    @FXML
    public void onSavePdf() {
        if (rows == null || ticket == null) {
            AlertUtil.error("Enregistrer", "Aucun ticket à enregistrer.");
            return;
        }

        String shortId = ticket.getString("id").substring(0, 8).toUpperCase();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le ticket en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        chooser.setInitialFileName("ticket_" + shortId + ".pdf");

        File file = chooser.showSaveDialog(SceneManager.getStage());
        if (file == null) return;

        try {
            ReceiptPdfBuilder.save(rows, file);
            AlertUtil.info("PDF enregistré", "Le ticket a été enregistré :\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AlertUtil.error("Erreur", "Échec de l'enregistrement du PDF : " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/ticket_history.fxml", "Historique des tickets");
    }
}