package com.mytodo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Force application-wide locale to English so JavaFX built-in dialogs use English labels
        Locale.setDefault(Locale.ENGLISH);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 650);

        // Ensure CSS is loaded
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

        // Window title
        stage.setTitle("MyTodo");
        stage.setScene(scene);
        stage.show();

        // Ensure data saved on exit
        MainController controller = loader.getController();
        stage.setOnCloseRequest(e -> controller.saveAndExit());
    }

    public static void main(String[] args) {
        launch();
    }
}
