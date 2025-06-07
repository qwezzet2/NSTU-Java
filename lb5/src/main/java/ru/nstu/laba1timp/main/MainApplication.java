package ru.nstu.laba1timp.main; // <<< ПАКЕТ НЕ ИЗМЕНЕН

import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
// Импортируем классы из правильного пакета
import ru.nstu.laba1timp.controllers.Habitat;
import ru.nstu.laba1timp.controllers.Statistics;
import ru.nstu.laba1timp.controllers.Controller;
import ru.nstu.laba1timp.controllers.FileMaster; // <<< Импорт FileMaster из корневого пакета
import javafx.application.Application;
import javafx.application.Platform;
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
    private Controller controller;

    @Override
    public void start(Stage stage) throws IOException {
        // Загрузка FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ru/nstu/laba1timp/Main.fxml"));
        Parent root = fxmlLoader.load();
        controller = fxmlLoader.getController(); // Получаем контроллер

        // Инициализация синглтонов и передача контроллера
        stats = Statistics.getInstance();
        stats.setMainController(controller); // Важно ДО loadConfig, который вызывается в controller.initialize()

        hab = Habitat.getInstance();

        // Инициализация AI
        developerAI = DeveloperAI.getInstance();
        managerAI = ManagerAI.getInstance();

        // Установка приоритетов из контроллера (после того как он их загрузил из конфига в initialize)
        if (controller != null && controller.boxDevPriority != null && controller.boxDevPriority.getValue() != null) {
            developerAI.setPriority(controller.boxDevPriority.getValue());
        }
        if (controller != null && controller.boxManPriority != null && controller.boxManPriority.getValue() != null) {
            managerAI.setPriority(controller.boxManPriority.getValue());
        }

        // Запуск AI потоков (они начнут работать только когда isActive станет true)
        developerAI.start();
        managerAI.start();

        // Настройка сцены
        Scene scene = new Scene(root, hab.getWidth() + 210, hab.getHeight());
        scene.getRoot().requestFocus();

        // Настройка окна
        stage.setMaximized(false);
        stage.setTitle("Симуляция рабочего коллектива");
        stage.setScene(scene);

        // Обработчик закрытия окна
        stage.setOnCloseRequest(event -> {
            System.out.println("Закрытие окна...");
            // Сохранение конфигурации
            if (controller != null) {
                FileMaster.saveConfig(this.controller);
            } else {
                System.err.println("Контроллер null, конфигурация не сохранена.");
            }
            // Остановка таймера статистики
            if (stats != null && stats.timer != null) {
                stats.timer.cancel();
            }
            // Прерывание AI потоков
            if (developerAI != null) {
                developerAI.interrupt();
            }
            if (managerAI != null) {
                managerAI.interrupt();
            }
            // Корректное завершение JavaFX и JVM
            Platform.exit();
            System.exit(0);
        });

        // Показ окна
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Этот метод вызывается после закрытия окна
        System.out.println("Application Stop method called.");
        super.stop();
        // Здесь можно добавить дополнительную логику очистки, если требуется,
        // но основные действия (сохранение, остановка потоков) лучше делать в setOnCloseRequest
    }

    public static void main(String[] args) {
        launch(args);
    }
}