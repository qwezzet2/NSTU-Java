package ru.nstu.laba1timp.console;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;

public class ConsoleWindow {

    public ConsoleWindow(Window owner) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        // Грузим FXML из ресурсов относительно текущего пакета
        URL fxmlUrl = getClass().getResource("/ru/nstu/laba1timp/Console.fxml");
        if (fxmlUrl == null) {
            throw new IOException("Не удалось найти файл Console.fxml");
        }
        fxmlLoader.setLocation(fxmlUrl);

        // Загружаем сцену
        Scene scene = new Scene(fxmlLoader.load(), 700, 450); // Задаем размер окна

        // Создаем новое окно (Stage)
        Stage stage = new Stage();
        stage.setTitle("Консоль управления");
        stage.setScene(scene);

        // Опционально: делаем окно модальным или указываем владельца
        if (owner != null) {
            stage.initOwner(owner);
            // stage.initModality(Modality.WINDOW_MODAL); // Раскомментировать, если нужно блокировать основное окно
        }

        // Показываем окно
        stage.show();
    }
}