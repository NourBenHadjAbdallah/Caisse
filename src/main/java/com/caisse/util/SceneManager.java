package com.caisse.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {}

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void show(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(SceneManager.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Caisse — " + title);
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger " + fxmlPath, e);
        }
    }

    public static Stage getStage() { return primaryStage; }
}
