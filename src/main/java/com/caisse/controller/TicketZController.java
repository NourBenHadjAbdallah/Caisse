package com.caisse.controller;

import com.caisse.model.JourneyClosingResult;
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

public class TicketZController {

    @FXML private VBox receiptContainer;

    private List<ReceiptRow> rows;
    private JourneyClosingResult result;

    @FXML
    public void initialize() {
        result = AppState.getInstance().getJourneyClosingResult();
        if (result == null) {
            AlertUtil.error("Erreur", "Aucune donnée de fermeture disponible.");
            SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
            return;
        }

        rows = ReceiptContentFactory.buildTicketZRows(result);
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
            AlertUtil.info("Impression", "Ticket Z envoyé à l'imprimante.");
        } else {
            AlertUtil.error("Impression", "Échec de l'impression.");
        }
    }

    @FXML
    public void onSavePdf() {
        if (rows == null || result == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le Ticket Z en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        chooser.setInitialFileName("ticket_z_" + result.getJourney().getId() + ".pdf");

        File file = chooser.showSaveDialog(SceneManager.getStage());
        if (file == null) return;

        try {
            ReceiptPdfBuilder.save(rows, file);
            AlertUtil.info("PDF enregistré", "Le Ticket Z a été enregistré :\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AlertUtil.error("Erreur", "Échec de l'enregistrement du PDF : " + e.getMessage());
        }
    }

    @FXML
    public void onFinish() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }
}