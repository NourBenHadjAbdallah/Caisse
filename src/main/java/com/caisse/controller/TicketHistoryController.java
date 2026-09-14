package com.caisse.controller;

import com.caisse.model.TicketSummary;
import com.caisse.service.TicketService;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TicketHistoryController {

    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private Label rangeLabel;
    @FXML private TableView<TicketSummary> ticketsTable;
    @FXML private TableColumn<TicketSummary, String> colDate;
    @FXML private TableColumn<TicketSummary, String> colTotal;
    @FXML private TableColumn<TicketSummary, String> colPayment;
    @FXML private TableColumn<TicketSummary, Void> colOpen;
    @FXML private ProgressIndicator progressIndicator;

    private final TicketService ticketService = new TicketService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : "-"));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().getTotal())));
        colPayment.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));

        colOpen.setCellFactory((Callback<TableColumn<TicketSummary, Void>, TableCell<TicketSummary, Void>>) column -> new TableCell<>() {
            private final Button button = new Button("Ouvrir");
            {
                button.getStyleClass().add("btn-primary");
                button.setOnAction(event -> {
                    TicketSummary ticket = getTableView().getItems().get(getIndex());
                    AppState.getInstance().setSelectedTicketId(ticket.getId());
                    SceneManager.show("/fxml/ticket_detail.fxml", "Ticket");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });

        // Default view: TODAY only.
        LocalDate today = LocalDate.now();
        fromPicker.setValue(today);
        toPicker.setValue(today);

        loadTickets(today, today);
    }

    @FXML
    public void onFilter() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();

        if (from == null || to == null || from.isAfter(to)) {
            AlertUtil.error("Période invalide", "Veuillez choisir une période valide (du / au).");
            return;
        }

        loadTickets(from, to);
    }

    @FXML
    public void onToday() {
        LocalDate today = LocalDate.now();
        fromPicker.setValue(today);
        toPicker.setValue(today);
        loadTickets(today, today);
    }

    private void loadTickets(LocalDate from, LocalDate to) {
        progressIndicator.setVisible(true);

        String cashierId = AppState.getInstance().getCurrentUser() != null
                ? AppState.getInstance().getCurrentUser().getId()
                : null;

        Task<List<TicketSummary>> task = new Task<>() {
            @Override
            protected List<TicketSummary> call() throws Exception {
                return ticketService.listTicketsForCashier(cashierId, from, to);
            }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            List<TicketSummary> tickets = task.getValue();
            ticketsTable.setItems(FXCollections.observableArrayList(tickets));

            String period = from.equals(to) ? from.toString() : from + " → " + to;
            rangeLabel.setText(tickets.size() + " ticket(s)\n" + period);
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            AlertUtil.error("Erreur",
                    task.getException() != null ? task.getException().getMessage() : "Échec du chargement de l'historique.");
        });
        new Thread(task, "load-ticket-history").start();
    }

    @FXML
    public void onBack() {
        SceneManager.show("/fxml/tickets.fxml", "Tickets");
    }

    private String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return v.setScale(3, RoundingMode.HALF_UP) + " DT";
    }
}