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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


public class TicketController {

    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private TextField codeField;

    @FXML
    private Label journeyLabel;

    @FXML
    private ListView<Product> searchResults;

    @FXML
    private TableView<TicketItem> cartTable;

    @FXML
    private TableColumn<TicketItem, String> colCode;

    @FXML
    private TableColumn<TicketItem, String> colLabel;

    @FXML
    private TableColumn<TicketItem, String> colQty;

    @FXML
    private TableColumn<TicketItem, String> colPrice;

    @FXML
    private TableColumn<TicketItem, String> colDiscount;

    @FXML
    private TableColumn<TicketItem, String> colTotal;

    @FXML
    private TableColumn<TicketItem, Void> colRemove;

    @FXML
    private Label totalLabel;

    @FXML
    private ToggleButton cashToggle;

    @FXML
    private ToggleButton cardToggle;

    @FXML
    private Button validateButton;

    @FXML
    private ProgressIndicator progressIndicator;


    // =========================================================
    // SERVICES
    // =========================================================

    private final ProductService productService =
            new ProductService();

    private final TicketService ticketService =
            new TicketService();


    // =========================================================
    // CART
    // =========================================================

    private final ObservableList<TicketItem> cart =
            FXCollections.observableArrayList();


    // =========================================================
    // PAYMENT
    // =========================================================

    private final ToggleGroup paymentGroup =
            new ToggleGroup();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        // -----------------------------------------------------
        // JOURNEY CHECK
        // -----------------------------------------------------

        if (!AppState.getInstance().hasOpenJourney()) {

            AlertUtil.error(
                    "Aucune journée ouverte",
                    "Veuillez ouvrir une journée avant de créer un ticket."
            );

            SceneManager.show(
                    "/fxml/main_menu.fxml",
                    "Menu principal"
            );

            return;
        }


        // -----------------------------------------------------
        // JOURNEY LABEL
        // -----------------------------------------------------

        journeyLabel.setText(
                "Journée : "
                        + AppState.getInstance()
                        .getCurrentJourney()
                        .getId()
        );


        // -----------------------------------------------------
        // PAYMENT
        // -----------------------------------------------------

        cashToggle.setToggleGroup(paymentGroup);
        cardToggle.setToggleGroup(paymentGroup);

        paymentGroup.selectToggle(null);

        cashToggle.setSelected(false);
        cardToggle.setSelected(false);

        validateButton.setDisable(true);


        /*
         * Enable validation only when a payment method
         * has been selected.
         */
        paymentGroup.selectedToggleProperty()
                .addListener(
                        (observable, oldToggle, newToggle) -> {

                            validateButton.setDisable(
                                    newToggle == null
                            );
                        }
                );


        // -----------------------------------------------------
        // TABLE
        // -----------------------------------------------------

        cartTable.setItems(cart);

        cartTable.setEditable(true);


        // -----------------------------------------------------
        // CODE
        // -----------------------------------------------------

        colCode.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                cell.getValue()
                                        .getProduct()
                                        .getCode()
                        )
        );


        // -----------------------------------------------------
        // LABEL
        // -----------------------------------------------------

        colLabel.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                cell.getValue()
                                        .getProduct()
                                        .getLabel()
                        )
        );


        // -----------------------------------------------------
        // QUANTITY
        // -----------------------------------------------------

        colQty.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                String.valueOf(
                                        cell.getValue()
                                                .getQuantity()
                                )
                        )
        );


        /*
         * Quantity becomes editable.
         */
        colQty.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final TextField textField =
                                    new TextField();


                            @Override
                            public void startEdit() {

                                if (isEmpty()) {
                                    return;
                                }

                                super.startEdit();

                                textField.setText(
                                        String.valueOf(
                                                getItem()
                                        )
                                );

                                setGraphic(textField);

                                setText(null);

                                textField.selectAll();

                                textField.requestFocus();

                                textField.setOnAction(
                                        event ->
                                                commitEdit(
                                                        textField.getText()
                                                )
                                );

                                textField.focusedProperty()
                                        .addListener(
                                                (obs, oldValue, focused) -> {

                                                    if (!focused &&
                                                            isEditing()) {

                                                        commitEdit(
                                                                textField.getText()
                                                        );
                                                    }
                                                }
                                        );
                            }


                            @Override
                            public void cancelEdit() {

                                super.cancelEdit();

                                setText(
                                        String.valueOf(
                                                getItem()
                                        )
                                );

                                setGraphic(null);
                            }


                            @Override
                            public void updateItem(
                                    String item,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        item,
                                        empty
                                );

                                if (empty) {

                                    setText(null);
                                    setGraphic(null);

                                } else if (isEditing()) {

                                    textField.setText(item);

                                    setText(null);

                                    setGraphic(textField);

                                } else {

                                    setText(item);

                                    setGraphic(null);
                                }
                            }
                        }
        );


        /*
         * When quantity editing finishes,
         * update TicketItem.
         */
        colQty.setOnEditCommit(event -> {

            TicketItem item =
                    event.getRowValue();

            String value =
                    event.getNewValue();


            try {

                int quantity =
                        Integer.parseInt(
                                value.trim()
                        );


                if (quantity <= 0) {

                    AlertUtil.error(
                            "Quantité invalide",
                            "La quantité doit être supérieure à 0."
                    );

                    cartTable.refresh();

                    return;
                }


                int availableStock =
                        item.getProduct()
                                .getStockQuantity();


                if (quantity > availableStock) {

                    AlertUtil.error(
                            "Stock insuffisant",
                            "Stock disponible : "
                                    + availableStock
                    );

                    cartTable.refresh();

                    return;
                }


                item.setQuantity(quantity);

                cartTable.refresh();

                updateTotal();

            } catch (NumberFormatException e) {

                AlertUtil.error(
                        "Quantité invalide",
                        "Veuillez saisir un nombre entier."
                );

                cartTable.refresh();
            }
        });


        // -----------------------------------------------------
        // PRICE
        // -----------------------------------------------------

        colPrice.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                money(
                                        cell.getValue()
                                                .getUnitPrice()
                                )
                        )
        );


        /*
         * Price becomes editable.
         */
        colPrice.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final TextField textField =
                                    new TextField();


                            @Override
                            public void startEdit() {

                                if (isEmpty()) {
                                    return;
                                }

                                super.startEdit();


                                textField.setText(
                                        getItem()
                                );


                                setGraphic(textField);

                                setText(null);

                                textField.selectAll();

                                textField.requestFocus();


                                textField.setOnAction(
                                        event ->
                                                commitEdit(
                                                        textField.getText()
                                                )
                                );


                                textField.focusedProperty()
                                        .addListener(
                                                (obs, oldValue, focused) -> {

                                                    if (!focused &&
                                                            isEditing()) {

                                                        commitEdit(
                                                                textField.getText()
                                                        );
                                                    }
                                                }
                                        );
                            }


                            @Override
                            public void cancelEdit() {

                                super.cancelEdit();

                                setText(
                                        getItem()
                                );

                                setGraphic(null);
                            }


                            @Override
                            public void updateItem(
                                    String item,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        item,
                                        empty
                                );


                                if (empty) {

                                    setText(null);
                                    setGraphic(null);

                                } else if (isEditing()) {

                                    textField.setText(item);

                                    setText(null);

                                    setGraphic(textField);

                                } else {

                                    setText(item);

                                    setGraphic(null);
                                }
                            }
                        }
        );


        /*
         * When price editing finishes,
         * update TicketItem.
         */
        colPrice.setOnEditCommit(event -> {

            TicketItem item =
                    event.getRowValue();

            String value =
                    event.getNewValue();


            try {

                /*
                 * Remove DT if user typed it.
                 */
                String cleanValue =
                        value
                                .replace("DT", "")
                                .trim()
                                .replace(",", ".");


                BigDecimal price =
                        new BigDecimal(
                                cleanValue
                        );


                if (price.compareTo(
                        BigDecimal.ZERO
                ) < 0) {

                    AlertUtil.error(
                            "Prix invalide",
                            "Le prix ne peut pas être négatif."
                    );

                    cartTable.refresh();

                    return;
                }


                item.setUnitPrice(price);

                cartTable.refresh();

                updateTotal();

            } catch (NumberFormatException e) {

                AlertUtil.error(
                        "Prix invalide",
                        "Veuillez saisir un prix valide."
                );

                cartTable.refresh();
            }
        });


        // -----------------------------------------------------
        // DISCOUNT
        // -----------------------------------------------------

        colDiscount.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                cell.getValue()
                                        .getDiscountPercent()
                                        + " %"
                        )
        );


        // -----------------------------------------------------
        // TOTAL
        // -----------------------------------------------------

        colTotal.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                money(
                                        cell.getValue()
                                                .lineTotal()
                                )
                        )
        );


        // -----------------------------------------------------
        // REMOVE
        // -----------------------------------------------------

        addRemoveButtonColumn();


        // -----------------------------------------------------
        // SEARCH
        // -----------------------------------------------------

        searchResults.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    Product product,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        product,
                                        empty
                                );


                                if (empty ||
                                        product == null) {

                                    setText(null);

                                } else {

                                    setText(
                                            product.getCode()
                                                    + " — "
                                                    + product.getLabel()
                                                    + " (stock: "
                                                    + product.getStockQuantity()
                                                    + ", "
                                                    + money(
                                                            product.finalPrice()
                                                    )
                                                    + ")"
                                    );
                                }
                            }
                        }
        );


        searchResults.setOnMouseClicked(
                event -> {

                    Product selected =
                            searchResults
                                    .getSelectionModel()
                                    .getSelectedItem();


                    if (selected != null) {

                        addToCart(selected);
                    }
                }
        );


        updateTotal();

        codeField.requestFocus();
    }


    // =========================================================
    // ADD BY CODE
    // =========================================================

    @FXML
    public void onAddByCode() {

        String code =
                codeField.getText() == null
                        ? ""
                        : codeField.getText().trim();


        if (code.isEmpty()) {
            return;
        }


        Task<Product> task =
                new Task<>() {

                    @Override
                    protected Product call()
                            throws Exception {

                        return productService
                                .findByCode(code);
                    }
                };


        task.setOnSucceeded(
                event -> {

                    Product product =
                            task.getValue();


                    if (product == null) {

                        AlertUtil.error(
                                "Produit introuvable",
                                "Aucun produit avec le code \""
                                        + code
                                        + "\"."
                        );

                    } else {

                        addToCart(product);
                    }


                    codeField.clear();

                    codeField.requestFocus();
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable exception =
                            task.getException();


                    AlertUtil.error(
                            "Erreur",
                            exception != null
                                    ? exception.getMessage()
                                    : "Recherche échouée."
                    );
                }
        );


        Thread thread =
                new Thread(
                        task,
                        "find-product"
                );

        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @FXML
    public void onSearch() {

        String term =
                codeField.getText() == null
                        ? ""
                        : codeField.getText().trim();


        if (term.isEmpty()) {
            return;
        }


        Task<List<Product>> task =
                new Task<>() {

                    @Override
                    protected List<Product> call()
                            throws Exception {

                        return productService.search(term);
                    }
                };


        task.setOnSucceeded(
                event -> {

                    List<Product> products =
                            task.getValue();


                    searchResults.setItems(
                            FXCollections
                                    .observableArrayList(
                                            products
                                    )
                    );


                    boolean hasResults =
                            !products.isEmpty();


                    searchResults.setVisible(
                            hasResults
                    );

                    searchResults.setManaged(
                            hasResults
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable exception =
                            task.getException();


                    AlertUtil.error(
                            "Erreur",
                            exception != null
                                    ? exception.getMessage()
                                    : "Recherche échouée."
                    );
                }
        );


        Thread thread =
                new Thread(
                        task,
                        "search-products"
                );

        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // ADD TO CART
    // =========================================================

    private void addToCart(Product product) {

        if (product.getStockQuantity() <= 0) {

            AlertUtil.error(
                    "Rupture de stock",
                    "\""
                            + product.getLabel()
                            + "\" n'a plus de stock disponible."
            );

            return;
        }


        for (TicketItem item : cart) {

            if (item.getProduct()
                    .getId()
                    .equals(product.getId())) {

                int newQuantity =
                        item.getQuantity() + 1;


                if (newQuantity >
                        product.getStockQuantity()) {

                    AlertUtil.error(
                            "Stock insuffisant",
                            "Stock disponible : "
                                    + product.getStockQuantity()
                    );

                    return;
                }


                item.setQuantity(
                        newQuantity
                );


                cartTable.refresh();

                updateTotal();

                return;
            }
        }


        cart.add(
                new TicketItem(
                        product,
                        1
                )
        );


        updateTotal();


        searchResults.setVisible(false);

        searchResults.setManaged(false);


        codeField.requestFocus();
    }


    // =========================================================
    // REMOVE BUTTON
    // =========================================================

    private void addRemoveButtonColumn() {

        colRemove.setCellFactory(
                (Callback<TableColumn<TicketItem, Void>,
                        TableCell<TicketItem, Void>>)
                        column -> {

                            return new TableCell<>() {

                                private final Button button =
                                        new Button("Retirer");


                                {

                                    button.setOnAction(
                                            event -> {

                                                TicketItem item =
                                                        getTableView()
                                                                .getItems()
                                                                .get(
                                                                        getIndex()
                                                                );


                                                cart.remove(item);


                                                updateTotal();


                                                codeField.requestFocus();
                                            }
                                    );
                                }


                                @Override
                                protected void updateItem(
                                        Void item,
                                        boolean empty
                                ) {

                                    super.updateItem(
                                            item,
                                            empty
                                    );


                                    setGraphic(
                                            empty
                                                    ? null
                                                    : button
                                    );
                                }
                            };
                        }
        );
    }


    // =========================================================
    // TOTAL
    // =========================================================

    private void updateTotal() {

        BigDecimal total =
                BigDecimal.ZERO;


        for (TicketItem item : cart) {

            total =
                    total.add(
                            item.lineTotal()
                    );
        }


        totalLabel.setText(
                money(total)
        );
    }


    // =========================================================
    // VALIDATE
    // =========================================================

    @FXML
    public void onValidate() {

        if (!AppState.getInstance().hasOpenJourney()) {

            AlertUtil.error(
                    "Aucune journée ouverte",
                    "Veuillez ouvrir une journée avant de créer un ticket."
            );

            SceneManager.show(
                    "/fxml/main_menu.fxml",
                    "Menu principal"
            );

            return;
        }


        if (cart.isEmpty()) {

            AlertUtil.error(
                    "Ticket vide",
                    "Ajoutez au moins un article avant de valider."
            );

            return;
        }


        if (paymentGroup.getSelectedToggle() == null) {

            AlertUtil.error(
                    "Mode de paiement requis",
                    "Veuillez sélectionner Espèces "
                            + "ou Carte bancaire."
            );

            return;
        }


        String paymentMethod =
                cashToggle.isSelected()
                        ? "CASH"
                        : "CARD";


        Ticket ticket =
                new Ticket();


        ticket.setJourneyId(
                AppState.getInstance()
                        .getCurrentJourney()
                        .getId()
        );


        ticket.setCashierId(
                AppState.getInstance()
                        .getCurrentUser()
                        .getId()
        );


        ticket.setPaymentMethod(
                paymentMethod
        );


        ticket.getItems().addAll(
                cart
        );


        // -----------------------------------------------------
        // DISABLE UI
        // -----------------------------------------------------

        progressIndicator.setVisible(true);

        codeField.setDisable(true);

        cashToggle.setDisable(true);

        cardToggle.setDisable(true);

        validateButton.setDisable(true);


        Task<Ticket> task =
                new Task<>() {

                    @Override
                    protected Ticket call()
                            throws Exception {

                        return ticketService
                                .saveTicket(ticket);
                    }
                };


        // -----------------------------------------------------
        // SUCCESS
        // -----------------------------------------------------

        task.setOnSucceeded(
                event -> {

                    progressIndicator.setVisible(false);


                    AlertUtil.info(
                            "Ticket validé",
                            "Ticket enregistré avec succès.\n\n"
                                    + "Total : "
                                    + money(ticket.total())
                                    + "\n"
                                    + "Paiement : "
                                    + ticket.getPaymentMethod()
                    );


                    cart.clear();

                    updateTotal();


                    paymentGroup.selectToggle(null);

                    cashToggle.setSelected(false);

                    cardToggle.setSelected(false);


                    validateButton.setDisable(true);


                    codeField.setDisable(false);

                    cashToggle.setDisable(false);

                    cardToggle.setDisable(false);


                    codeField.clear();

                    codeField.requestFocus();
                }
        );


        // -----------------------------------------------------
        // FAILURE
        // -----------------------------------------------------

        task.setOnFailed(
                event -> {

                    progressIndicator.setVisible(false);


                    Throwable exception =
                            task.getException();


                    AlertUtil.error(
                            "Erreur",
                            exception != null
                                    ? exception.getMessage()
                                    : "Échec de l'enregistrement du ticket."
                    );


                    codeField.setDisable(false);

                    cashToggle.setDisable(false);

                    cardToggle.setDisable(false);


                    validateButton.setDisable(
                            paymentGroup
                                    .getSelectedToggle()
                                    == null
                    );


                    codeField.requestFocus();
                }
        );


        Thread thread =
                new Thread(
                        task,
                        "save-ticket"
                );

        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // REPORTS
    // =========================================================

    @FXML
    public void onReports() {

        SceneManager.show(
                "/fxml/reports.fxml",
                "Rapports"
        );
    }


    // =========================================================
    // BACK
    // =========================================================

    @FXML
    public void onBack() {

        SceneManager.show(
                "/fxml/main_menu.fxml",
                "Menu principal"
        );
    }


    // =========================================================
    // MONEY
    // =========================================================

    private String money(BigDecimal value) {

        if (value == null) {
            value = BigDecimal.ZERO;
        }


        return value
                .setScale(
                        3,
                        RoundingMode.HALF_UP
                )
                + " DT";
    }
}