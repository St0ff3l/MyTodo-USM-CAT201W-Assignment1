package com.mytodo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 650);

        // 确保加载 CSS 样式
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

        stage.setTitle("Smart Todo v2.1");
        stage.setScene(scene);
        stage.show();

        // 确保在应用关闭时保存数据 (Part 4)
        MainController controller = loader.getController();
        stage.setOnCloseRequest(e -> controller.saveAndExit());
    }

    public static void main(String[] args) {
        launch();
    }
}