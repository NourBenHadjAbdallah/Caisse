package com.caisse.controller;

import com.caisse.service.AuthService;
import com.caisse.state.AppState;
import com.caisse.util.AlertUtil;
import com.caisse.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainMenuController {

    @FXML
    private Label cashierLabel;

    @FXML
    private Label journeyBadge;

    @FXML
    private Button ticketsButton;

    @FXML
    private Button returnsButton;


    private final AuthService authService =
            new AuthService();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        AppState state =
                AppState.getInstance();


        /*
         * Display cashier code.
         */
        if (state.getCurrentUser() != null) {

            cashierLabel.setText(
                    state.getCurrentUser()
                            .getCashierCode()
            );
        }


        /*
         * Update journey status.
         */
        refreshBadge();


        /*
         * Automatically refresh menu when journey state changes.
         */
        state.journeyStateProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                refreshBadge()
                );
    }


    // =========================================================
    // JOURNEY STATUS
    // =========================================================

    private void refreshBadge() {

        boolean open =
                AppState.getInstance()
                        .hasOpenJourney();


        /*
         * Journey badge.
         */
        journeyBadge.setText(
                open
                        ? "Journée : OUVERTE"
                        : "Journée : FERMÉE"
        );


        journeyBadge
                .getStyleClass()
                .removeAll(
                        "badge-open",
                        "badge-closed"
                );


        journeyBadge
                .getStyleClass()
                .add(
                        open
                                ? "badge-open"
                                : "badge-closed"
                );


        /*
         * IMPORTANT:
         *
         * Tickets and Retours stay visible.
         *
         * They simply become disabled when
         * there is no open journey.
         */
        ticketsButton.setDisable(!open);

        returnsButton.setDisable(!open);
    }


    // =========================================================
    // JOURNEY
    // =========================================================

    @FXML
    public void onJourney() {

        SceneManager.show(
                "/fxml/journey_menu.fxml",
                "Journée"
        );
    }


    // =========================================================
    // TICKETS
    // =========================================================

    @FXML
    public void onTickets() {

        /*
         * Extra safety check.
         */
        if (!AppState.getInstance().hasOpenJourney()) {

            AlertUtil.error(
                    "Aucune journée ouverte",
                    "Veuillez ouvrir une journée avant de créer un ticket."
            );

            return;
        }


        SceneManager.show(
                "/fxml/tickets.fxml",
                "Tickets"
        );
    }


    // =========================================================
    // RETURNS
    // =========================================================

    @FXML
    public void onReturns() {

        /*
         * Extra safety check.
         */
        if (!AppState.getInstance().hasOpenJourney()) {

            AlertUtil.error(
                    "Aucune journée ouverte",
                    "Veuillez ouvrir une journée avant de traiter un retour."
            );

            return;
        }


        SceneManager.show(
                "/fxml/returns.fxml",
                "Retours"
        );
    }


    // =========================================================
    // STOCK
    // =========================================================

    @FXML
    public void onStock() {

        SceneManager.show(
                "/fxml/stock.fxml",
                "Stock"
        );
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
    // LOGOUT
    // =========================================================

    @FXML
    public void onLogout() {

        authService.logout();

        AppState.getInstance().reset();

        SceneManager.show(
                "/fxml/login.fxml",
                "Connexion"
        );
    }
}