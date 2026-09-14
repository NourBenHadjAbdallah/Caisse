package com.caisse.util;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {}

    public static void init(Stage stage) {
        primaryStage = stage;

        // Size the window to the device's screen right from the start.
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
    }

    public static void show(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Always use the device's current screen size, regardless of the
            // prefWidth/prefHeight declared in the individual FXML file.
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

            Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
            scene.getStylesheets().add(SceneManager.class.getResource("/css/style.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setTitle("Caisse — " + title);

            primaryStage.setX(bounds.getMinX());
            primaryStage.setY(bounds.getMinY());
            primaryStage.setWidth(bounds.getWidth());
            primaryStage.setHeight(bounds.getHeight());
            primaryStage.setMaximized(true);

            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger " + fxmlPath, e);
        }
    }

    public static Stage getStage() { return primaryStage; }
}