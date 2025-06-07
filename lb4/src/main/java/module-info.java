module ru.nstu.laba1timp {
        requires javafx.controls;
        requires javafx.fxml;
        requires javafx.graphics;
        requires javafx.media;

        opens ru.nstu.laba1timp to javafx.fxml;
        exports ru.nstu.laba1timp;
        exports ru.nstu.laba1timp.model;
        opens ru.nstu.laba1timp.model to javafx.fxml;
}