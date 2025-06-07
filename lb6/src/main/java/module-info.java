module ru.nstu.laba1timp {
        requires javafx.controls;
        requires javafx.fxml;
        requires javafx.media;
        requires javafx.graphics;
        requires javafx.base;
        requires java.sql;
        // Открываем пакеты контроллеров для FXML
        opens ru.nstu.laba1timp.controllers to javafx.fxml;
        opens ru.nstu.laba1timp.console to javafx.fxml;

        // Открываем модели
        opens ru.nstu.laba1timp.model to javafx.base;

        // Экспортируем необходимые пакеты
        //exports ru.nstu.laba1timp;
        exports ru.nstu.laba1timp.controllers;
        exports ru.nstu.laba1timp.console;
        exports ru.nstu.laba1timp.model;
        exports ru.nstu.laba1timp.main;
}