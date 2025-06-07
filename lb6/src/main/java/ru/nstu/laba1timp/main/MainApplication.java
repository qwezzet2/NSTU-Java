package ru.nstu.laba1timp.main;

import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
import ru.nstu.laba1timp.controllers.Habitat;
import ru.nstu.laba1timp.controllers.Statistics;
import ru.nstu.laba1timp.controllers.Controller;
import ru.nstu.laba1timp.controllers.FileMaster;
import ru.nstu.laba1timp.api.Client;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class MainApplication extends Application {

    private Habitat hab;
    private Statistics stats;
    private DeveloperAI developerAI;
    private ManagerAI managerAI;
    private Controller uiController; // Переименовал для ясности, что это контроллер UI

    @Override
    public void start(Stage stage) throws IOException {

        // Шаг 1: Показываем диалог входа.
        // connectionSuccess будет true ТОЛЬКО если пользователь нажал "Подключиться" И соединение установилось.
        boolean connectionSuccess = Client.showLoginDialogAndAttemptConnect();

        // Шаг 2: Загрузка FXML и получение контроллера UI в любом случае
        URL fxmlUrl = getClass().getResource("/ru/nstu/laba1timp/Main.fxml");
        if (fxmlUrl == null) {
            System.err.println("Критическая ошибка: FXML файл Main.fxml не найден!");
            // Можно показать Alert здесь, если Platform уже инициализирована
            Platform.exit();
            System.exit(1);
            return;
        }
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Parent root = fxmlLoader.load();
        uiController = fxmlLoader.getController(); // Получаем контроллер UI

        // Шаг 3: Устанавливаем контроллер UI в Client API.
        // Это нужно сделать до любых вызовов Client.updateConnectionStatusUI() или других методов Client,
        // которые могут попытаться обновить UI.
        Client.setController(uiController);

        // Шаг 4: Инициализация статистики и передача ей контроллера UI
        stats = Statistics.getInstance();
        stats.setMainController(uiController);

        String windowTitle = "Симуляция";

        // Шаг 5: Настройка UI в зависимости от результата диалога и подключения
        if (connectionSuccess) {
            // Пользователь выбрал подключение, и оно было успешным
            System.out.println("MainApplication: Пользователь успешно подключился к серверу.");
            // Client.updateConnectionStatusUI уже был вызван из Client.connectToServer
            // при успешном подключении, если Client.controller (теперь uiController) был установлен.
            // Но для надежности можно вызвать еще раз, если предыдущий вызов был до установки контроллера.
            Client.updateConnectionStatusUI(true, "Подключен как " + Client.getUserName());
            windowTitle += " [" + Client.getUserName() + " - Онлайн]";
        } else {
            // Пользователь выбрал "Работать оффлайн", или закрыл диалог,
            // или попытка подключения через кнопку "Подключиться" не удалась.
            System.out.println("MainApplication: Запуск в оффлайн-режиме (или ошибка подключения). Имя: " + Client.getUserName());
            Client.updateConnectionStatusUI(false, "Оффлайн");
            windowTitle += " [" + (Client.getUserName() != null ? Client.getUserName() : "Пользователь") + " - Оффлайн]";
        }


        // Шаг 6: Инициализация остальных компонентов симуляции
        hab = Habitat.getInstance();
        developerAI = DeveloperAI.getInstance();
        managerAI = ManagerAI.getInstance();
        if (!developerAI.isAlive()) { developerAI.setDaemon(true); developerAI.start(); }
        if (!managerAI.isAlive()) { managerAI.setDaemon(true); managerAI.start(); }

        // Шаг 7: Настройка и отображение главного окна
        Scene scene = new Scene(root, hab.getWidth() + 210, hab.getHeight());
        scene.getRoot().requestFocus(); // Для обработки нажатий клавиш
        stage.setMaximized(false);
        stage.setTitle(windowTitle);
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            System.out.println("Запрос на закрытие окна...");
            if (Client.isConnected()) {
                Client.disconnectFromServer();
            }
            if (stats != null) {
                if (stats.timer != null) {
                    stats.timer.cancel();
                    stats.timer = null;
                }
                stats.stopMusic();
            }
            // Сохраняем конфигурацию контроллера UI
            if (uiController != null) {
                FileMaster.saveConfig(this.uiController);
            }
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Этот метод вызывается при нормальном завершении работы JavaFX приложения,
        // но Platform.exit() и System.exit(0) в setOnCloseRequest более явно контролируют завершение.
        System.out.println("MainApplication stop() called.");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}