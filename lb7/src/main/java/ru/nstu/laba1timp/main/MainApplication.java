package ru.nstu.laba1timp.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import ru.nstu.laba1timp.controllers.Controller;
import ru.nstu.laba1timp.controllers.DatabaseManager; // Импорт менеджера БД
import ru.nstu.laba1timp.controllers.FileMaster;
import ru.nstu.laba1timp.controllers.Habitat;
import ru.nstu.laba1timp.controllers.Statistics;
import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Основной класс приложения.
 * Отвечает за запуск JavaFX-приложения, инициализацию компонентов,
 * обработку закрытия окна и взаимодействие с базой данных.
 */
public class MainApplication extends Application {

    private Habitat hab;             // Среда обитания объектов
    private Statistics stats;        // Класс статистики и управления временем
    private DeveloperAI developerAI; // AI-поток разработчика
    private ManagerAI managerAI;     // AI-поток менеджера
    private Controller controller;   // Главный контроллер UI
    private DatabaseManager dbManager; // Менеджер взаимодействия с БД

    /**
     * Точка входа в приложение.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // <<< НАЧАЛО ИЗМЕНЕНИЙ: Инициализация DatabaseManager >>>
        try {
            dbManager = new DatabaseManager(); // Подключение к БД SQLite
        } catch (SQLException e) {
            System.err.println("КРИТИЧЕСКАЯ ОШИБКА: Не удалось инициализировать менеджер базы данных.");
            e.printStackTrace();

            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Ошибка Базы Данных");
            errorAlert.setHeaderText("Не удалось подключиться к базе данных сохранений.");
            errorAlert.setContentText("Приложение не может продолжить работу.\nПодробности: " + e.getMessage());
            errorAlert.showAndWait();

            Platform.exit(); // Завершаем работу JavaFX
            return; // Прекращаем выполнение метода start()
        }
        // <<< КОНЕЦ ИЗМЕНЕНИЙ >>>

        // Загрузка FXML-интерфейса
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ru/nstu/laba1timp/Main.fxml"));
        Parent root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        // <<< НАЧАЛО ИЗМЕНЕНИЙ: Передача dbManager в Controller >>>
        if (controller != null) {
            controller.setDatabaseManager(dbManager); // Устанавливаем ссылку на менеджер БД
        }
        // <<< КОНЕЦ ИЗМЕНЕНИЙ >>>

        // Получаем или создаем экземпляры основных компонентов
        stats = Statistics.getInstance();
        stats.setMainController(controller);

        hab = Habitat.getInstance();
        developerAI = DeveloperAI.getInstance();
        managerAI = ManagerAI.getInstance();

        // Устанавливаем приоритеты потоков AI из интерфейса, если доступны
        if (controller != null && controller.boxDevPriority != null && controller.boxDevPriority.getValue() != null) {
            developerAI.setPriority(controller.boxDevPriority.getValue());
        }
        if (controller != null && controller.boxManPriority != null && controller.boxManPriority.getValue() != null) {
            managerAI.setPriority(controller.boxManPriority.getValue());
        }

        // Запускаем AI-потоки
        developerAI.start();
        managerAI.start();

        // Создание и настройка сцены
        Scene scene = new Scene(root, hab.getWidth() + 210, hab.getHeight());
        scene.getRoot().requestFocus();

        // Настройка главного окна
        stage.setMaximized(false);
        stage.setTitle("Симуляция рабочего коллектива");
        stage.setScene(scene);

        // Обработчик закрытия окна
        stage.setOnCloseRequest(event -> {
            System.out.println("Закрытие окна...");

            // Сохраняем конфигурацию
            if (controller != null) {
                FileMaster.saveConfig(this.controller);
            } else {
                System.err.println("Контроллер null, конфигурация не сохранена.");
            }

            // Останавливаем таймер
            if (stats != null && stats.timer != null) {
                stats.timer.cancel();
            }

            // Прерываем AI-потоки
            if (developerAI != null) {
                developerAI.interrupt();
            }
            if (managerAI != null) {
                managerAI.interrupt();
            }

            // <<< НАЧАЛО ИЗМЕНЕНИЙ: Закрытие соединения с БД >>>
            if (dbManager != null) {
                dbManager.close();
                System.out.println("Соединение DatabaseManager закрыто.");
            }
            // <<< КОНЕЦ ИЗМЕНЕНИЙ >>>

            // Завершаем приложение
            Platform.exit();
            System.exit(0);
        });

        // Показываем главное окно
        stage.show();
    }

    /**
     * Вызывается при завершении работы приложения.
     * Может быть использован для дополнительной очистки ресурсов.
     */
    @Override
    public void stop() throws Exception {
        System.out.println("Application Stop method called.");

        // Если не был вызван on-close handler, можно закрыть БД здесь
        // (опционально, зависит от логики)
        if (dbManager != null) {
            // dbManager.close(); // Можно раскомментировать, если нужно
        }

        super.stop();
    }

    /**
     * Точка входа в приложение.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
