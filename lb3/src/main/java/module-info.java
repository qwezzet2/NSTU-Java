module ru.nstu.laba1timp {
    requires javafx.controls; // Модуль для JavaFX UI
    requires javafx.fxml;     // Модуль для FXML
    requires javafx.media;    // Модуль для работы с медиа (музыка, видео)

    opens ru.nstu.laba1timp to javafx.fxml; // Открываем пакет для JavaFX
    exports ru.nstu.laba1timp; // Экспортируем пакет
}

