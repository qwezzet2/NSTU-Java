// MainApplication.java
package ru.nstu.laba1timp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainApplication extends Application {
    private Habitat hab;
    private Statistics stats;
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ru/nstu/laba1timp/Main.fxml"));
        Parent root = fxmlLoader.load();

        Controller controller = fxmlLoader.getController();
        Statistics.getInstance().setMainController(controller);
        hab = Habitat.getInstance();
        stats = Statistics.getInstance();

        Scene scene = new Scene(root, hab.getWidth(), hab.getHeight());
        scene.getRoot().requestFocus();
        stage.setMaximized(false); // Запуск на весь экран или нет?
        stage.setTitle("Симуляция рабочего коллектива");
        stage.setScene(scene);
        stage.setOnCloseRequest(t -> System.exit(0)); // Остановка приложения по нажатию крестика
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}