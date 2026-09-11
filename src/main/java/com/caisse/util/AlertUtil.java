package com.caisse.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class AlertUtil {

    private AlertUtil() {}

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    public static void error(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        Optional<ButtonType> result = a.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
