package ru.nstu.laba1timp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextFlow;

public class Controller {
    @FXML
    private Label labelTimer; // Метка для отображения времени
    @FXML
    private TextFlow statisticTextFlow; // TextFlow для отображения статистики
    @FXML
    private Pane visualizationPane; // Панель для визуализации объектов
    @FXML
    private ImageView resultImageView; // ImageView для отображения фотографии

    public Label getLabelTimer() {
        return labelTimer;
    }

    public TextFlow getStatisticTextFlow() {
        return statisticTextFlow;
    }

    public Pane getPane() {
        return visualizationPane;
    }

    public ImageView getResultImageView() {
        return resultImageView;
    }

    @FXML
    void keyPressed(KeyEvent keyEvent) {
        keyEvent.consume();
        Statistics st = Statistics.getInstance();

        switch (keyEvent.getCode()) {
            case T: // Переключение видимости таймера
                st.showTimer();
                break;
            case B: // Запуск симуляции
                if (!st.startFlag) {
                    st.startAction();
                }
                break;
            case E: // Остановка симуляции
                if (st.startFlag) {
                    st.stopAction();
                }
                break;
        }
    }
}