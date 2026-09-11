package com.caisse;

import com.caisse.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        SceneManager.init(stage);
        SceneManager.show("/fxml/login.fxml", "Connexion");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
