package ru.nstu.laba1timp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("Main.fxml"));
        Parent root = fxmlLoader.load();

        // Инициализация Habitat и Statistics
        Habitat habitat = Habitat.getInstance();
        Habitat.setInstance(habitat);

        Statistics statistics = new Statistics(fxmlLoader.getController());
        Statistics.setInstance(statistics);

        // Создание сцены
        Scene scene = new Scene(root, habitat.getWidth(), habitat.getHeight());
        scene.getRoot().requestFocus();

        // Настройка окна
        stage.setTitle("Симуляция рабочего коллектива");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}