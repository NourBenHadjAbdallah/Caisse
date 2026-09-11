package com.caisse.controller;

import com.caisse.model.Product;
import com.caisse.service.ProductService;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class StockController {

    @FXML private TextField searchField;
    @FXML private TableView<Product> stockTable;
    @FXML private TableColumn<Product, String> colCode;
    @FXML private TableColumn<Product, String> colLabel;
    @FXML private TableColumn<Product, String> colStock;
    @FXML private TableColumn<Product, String> colNormalPrice;
    @FXML private TableColumn<Product, String> colDiscount;
    @FXML private TableColumn<Product, String> colFinalPrice;
    @FXML private ProgressIndicator progressIndicator;

    private final ProductService productService = new ProductService();

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        colLabel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLabel()));
        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStockQuantity())));
        colNormalPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNormalPrice() + " DT"));
        colDiscount.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDiscountPercent() + " %"));
        colFinalPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().finalPrice() + " DT"));
        onListAll();
    }

    @FXML
    public void onSearch() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim();
        if (term.isEmpty()) { onListAll(); return; }
        runQuery(() -> productService.search(term));
    }

    @FXML
    public void onListAll() {
        runQuery(productService::listAll);
    }

    private void runQuery(java.util.concurrent.Callable<List<Product>> query) {
        progressIndicator.setVisible(true);
        Task<List<Product>> task = new Task<>() {
            @Override protected List<Product> call() throws Exception { return query.call(); }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            stockTable.setItems(FXCollections.observableArrayList(task.getValue()));
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Recherche échouée.");
        });
        new Thread(task, "stock-query").start();
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }
}
