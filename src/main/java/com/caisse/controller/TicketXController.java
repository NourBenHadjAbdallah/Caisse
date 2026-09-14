package com.caisse.controller;

import com.caisse.model.Journey;
import com.caisse.model.ReceiptRow;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.ReceiptContentFactory;
import com.caisse.util.ReceiptPdfBuilder;
import com.caisse.util.ReceiptViewBuilder;
import com.caisse.util.SceneManager;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TicketXController {

    @FXML private VBox receiptContainer;

    private List<ReceiptRow> rows;
    private Journey journey;

    @FXML
    public void initialize() {
        journey = AppState.getInstance().getTicketXSnapshot();
        if (journey == null) {
            AlertUtil.error("Erreur", "Aucune donnée de journée disponible.");
            SceneManager.show("/fxml/tickets.fxml", "Tickets");
            return;
        }

        rows = ReceiptContentFactory.buildTicketXRows(journey);
        VBox built = ReceiptViewBuilder.build(rows);
        receiptContainer.getChildren().setAll(built.getChildren());
        receiptContainer.setAlignment(built.getAlignment());
        receiptContainer.setPadding(built.getPadding());
        receiptContainer.setSpacing(built.getSpacing());
    }

    @FXML
    public void onPrint() {
        if (rows == null) return;

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            AlertUtil.error("Impression", "Aucune imprimante disponible.");
            return;
        }
        if (!job.showPrintDialog(SceneManager.getStage())) return;

        VBox receiptForPrint = ReceiptViewBuilder.build(rows);
        Printer printer = job.getPrinter();
        PageLayout layout = printer.createPageLayout(Paper.A5, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);

        boolean success = job.printPage(layout, receiptForPrint);
        if (success) {
            job.endJob();
            AlertUtil.info("Impression", "Ticket X envoyé à l'imprimante.");
        } else {
            AlertUtil.error("Impression", "Échec de l'impression.");
        }
    }

    @FXML
    public void onSavePdf() {
        if (rows == null || journey == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le Ticket X en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        chooser.setInitialFileName("ticket_x_" + journey.getId() + ".pdf");

        File file = chooser.showSaveDialog(SceneManager.getStage());
        if (file == null) return;

        try {
            ReceiptPdfBuilder.save(rows, file);
            AlertUtil.info("PDF enregistré", "Le Ticket X a été enregistré :\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AlertUtil.error("Erreur", "Échec de l'enregistrement du PDF : " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/tickets.fxml", "Tickets");
    }
}