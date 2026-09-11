package com.caisse.controller;

import com.caisse.model.Product;
import com.caisse.model.Ticket;
import com.caisse.model.TicketItem;
import com.caisse.service.ProductService;
import com.caisse.service.TicketService;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.util.List;

public class TicketController {

    @FXML private TextField codeField;
    @FXML private Label journeyLabel;
    @FXML private ListView<Product> searchResults;
    @FXML private TableView<TicketItem> cartTable;
    @FXML private TableColumn<TicketItem, String> colCode;
    @FXML private TableColumn<TicketItem, String> colLabel;
    @FXML private TableColumn<TicketItem, String> colQty;
    @FXML private TableColumn<TicketItem, String> colPrice;
    @FXML private TableColumn<TicketItem, String> colDiscount;
    @FXML private TableColumn<TicketItem, String> colTotal;
    @FXML private TableColumn<TicketItem, Void> colRemove;
    @FXML private Label totalLabel;
    @FXML private ToggleButton cashToggle;
    @FXML private ToggleButton cardToggle;
    @FXML private ProgressIndicator progressIndicator;

    private final ProductService productService = new ProductService();
    private final TicketService ticketService = new TicketService();
    private final ObservableList<TicketItem> cart = FXCollections.observableArrayList();
    private final ToggleGroup paymentGroup = new ToggleGroup();

    @FXML
    public void initialize() {
        // Defense in depth: re-verify the journey is open even though navigation should have blocked us already.
        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "No open journey.\n\nPlease open a journey before creating a ticket.");
            SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
            return;
        }
        journeyLabel.setText("Journée: " + AppState.getInstance().getCurrentJourney().getId());

        cashToggle.setToggleGroup(paymentGroup);
        cardToggle.setToggleGroup(paymentGroup);
        cashToggle.setSelected(true);

        cartTable.setItems(cart);
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduct().getCode()));
        colLabel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduct().getLabel()));
        colQty.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().getUnitPrice())));
        colDiscount.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDiscountPercent() + " %"));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().lineTotal())));
        addRemoveButtonColumn();

        searchResults.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null :
                        p.getCode() + " — " + p.getLabel() + " (stock: " + p.getStockQuantity() + ", " + money(p.finalPrice()) + ")");
            }
        });
        searchResults.setOnMouseClicked(e -> {
            Product selected = searchResults.getSelectionModel().getSelectedItem();
            if (selected != null) addToCart(selected);
        });
    }

    @FXML
    public void onAddByCode() {
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        if (code.isEmpty()) return;
        Task<Product> task = new Task<>() {
            @Override protected Product call() throws Exception { return productService.findByCode(code); }
        };
        task.setOnSucceeded(e -> {
            Product p = task.getValue();
            if (p == null) {
                AlertUtil.error("Produit introuvable", "Aucun produit avec le code \"" + code + "\".");
            } else {
                addToCart(p);
            }
            codeField.clear();
        });
        task.setOnFailed(e -> AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Recherche échouée."));
        new Thread(task, "find-product").start();
    }

    @FXML
    public void onSearch() {
        String term = codeField.getText() == null ? "" : codeField.getText().trim();
        if (term.isEmpty()) return;
        Task<List<Product>> task = new Task<>() {
            @Override protected List<Product> call() throws Exception { return productService.search(term); }
        };
        task.setOnSucceeded(e -> {
            searchResults.setItems(FXCollections.observableArrayList(task.getValue()));
            searchResults.setVisible(!task.getValue().isEmpty());
            searchResults.setManaged(!task.getValue().isEmpty());
        });
        task.setOnFailed(e -> AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Recherche échouée."));
        new Thread(task, "search-products").start();
    }

    private void addToCart(Product p) {
        if (p.getStockQuantity() <= 0) {
            AlertUtil.error("Rupture de stock", "\"" + p.getLabel() + "\" n'a plus de stock disponible.");
            return;
        }
        for (TicketItem item : cart) {
            if (item.getProduct().getId().equals(p.getId())) {
                item.setQuantity(item.getQuantity() + 1);
                cartTable.refresh();
                updateTotal();
                return;
            }
        }
        cart.add(new TicketItem(p, 1));
        updateTotal();
        searchResults.setVisible(false);
        searchResults.setManaged(false);
    }

    private void addRemoveButtonColumn() {
        colRemove.setCellFactory((Callback<TableColumn<TicketItem, Void>, TableCell<TicketItem, Void>>) column -> new TableCell<>() {
            private final Button btn = new Button("Retirer");
            {
                btn.setOnAction(e -> {
                    cart.remove(getTableView().getItems().get(getIndex()));
                    updateTotal();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (TicketItem item : cart) total = total.add(item.lineTotal());
        totalLabel.setText(money(total));
    }

    @FXML
    public void onValidate() {
        if (!AppState.getInstance().hasOpenJourney()) {
            AlertUtil.error("Aucune journée ouverte", "No open journey.\n\nPlease open a journey before creating a ticket.");
            SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
            return;
        }
        if (cart.isEmpty()) {
            AlertUtil.error("Ticket vide", "Ajoutez au moins un article avant de valider.");
            return;
        }
        Ticket ticket = new Ticket();
        ticket.setJourneyId(AppState.getInstance().getCurrentJourney().getId());
        ticket.setCashierId(AppState.getInstance().getCurrentUser().getId());
        ticket.setPaymentMethod(cashToggle.isSelected() ? "CASH" : "CARD");
        ticket.getItems().addAll(cart);

        progressIndicator.setVisible(true);
        Task<Ticket> task = new Task<>() {
            @Override protected Ticket call() throws Exception { return ticketService.saveTicket(ticket); }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.info("Ticket validé", "Ticket enregistré avec succès.\nTotal: " + money(ticket.total()) +
                    "\nPaiement: " + ticket.getPaymentMethod());
            cart.clear();
            updateTotal();
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.error("Erreur", task.getException() != null ? task.getException().getMessage() : "Échec de l'enregistrement du ticket.");
        });
        new Thread(task, "save-ticket").start();
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/main_menu.fxml", "Menu principal");
    }

    private String money(BigDecimal v) {
        return v.setScale(3, java.math.RoundingMode.HALF_UP) + " DT";
    }
}
