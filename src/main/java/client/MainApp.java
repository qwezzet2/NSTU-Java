package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private final String userType;
    private final boolean isAdmin;

    public MainApp(String userType, boolean isAdmin) {
        this.userType = userType;
        this.isAdmin = isAdmin;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            // Получаем контроллер и передаем параметры
            ArtworkController controller = loader.getController();
            controller.initData(userType, isAdmin);

            Scene scene = new Scene(root, 1000, 700); // Увеличили размер окна
            primaryStage.setTitle("Выставка произведений искусства | " + userType + (isAdmin ? " (Администратор)" : " (Пользователь)"));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);

            primaryStage.show();

            primaryStage.setOnCloseRequest(event -> controller.stopRefresh());
        } catch (Exception e) {
            System.err.println("Ошибка при запуске приложения: " + e.getMessage());
            throw e;
        }
    }
}