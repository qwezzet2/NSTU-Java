package ru.nstu.laba1timp;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;

public class Statistics {
    public Timer timer;
    public boolean timeFlag, startFlag;
    private boolean statisticFlag;
    private long startTime;
    private int seconds = 0, minutes = 0;
    private Controller mainController; // Поле для хранения контроллера
    private static Statistics instance;
    private MediaPlayer mediaPlayer; // MediaPlayer для воспроизведения музыки

    public Statistics(Controller mainController) {
        this.mainController = mainController;

        // Загрузка музыки
        URL musicUrl = getClass().getResource("/music.mp3");
        if (musicUrl != null) {
            Media media = new Media(musicUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.5); // Установка громкости на 50%
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Бесконечное повторение
        }
    }

    public static void setInstance(Statistics instance) {
        Statistics.instance = instance;
    }

    public static Statistics getInstance() {
        return instance;
    }

    public Controller getMainController() {
        return mainController;
    }

    public void showStatisticLabel() {
        if (statisticFlag) {
            // Очищаем TextFlow перед добавлением новых элементов
            mainController.getStatisticTextFlow().getChildren().clear();

            // Создаем текстовые элементы с одинаковым стилем
            Text developersText = new Text("Разработчики: " + Developer.count + "\n");
            Text managersText = new Text("Менеджеры: " + Manager.count + "\n");
            Text timeText = new Text("Время: " + (System.currentTimeMillis() - startTime) / 1000 + " сек");

            // Устанавливаем стиль для всех текстовых элементов
            Font comicSansFont = Font.font("Comic Sans MS", FontWeight.NORMAL, 16); // Шрифт Comic Sans MS
            if (comicSansFont == null) {
                comicSansFont = Font.font("Arial", FontWeight.NORMAL, 16); // Запасной шрифт
            }

            developersText.setFont(comicSansFont);
            managersText.setFont(comicSansFont);
            timeText.setFont(comicSansFont);

            developersText.setFill(Color.BLACK); // Черный цвет
            managersText.setFill(Color.BLACK); // Черный цвет
            timeText.setFill(Color.BLACK); // Черный цвет

            // Добавляем текстовые элементы в TextFlow
            mainController.getStatisticTextFlow().getChildren().addAll(developersText, managersText, timeText);

            // Показываем TextFlow и картинку
            mainController.getStatisticTextFlow().setVisible(true);
            mainController.getResultImageView().setVisible(true);
            mainController.getLabelTimer().setVisible(false); // Скрываем таймер
        } else {
            // Скрываем TextFlow и картинку
            mainController.getStatisticTextFlow().setVisible(false);
            mainController.getResultImageView().setVisible(false);
            mainController.getLabelTimer().setVisible(true); // Показываем таймер
        }
    }

    public void showTimer() {
        timeFlag = !timeFlag;
        if (timeFlag) {
            mainController.getLabelTimer().setVisible(true);
        } else {
            mainController.getLabelTimer().setVisible(false);
        }
    }

    public void updateTimer() {
        String time = String.format("%02d:%02d", minutes, seconds);
        mainController.getLabelTimer().setText(time);
        mainController.getLabelTimer().setTextFill(Color.BLACK); // Устанавливаем черный цвет текста
        mainController.getLabelTimer().setFont(Font.font("Comic Sans MS", FontWeight.NORMAL, 20)); // Устанавливаем шрифт Comic Sans
    }

    public void startAction() {
        startFlag = timeFlag = true;
        statisticFlag = false;
        seconds = -1;
        minutes = 0;
        timer = new Timer();
        showStatisticLabel();
        startTime = System.currentTimeMillis();

        // Запуск музыки
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }

        // Запуск таймера
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                seconds++;
                if (seconds == 60) {
                    minutes++;
                    seconds = 0;
                }
                Platform.runLater(() -> {
                    updateTimer();
                    Habitat.getInstance().update((System.currentTimeMillis() - startTime) / 1000);
                });
            }
        }, 0, 1000); // Обновление каждую секунду
    }

    public void stopAction() {
        Habitat hab = Habitat.getInstance();
        startFlag = timeFlag = false;
        statisticFlag = true;
        showStatisticLabel(); // Отображаем статистику и картинку
        timer.cancel();

        // Остановка музыки
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        // Очистка объектов
        hab.getArray().forEach((tmp) -> mainController.getPane().getChildren().remove(tmp.getImageView()));
        hab.getArray().clear();
        Developer.count = 0; // Сброс счетчика разработчиков
        Manager.count = 0; // Сброс счетчика менеджеров

        // Установка картинки (без изменения видимости, так как это уже делается в showStatisticLabel)
        Platform.runLater(() -> {
            ImageView resultImageView = mainController.getResultImageView();
            URL imageUrl = getClass().getResource("/result_image.png");
            if (imageUrl != null) {
                Image image = new Image(imageUrl.toExternalForm());
                resultImageView.setImage(image);

                // Центрирование картинки
                double centerX = (mainController.getPane().getWidth() - resultImageView.getFitWidth()) / 2;
                double centerY = (mainController.getPane().getHeight() - resultImageView.getFitHeight()) / 2;
                resultImageView.setLayoutX(centerX);
                resultImageView.setLayoutY(centerY + 100); // Смещение под статистикой
            }
        });
    }
}