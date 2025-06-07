module ru.nstu.laba1timp { // Убедитесь, что имя модуля ваше
        requires javafx.controls;
        requires javafx.fxml;
        requires javafx.media;
        requires javafx.graphics; // Добавляем для полноты
        requires javafx.base;     // Добавляем для полноты

        // Открываем пакеты контроллеров для FXML
        opens ru.nstu.laba1timp.controllers to javafx.fxml; // Для основного Controller
        opens ru.nstu.laba1timp.console to javafx.fxml;   // <<< ЭТО КЛЮЧЕВАЯ СТРОКА >>>

        // Открываем модели (если используются JavaFX Properties)
        opens ru.nstu.laba1timp.model to javafx.base;

        // Экспортируем необходимые пакеты
        //exports ru.nstu.laba1timp; // Для Habitat, Statistics, FileMaster, ConsoleWindow
        exports ru.nstu.laba1timp.controllers;
        exports ru.nstu.laba1timp.console;
        exports ru.nstu.laba1timp.model;
        exports ru.nstu.laba1timp.main; // Для запуска MainApplication
}