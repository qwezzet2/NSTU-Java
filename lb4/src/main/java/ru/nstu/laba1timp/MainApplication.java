// ru.nstu.laba1timp.MainApplication.java
package ru.nstu.laba1timp;

import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainApplication extends Application {
    private Habitat hab;
    private Statistics stats;
    private DeveloperAI developerAI;
    private ManagerAI managerAI;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ru/nstu/laba1timp/Main.fxml"));
        Parent root = fxmlLoader.load();

        Controller controller = fxmlLoader.getController();
        Statistics.getInstance().setMainController(controller);
        hab = Habitat.getInstance();
        stats = Statistics.getInstance();

        developerAI = DeveloperAI.getInstance();
        managerAI = ManagerAI.getInstance();
        developerAI.start();
        managerAI.start();

        // Use dimensions from Habitat which are now corrected based on FXML
        Scene scene = new Scene(root, hab.getWidth() + 210, hab.getHeight());
        scene.getRoot().requestFocus();
        stage.setMaximized(false);
        stage.setTitle("Симуляция рабочего коллектива");
        stage.setScene(scene);
        stage.setOnCloseRequest(t -> System.exit(0));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}