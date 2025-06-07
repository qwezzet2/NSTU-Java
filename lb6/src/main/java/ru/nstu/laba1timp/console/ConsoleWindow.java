package ru.nstu.laba1timp.console;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;

/**
 * Класс для создания и отображения консольного окна управления.
 * Обеспечивает загрузку FXML-разметки и настройку параметров окна.
 */
public class ConsoleWindow {

    // Создает и отображает новое консольное окно.


    public ConsoleWindow(Window owner) throws IOException {
        // Инициализация загрузчика FXML
        FXMLLoader fxmlLoader = new FXMLLoader();

        // Поиск FXML-файла в ресурсах
        URL fxmlUrl = getClass().getResource("/ru/nstu/laba1timp/Console.fxml");
        if (fxmlUrl == null) {
            throw new IOException("Не удалось найти файл Console.fxml");
        }
        fxmlLoader.setLocation(fxmlUrl);

        // Создание сцены с заданными размерами
        Scene scene = new Scene(fxmlLoader.load(), 700, 450);

        // Настройка параметров основного окна
        Stage stage = new Stage();
        stage.setTitle("Консоль управления");
        stage.setScene(scene);

        // Установка связи с родительским окном, если оно указано
        if (owner != null) {
            stage.initOwner(owner);

        }

        // Отображение окна пользователю
        stage.show();
    }
}