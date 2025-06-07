package ru.nstu.laba1timp.controllers;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.scene.image.ImageView; // Может понадобиться для removeIf

import java.net.URL;
import java.util.*;

public class Statistics {
    public Timer timer;
    // true = таймер ВИДЕН, false = таймер СКРЫТ
    public boolean timeFlag = true; // Начальное значение = true (по умолчанию показываем)
    public boolean startFlag = false;
    public boolean restartFlag = false;
    public boolean firstActionFlag = true;

    private int seconds = -1;
    private int minutes = 0;

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
        URL musicUrl = getClass().getResource("/music.mp3");
        if (musicUrl != null) {
            try {
                Media media = new Media(musicUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(0.5);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                pauseTime = Duration.ZERO;
            } catch (Exception e) {
                System.err.println("Ошибка инициализации медиаплеера: " + e.getMessage());
                mediaPlayer = null;
            }
        } else {
            System.err.println("music.mp3 не найден.");
            mediaPlayer = null;
        }
    }

    public Controller getMainController() {
        return mainController;
    }

    public int getTime() {
        return minutes * 60 + Math.max(0, seconds);
    }

    public void setTimeFromLoad(int totalSeconds) {
        if (totalSeconds < 0) {
            this.minutes = 0;
            this.seconds = -1;
        } else {
            this.minutes = totalSeconds / 60;
            this.seconds = totalSeconds % 60; // Просто устанавливаем
        }
        Platform.runLater(this::updateTimer);
    }

    // Применяет видимость на основе ТЕКУЩЕГО timeFlag
    public void showTimer() {
        // timeFlag = !timeFlag; // <<< УБРАНА ИНВЕРСИЯ >>>
        if (mainController != null) {
            mainController.getLabelTextTIMER().setVisible(timeFlag);
            mainController.getLabelTimer().setVisible(timeFlag);
        }
    }

    public void updateTimer() {
        if (mainController == null) return;
        String min = String.format("%02d", minutes);
        String sec = String.format("%02d", Math.max(0, seconds));
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

    // Логика старта/рестарта/продолжения
    public void startAction() {
        Habitat hab = Habitat.getInstance();

        // 1. Очистка и сброс ТОЛЬКО при рестарте
        if (restartFlag) {
            hab.clearObjects();
            seconds = -1;
            minutes = 0;
            pauseTime = Duration.ZERO;
            musicPaused = false;
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.ZERO);
            }
            System.out.println("Симуляция перезапущена (restartFlag=true)");
        } else {
            System.out.println("Симуляция стартует/продолжается (restartFlag=false)");
        }

        // 2. Гарантированная инициализация таймера
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer("SimulationTimer", true);

        // 3. Сброс флагов и установка флага запуска
        restartFlag = false;
        firstActionFlag = false;
        startFlag = true;

        // 4. Возобновление AI и музыки
        resumeAIThreads();
        playMusic();

        // 5. Запуск задачи таймера
        try {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!startFlag) {
                        this.cancel();
                        return;
                    }
                    seconds++;
                    if (seconds == 60) {
                        minutes++;
                        seconds = 0;
                    }
                    Platform.runLater(() -> {
                        if (!startFlag || mainController == null) return;
                        updateTimer();
                        Habitat.getInstance().update();
                    });
                }
            }, 1000, 1000); // Начать через 1 сек, повторять каждую сек
        } catch (Exception e) {
            System.err.println("Ошибка планирования задачи таймера: " + e.getMessage());
        }
    }

    private void resumeAIThreads() {
        if (mainController != null && mainController.btnDevIntellect.getText().equals("ON")) {
            DeveloperAI ai = DeveloperAI.getInstance();
            ai.isActive = true;
            synchronized (ai.monitor) {
                ai.monitor.notify();
            }
        }
        if (mainController != null && mainController.btnManIntellect.getText().equals("ON")) {
            ManagerAI ai = ManagerAI.getInstance();
            ai.isActive = true;
            synchronized (ai.monitor) {
                ai.monitor.notify();
            }
        }
    }

    // Логика остановки
    public void stopAction(boolean showStatsDialog) {
        if (!startFlag) return;
        startFlag = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        DeveloperAI.getInstance().isActive = false;
        ManagerAI.getInstance().isActive = false;
        pauseMusic();

        if (showStatsDialog && mainController != null && mainController.btnShowInfo.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Статистика");
            alert.setHeaderText("OK - прекратить\nCancel - продолжить");
            int totalTime = getTime();
            int cM = totalTime / 60;
            int cS = totalTime % 60;
            String stat = "Создано:\nРазработчики: " + Developer.spawnedCount + "\nМенеджеры: " + Manager.spawnedCount + "\nВремя: ";
            if (cM >= 1) {
                stat += cM + " мин ";
            }
            stat += cS + " сек";
            TextArea ta = new TextArea(stat);
            ta.setPrefColumnCount(20);
            ta.setPrefRowCount(5);
            ta.setEditable(false);
            ta.setWrapText(true);
            alert.getDialogPane().setContent(ta);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                confirmStop(); // Устанавливает restartFlag
            } else {
                resumeSimulation(); // НЕ устанавливает restartFlag
            }
        } else if (showStatsDialog) {
            confirmStop(); // Остановка по кнопке "Стоп" (без диалога инфо) = рестарт
        }

        // Обновляем UI если остановка была программной (без диалога)
        if (!showStatsDialog && mainController != null) {
            Platform.runLater(() -> {
                mainController.btnStart.setDisable(false);
                mainController.btnStop.setDisable(true);
                mainController.menuStart.setDisable(false);
                mainController.menuStop.setDisable(true);
                mainController.btnDevIntellect.setDisable(true);
                mainController.btnManIntellect.setDisable(true);
            });
        }
    }

    // Подтвержденная остановка (устанавливает restartFlag)
    private void confirmStop() {
        this.restartFlag = true; // Устанавливаем флаг рестарта
        if (mainController != null) {
            Platform.runLater(() -> {
                mainController.setFieldsDisabled(false); // Разблокируем поля
                mainController.btnStart.setDisable(false);
                mainController.btnStop.setDisable(true);
                mainController.menuStart.setDisable(false);
                mainController.menuStop.setDisable(true);
                mainController.btnDevIntellect.setDisable(true);
                mainController.btnManIntellect.setDisable(true);
            });
        }
        stopMusic();
    }

    // Возобновление после отмены диалога
    private void resumeSimulation() {
        // restartFlag остается false
        if (mainController != null) {
            Platform.runLater(() -> {
                mainController.btnStart.setDisable(true);
                mainController.btnStop.setDisable(false);
                mainController.menuStart.setDisable(true);
                mainController.menuStop.setDisable(false);
                mainController.btnDevIntellect.setDisable(false);
                mainController.btnManIntellect.setDisable(false);
            });
        }
        startAction(); // Перезапускаем логику симуляции и таймер
    }

    @Deprecated
    public void stopAction() {
        stopAction(true);
    } // Для обратной совместимости
}