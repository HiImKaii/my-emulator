package com.j2me.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    private com.j2me.launcher.controller.MainController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        primaryStage.setTitle("J2ME Launcher");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/app_icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();

        primaryStage.setOnCloseRequest(e -> {
            if (controller != null) controller.shutdown();
        });

        primaryStage.show();
        System.out.println("J2ME Launcher started");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
