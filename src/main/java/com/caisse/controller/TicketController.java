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

import java.math.BigDecimal;
import java.util.List;

public class TicketController {

    // =========================================================
    // FXML FIELDS
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

    private final ProductService productService = new ProductService();
    private final TicketService ticketService = new TicketService();


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

        /*
         * Defense in depth:
         * A ticket cannot be created if there is no open journey.
         */
        if (!AppState.getInstance().hasOpenJourney()) {

            AlertUtil.error(
                    "Aucune journée ouverte",
                    "Aucune journée n'est ouverte.\n\n"
                            + "Veuillez ouvrir une journée avant de créer un ticket."
            );

            SceneManager.show(
                    "/fxml/main_menu.fxml",
                    "Menu principal"
            );

            return;
        }


        // =====================================================
        // JOURNEY
        // =====================================================

        if (AppState.getInstance().getCurrentJourney() != null) {

            journeyLabel.setText(
                    "Journée : "
                            + AppState.getInstance()
                            .getCurrentJourney()
                            .getId()
            );

        } else {

            journeyLabel.setText("Journée ouverte");

        }


        // =====================================================
        // PAYMENT SETUP
        // =====================================================

        cashToggle.setToggleGroup(paymentGroup);
        cardToggle.setToggleGroup(paymentGroup);

        /*
         * IMPORTANT:
         * No payment method is selected by default.
         */
        paymentGroup.selectToggle(null);

        /*
         * Validation button is hidden until
         * the cashier chooses a payment method.
         */
        validateButton.setVisible(false);
        validateButton.setManaged(false);


        /*
         * Listen for payment selection.
         */
        paymentGroup.selectedToggleProperty().addListener(
                (observable, oldToggle, newToggle) -> {

                    boolean paymentSelected =
                            newToggle != null;

                    validateButton.setVisible(paymentSelected);
                    validateButton.setManaged(paymentSelected);
                }
        );


        // =====================================================
        // CART TABLE
        // =====================================================

        cartTable.setItems(cart);


        colCode.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue()
                                .getProduct()
                                .getCode()
                )
        );


        colLabel.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue()
                                .getProduct()
                                .getLabel()
                )
        );


        colQty.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        String.valueOf(
                                cell.getValue().getQuantity()
                        )
                )
        );


        colPrice.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        money(
                                cell.getValue()
                                        .getUnitPrice()
                        )
                )
        );


        colDiscount.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue()
                                .getDiscountPercent()
                                + " %"
                )
        );


        colTotal.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        money(
                                cell.getValue().lineTotal()
                        )
                )
        );


        addRemoveButtonColumn();


        // =====================================================
        // SEARCH RESULTS
        // =====================================================

        searchResults.setCellFactory(
                listView -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Product product,
                            boolean empty
                    ) {

                        super.updateItem(product, empty);

                        if (empty || product == null) {

                            setText(null);

                        } else {

                            setText(
                                    product.getCode()
                                            + " — "
                                            + product.getLabel()
                                            + " (stock: "
                                            + product.getStockQuantity()
                                            + ", "
                                            + money(product.finalPrice())
                                            + ")"
                            );
                        }
                    }
                }
        );


        /*
         * Clicking a search result adds it to the cart.
         */
        searchResults.setOnMouseClicked(event -> {

            Product selected =
                    searchResults
                            .getSelectionModel()
                            .getSelectedItem();

            if (selected != null) {

                addToCart(selected);
            }
        });


        // =====================================================
        // INITIAL STATE
        // =====================================================

        updateTotal();
    }


    // =========================================================
    // ADD PRODUCT BY CODE
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


        Task<Product> task = new Task<>() {

            @Override
            protected Product call() throws Exception {

                return productService.findByCode(code);
            }
        };


        task.setOnSucceeded(event -> {

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
        });


        task.setOnFailed(event -> {

            Throwable exception =
                    task.getException();


            AlertUtil.error(
                    "Erreur",
                    exception != null
                            ? exception.getMessage()
                            : "Recherche échouée."
            );
        });


        Thread thread =
                new Thread(
                        task,
                        "find-product"
                );

        thread.setDaemon(true);
        thread.start();
    }


    // =========================================================
    // SEARCH PRODUCT
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


        task.setOnSucceeded(event -> {

            List<Product> products =
                    task.getValue();


            searchResults.setItems(
                    FXCollections.observableArrayList(
                            products
                    )
            );


            boolean hasResults =
                    products != null
                            && !products.isEmpty();


            searchResults.setVisible(hasResults);
            searchResults.setManaged(hasResults);
        });


        task.setOnFailed(event -> {

            Throwable exception =
                    task.getException();


            AlertUtil.error(
                    "Erreur",
                    exception != null
                            ? exception.getMessage()
                            : "Recherche échouée."
            );
        });


        Thread thread =
                new Thread(
                        task,
                        "search-products"
                );

        thread.setDaemon(true);
        thread.start();
    }


    // =========================================================
    // ADD PRODUCT TO CART
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


        /*
         * If the product is already in the cart,
         * increase its quantity.
         */
        for (TicketItem item : cart) {

            if (item.getProduct()
                    .getId()
                    .equals(product.getId())) {

                /*
                 * Don't allow quantity to exceed stock.
                 */
                if (item.getQuantity()
                        >= product.getStockQuantity()) {

                    AlertUtil.error(
                            "Stock insuffisant",
                            "La quantité disponible pour \""
                                    + product.getLabel()
                                    + "\" est de "
                                    + product.getStockQuantity()
                                    + "."
                    );

                    return;
                }


                item.setQuantity(
                        item.getQuantity() + 1
                );


                cartTable.refresh();
                updateTotal();

                return;
            }
        }


        /*
         * Product is not already in the cart.
         */
        cart.add(
                new TicketItem(
                        product,
                        1
                )
        );


        updateTotal();


        /*
         * Hide search results after adding.
         */
        searchResults.setVisible(false);
        searchResults.setManaged(false);


        codeField.clear();
        codeField.requestFocus();
    }


    // =========================================================
    // REMOVE PRODUCT
    // =========================================================

    private void addRemoveButtonColumn() {

        colRemove.setCellFactory(
                column -> new TableCell<>() {

                    private final Button button =
                            new Button("Retirer");


                    {
                        button.setOnAction(event -> {

                            TicketItem item =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());


                            if (item != null) {

                                cart.remove(item);
                                updateTotal();
                            }
                        });
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
                }
        );
    }


    // =========================================================
    // UPDATE TOTAL
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
    // VALIDATE TICKET
    // =========================================================

    @FXML
    public void onValidate() {

        // =====================================================
        // CHECK JOURNEY
        // =====================================================

        if (!AppState.getInstance().hasOpenJourney()) {

            AlertUtil.error(
                    "Aucune journée ouverte",
                    "Aucune journée n'est ouverte.\n\n"
                            + "Veuillez ouvrir une journée avant de créer un ticket."
            );

            SceneManager.show(
                    "/fxml/main_menu.fxml",
                    "Menu principal"
            );

            return;
        }


        // =====================================================
        // CHECK CART
        // =====================================================

        if (cart.isEmpty()) {

            AlertUtil.error(
                    "Ticket vide",
                    "Ajoutez au moins un article avant de valider."
            );

            return;
        }


        // =====================================================
        // CHECK PAYMENT
        // =====================================================

        Toggle selectedPayment =
                paymentGroup.getSelectedToggle();


        if (selectedPayment == null) {

            AlertUtil.error(
                    "Mode de paiement manquant",
                    "Veuillez choisir le mode de paiement du client."
            );

            return;
        }


        // =====================================================
        // DETERMINE PAYMENT METHOD
        // =====================================================

        String paymentMethod;


        if (selectedPayment == cashToggle) {

            paymentMethod = "CASH";

        } else if (selectedPayment == cardToggle) {

            paymentMethod = "CARD";

        } else {

            AlertUtil.error(
                    "Mode de paiement invalide",
                    "Veuillez sélectionner un mode de paiement."
            );

            return;
        }


        // =====================================================
        // CREATE TICKET
        // =====================================================

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


        ticket.getItems()
                .addAll(cart);


        // =====================================================
        // LOADING STATE
        // =====================================================

        progressIndicator.setVisible(true);

        validateButton.setDisable(true);
        cashToggle.setDisable(true);
        cardToggle.setDisable(true);
        codeField.setDisable(true);


        // =====================================================
        // SAVE TICKET
        // =====================================================

        Task<Ticket> task =
                new Task<>() {

                    @Override
                    protected Ticket call()
                            throws Exception {

                        return ticketService.saveTicket(
                                ticket
                        );
                    }
                };


        // =====================================================
        // SUCCESS
        // =====================================================

        task.setOnSucceeded(event -> {

            progressIndicator.setVisible(false);

            validateButton.setDisable(false);
            cashToggle.setDisable(false);
            cardToggle.setDisable(false);
            codeField.setDisable(false);


            AlertUtil.info(
                    "Ticket validé",
                    "Ticket enregistré avec succès.\n\n"
                            + "Total : "
                            + money(ticket.total())
                            + "\n"
                            + "Paiement : "
                            + paymentLabel(paymentMethod)
            );


            // Clear cart
            cart.clear();

            updateTotal();


            // =================================================
            // RESET PAYMENT
            // =================================================

            paymentGroup.selectToggle(null);

            cashToggle.setSelected(false);
            cardToggle.setSelected(false);


            /*
             * Hide validation button again.
             */
            validateButton.setVisible(false);
            validateButton.setManaged(false);


            codeField.clear();
            codeField.requestFocus();
        });


        // =====================================================
        // FAILURE
        // =====================================================

        task.setOnFailed(event -> {

            progressIndicator.setVisible(false);

            validateButton.setDisable(false);
            cashToggle.setDisable(false);
            cardToggle.setDisable(false);
            codeField.setDisable(false);


            Throwable exception =
                    task.getException();


            AlertUtil.error(
                    "Erreur",
                    exception != null
                            ? exception.getMessage()
                            : "Échec de l'enregistrement du ticket."
            );
        });


        Thread thread =
                new Thread(
                        task,
                        "save-ticket"
                );


        thread.setDaemon(true);
        thread.start();
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
    // PAYMENT LABEL
    // =========================================================

    private String paymentLabel(
            String paymentMethod
    ) {

        if ("CASH".equals(paymentMethod)) {
            return "Espèces";
        }

        if ("CARD".equals(paymentMethod)) {
            return "Carte bancaire";
        }

        return paymentMethod;
    }


    // =========================================================
    // MONEY FORMAT
    // =========================================================

    private String money(BigDecimal value) {

        if (value == null) {
            value = BigDecimal.ZERO;
        }


        return value
                .setScale(
                        3,
                        java.math.RoundingMode.HALF_UP
                )
                + " DT";
    }
}