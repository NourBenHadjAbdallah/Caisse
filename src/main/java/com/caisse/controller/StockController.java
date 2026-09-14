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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class StockController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> stockStatusCombo;
    @FXML private ComboBox<String> discountCombo;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private Label resultCountLabel;

    @FXML private TableView<Product> stockTable;
    @FXML private TableColumn<Product, String> colCode;
    @FXML private TableColumn<Product, String> colLabel;
    @FXML private TableColumn<Product, String> colStock;
    @FXML private TableColumn<Product, String> colNormalPrice;
    @FXML private TableColumn<Product, String> colDiscount;
    @FXML private TableColumn<Product, String> colFinalPrice;
    @FXML private ProgressIndicator progressIndicator;

    private final ProductService productService = new ProductService();

    /** Full list currently loaded from the server (before filters are applied). */
    private List<Product> loadedProducts = List.of();

    private static final String STATUS_ALL = "Tous";
    private static final String STATUS_IN_STOCK = "En stock";
    private static final String STATUS_LOW = "Stock faible (< 10)";
    private static final String STATUS_OUT = "Rupture de stock";

    private static final String DISCOUNT_ALL = "Tous";
    private static final String DISCOUNT_WITH = "Avec remise";
    private static final String DISCOUNT_WITHOUT = "Sans remise";

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        colLabel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLabel()));
        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStockQuantity())));
        colNormalPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNormalPrice() + " DT"));
        colDiscount.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDiscountPercent() + " %"));
        colFinalPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().finalPrice() + " DT"));

        stockStatusCombo.setItems(FXCollections.observableArrayList(
                STATUS_ALL, STATUS_IN_STOCK, STATUS_LOW, STATUS_OUT));
        stockStatusCombo.getSelectionModel().select(STATUS_ALL);

        discountCombo.setItems(FXCollections.observableArrayList(
                DISCOUNT_ALL, DISCOUNT_WITH, DISCOUNT_WITHOUT));
        discountCombo.getSelectionModel().select(DISCOUNT_ALL);

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

    @FXML
    public void onApplyFilters() {
        applyFilters();
    }

    @FXML
    public void onResetFilters() {
        stockStatusCombo.getSelectionModel().select(STATUS_ALL);
        discountCombo.getSelectionModel().select(DISCOUNT_ALL);
        minPriceField.clear();
        maxPriceField.clear();
        applyFilters();
    }

    private void runQuery(java.util.concurrent.Callable<List<Product>> query) {
        progressIndicator.setVisible(true);
        Task<List<Product>> task = new Task<>() {
            @Override protected List<Product> call() throws Exception { return query.call(); }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            loadedProducts = task.getValue();
            applyFilters();
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Recherche échouée.");
        });
        new Thread(task, "stock-query").start();
    }

    /** Applies the stock/discount/price filters on top of whatever is currently loaded (all products or a search result). */
    private void applyFilters() {
        String status = stockStatusCombo.getValue() == null ? STATUS_ALL : stockStatusCombo.getValue();
        String discountFilter = discountCombo.getValue() == null ? DISCOUNT_ALL : discountCombo.getValue();

        BigDecimal minPrice = parsePriceOrNull(minPriceField.getText());
        BigDecimal maxPrice = parsePriceOrNull(maxPriceField.getText());

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            AlertUtil.error("Filtre invalide", "Le prix minimum doit être inférieur ou égal au prix maximum.");
            return;
        }

        List<Product> filtered = loadedProducts.stream()
                .filter(p -> matchesStatus(p, status))
                .filter(p -> matchesDiscount(p, discountFilter))
                .filter(p -> minPrice == null || p.finalPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.finalPrice().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());

        stockTable.setItems(FXCollections.observableArrayList(filtered));
        resultCountLabel.setText(filtered.size() + " produit(s) affiché(s) sur " + loadedProducts.size());
    }

    private boolean matchesStatus(Product p, String status) {
        int qty = p.getStockQuantity();
        return switch (status) {
            case STATUS_IN_STOCK -> qty >= 10;
            case STATUS_LOW -> qty > 0 && qty < 10;
            case STATUS_OUT -> qty <= 0;
            default -> true; // STATUS_ALL
        };
    }

    private boolean matchesDiscount(Product p, String discountFilter) {
        boolean hasDiscount = p.getDiscountPercent() != null && p.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0;
        return switch (discountFilter) {
            case DISCOUNT_WITH -> hasDiscount;
            case DISCOUNT_WITHOUT -> !hasDiscount;
            default -> true; // DISCOUNT_ALL
        };
    }

    private BigDecimal parsePriceOrNull(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return new BigDecimal(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            AlertUtil.error("Prix invalide", "Veuillez saisir un nombre valide pour le prix.");
            return null;
        }
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }
}