// ru.nstu.laba1timp.Statistics.java
package ru.nstu.laba1timp;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class Statistics {
    public Timer timer;
    public boolean timeFlag = true, startFlag, restartFlag = false, firstActionFlag = true;
    private int seconds = -1, minutes = 0;
    private Controller mainController;
    private static Statistics instance;
    public MediaPlayer mediaPlayer;
    public Duration pauseTime;
    public boolean musicPaused = false;
    private boolean musicEnabled = true;

    private Statistics() {}
    public static Statistics getInstance() {
        if (instance == null) {
            instance = new Statistics();
        }
        return instance;
    }

    public void setMainController(Controller mainController) {
        this.mainController = mainController;
        // Load music
        URL musicUrl = getClass().getResource("/music.mp3");
        if (musicUrl != null) {
            Media media = new Media(musicUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.5);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            pauseTime = Duration.ZERO;
        }
    }

    public Controller getMainController() {
        return mainController;
    }

    public int getTime() {
        return minutes * 60 + seconds;
    }

    public void showTimer() {
        timeFlag = !timeFlag;
        if (timeFlag) {
            mainController.getLabelTextTIMER().setVisible(true);
            mainController.getLabelTimer().setVisible(true);
        } else {
            mainController.getLabelTextTIMER().setVisible(false);
            mainController.getLabelTimer().setVisible(false);
        }
    }

    public void updateTimer() {
        String min = minutes + "";
        String sec = seconds + "";
        if (min.length() < 2)
            min = "0"+ min;
        if (sec.length() < 2)
            sec = ("0" + sec);
        String time = min + ":" + sec;
        mainController.getLabelTimer().setText(time);
    }

    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void playMusic() {
        if (musicEnabled && mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            if (musicPaused) {
                mediaPlayer.seek(pauseTime);
                mediaPlayer.play();
                musicPaused = false;
            } else {
                mediaPlayer.play();
            }
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            pauseTime = mediaPlayer.getCurrentTime();
            mediaPlayer.pause();
            musicPaused = true;
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            musicPaused = false;
            pauseTime = Duration.ZERO;
        }
    }

    public void startAction() {
        Habitat hab = Habitat.getInstance();

        if (restartFlag || firstActionFlag) {
            hab.clearObjects();
            mainController.getPane().getChildren().removeIf(node -> node instanceof javafx.scene.image.ImageView); // Очистка изображений
            seconds = -1;
            minutes = 0;
            timer = new Timer();
            restartFlag = false;
            firstActionFlag = false;
            musicPaused = false;
            pauseTime = Duration.ZERO;
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.ZERO);
            }
        }

        startFlag = true;

        playMusic(); // Используем новый метод для воспроизведения музыки

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
                    if (seconds >= 1 || minutes >= 1) {
                        hab.update();
                    }
                });
            }
        }, 4, 1000); // Изменено на 100 мс для более плавной симуляции
    }

    public void stopAction() {
        startFlag = false;
        timer.cancel();
        timer = new Timer(); // Re-initialize timer

        pauseMusic(); // Используем новый метод для паузы музыки

        if (mainController.btnShowInfo.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Статистика");
            alert.setHeaderText("OK - прекратить симуляцию\nCancel - продолжить симуляцию");

            String statistic = "Создано:\nРазработчики: " + Developer.spawnedCount +
                    "\nМенеджеры: " + Manager.spawnedCount +
                    "\nВремя: ";
            if (minutes >= 1) {
                statistic += minutes + " мин ";
            }
            statistic += seconds + " сек";

            TextArea textArea = new TextArea(statistic);
            textArea.setPrefColumnCount(20);
            textArea.setPrefRowCount(5);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            alert.getDialogPane().setContent(textArea);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                restartFlag = true;
                mainController.fieldN1.setDisable(false);
                mainController.fieldN2.setDisable(false);
                mainController.fieldLifeTimeDev.setDisable(false);
                mainController.fieldLifeTimeMan.setDisable(false);
                mainController.fieldMaxManagerPercent.setDisable(false);
                mainController.boxP1.setDisable(false);
                mainController.boxP2.setDisable(false);
                stopMusic(); // Используем новый метод для остановки музыки
            } else {
                mainController.btnStart.setDisable(true);
                mainController.btnStop.setDisable(false);
                mainController.menuStart.setDisable(true);
                mainController.menuStop.setDisable(false);
                startAction();
            }
        } else {
            stopMusic(); // Используем новый метод для остановки музыки
        }
    }
}