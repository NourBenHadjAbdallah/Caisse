package com.caisse.controller;

import com.caisse.model.Product;
import com.caisse.model.ReturnItem;
import com.caisse.model.ReturnRecord;
import com.caisse.service.ProductService;
import com.caisse.service.ReturnService;
import com.caisse.service.TicketService;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReturnController {

    @FXML private TextField ticketIdField;
    @FXML private Label originalTicketLabel;
    @FXML private TableView<ReturnRow> itemsTable;
    @FXML private TableColumn<ReturnRow, String> colCode;
    @FXML private TableColumn<ReturnRow, String> colLabel;
    @FXML private TableColumn<ReturnRow, String> colSold;
    @FXML private TableColumn<ReturnRow, Integer> colReturnQty;
    @FXML private TableColumn<ReturnRow, String> colUnitPrice;
    @FXML private TableColumn<ReturnRow, String> colRefund;
    @FXML private Label totalLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final TicketService ticketService = new TicketService();
    private final ProductService productService = new ProductService();
    private final ReturnService returnService = new ReturnService();
    private final ObservableList<ReturnRow> rows = FXCollections.observableArrayList();
    private String currentTicketId;

    public static class ReturnRow {
        final Product product;
        final int soldQty;
        final BigDecimal unitPrice;
        final SimpleIntegerProperty returnQty = new SimpleIntegerProperty(0);

        ReturnRow(Product product, int soldQty, BigDecimal unitPrice) {
            this.product = product;
            this.soldQty = soldQty;
            this.unitPrice = unitPrice;
        }
        BigDecimal refund() { return unitPrice.multiply(BigDecimal.valueOf(returnQty.get())); }
    }

    @FXML
    public void initialize() {
        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "Une journée doit être ouverte pour traiter un retour.");
            SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
            return;
        }
        itemsTable.setItems(rows);
        itemsTable.setEditable(true);

        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().product.getCode()));
        colLabel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().product.getLabel()));
        colSold.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().soldQty)));
        colUnitPrice.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().unitPrice)));
        colRefund.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().refund())));

        colReturnQty.setCellValueFactory(c -> c.getValue().returnQty.asObject());
        colReturnQty.setCellFactory(col -> new TextFieldTableCell<>(new javafx.util.converter.IntegerStringConverter()) {
            @Override
            public void commitEdit(Integer value) {
                super.commitEdit(value);
                ReturnRow row = getTableView().getItems().get(getIndex());
                int clamped = Math.max(0, Math.min(value == null ? 0 : value, row.soldQty));
                row.returnQty.set(clamped);
                itemsTable.refresh();
                updateTotal();
            }
        });
    }

    @FXML
    public void onLookup() {
        String ticketId = ticketIdField.getText() == null ? "" : ticketIdField.getText().trim();
        if (ticketId.isEmpty()) return;

        Task<JSONObject> task = new Task<>() {
            @Override protected JSONObject call() throws Exception { return ticketService.findTicketWithItems(ticketId); }
        };
        task.setOnSucceeded(e -> {
            JSONObject ticket = task.getValue();
            if (ticket == null) {
                AlertUtil.error("Ticket introuvable", "Aucun ticket avec ce numéro.");
                return;
            }
            currentTicketId = ticket.getString("id");
            originalTicketLabel.setText("Ticket original: " + currentTicketId + " — Total: " +
                    money(ticket.optBigDecimal("total", BigDecimal.ZERO)));
            populateRows(ticket.getJSONArray("items"));
        });
        task.setOnFailed(e -> AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Recherche échouée."));
        new Thread(task, "lookup-ticket").start();
    }

    private void populateRows(JSONArray items) {
        rows.clear();
        List<ReturnRow> built = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject productJson = item.optJSONObject("products");
            Product p = new Product();
            p.setId(item.getString("product_id"));
            p.setCode(productJson != null ? productJson.optString("code", "") : "");
            p.setLabel(productJson != null ? productJson.optString("label", "") : "");
            int soldQty = item.optInt("quantity", 0);
            BigDecimal unitPrice = item.optBigDecimal("unit_price", BigDecimal.ZERO);
            built.add(new ReturnRow(p, soldQty, unitPrice));
        }
        rows.setAll(built);
        updateTotal();
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ReturnRow r : rows) total = total.add(r.refund());
        totalLabel.setText(money(total));
    }

    @FXML
    public void onConfirmReturn() {
        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "Une journée doit être ouverte pour traiter un retour.");
            SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
            return;
        }
        if (currentTicketId == null) {
            AlertUtil.error("Aucun ticket sélectionné", "Recherchez d'abord un ticket.");
            return;
        }
        ReturnRecord ret = new ReturnRecord();
        ret.setOriginalTicketId(currentTicketId);
        ret.setJourneyId(AppState.getInstance().getCurrentJourney().getId());
        for (ReturnRow r : rows) {
            if (r.returnQty.get() > 0) {
                ret.getItems().add(new ReturnItem(r.product, r.returnQty.get(), r.unitPrice));
            }
        }
        if (ret.getItems().isEmpty()) {
            AlertUtil.error("Aucun article sélectionné", "Indiquez une quantité à retourner pour au moins un article.");
            return;
        }

        progressIndicator.setVisible(true);
        Task<ReturnRecord> task = new Task<>() {
            @Override protected ReturnRecord call() throws Exception { return returnService.saveReturn(ret); }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.info("Retour enregistré", "Remboursement: " + money(ret.total()));
            rows.clear();
            currentTicketId = null;
            originalTicketLabel.setText("");
            totalLabel.setText(money(BigDecimal.ZERO));
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Échec de l'enregistrement du retour.");
        });
        new Thread(task, "save-return").start();
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }

    private String money(BigDecimal v) {
        return v.setScale(3, java.math.RoundingMode.HALF_UP) + " DT";
    }
}
