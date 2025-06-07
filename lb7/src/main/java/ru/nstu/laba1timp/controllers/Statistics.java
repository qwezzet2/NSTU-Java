package ru.nstu.laba1timp.controllers;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import ru.nstu.laba1timp.model.Developer; // Используется для получения статистики по количеству созданных объектов
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;

// Явный импорт Habitat
import ru.nstu.laba1timp.controllers.Habitat;

import java.net.URL;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Класс Statistics отвечает за:
 * - Управление временем симуляции
 * - Воспроизведение фоновой музыки
 * - Управление состоянием симуляции (запуск/остановка)
 * - Отображение информации о времени и состоянии
 */
public class Statistics {
    public Timer timer;                // Таймер симуляции
    public boolean timeFlag = true;     // Флаг: показывать ли время на UI
    public boolean startFlag = false;   // Статус запуска симуляции
    public boolean restartFlag = false; // Флаг, указывающий, что следующий запуск должен быть "чистым" стартом

    private int seconds = -1;           // Секунды текущего времени
    private int minutes = 0;            // Минуты текущего времени
    private Controller mainController;   // Ссылка на главный контроллер для обновления UI
    private static Statistics instance;  // Единственный экземпляр класса (Singleton)

    public MediaPlayer mediaPlayer;      // Плеер для воспроизведения музыки
    public Duration pauseTime = Duration.ZERO; // Время паузы медиаплеера
    public boolean musicPaused = false;  // Флаг: включена ли пауза у музыки
    private boolean musicEnabled = true; // Флаг: включена ли музыка

    /**
     * Приватный конструктор для реализации шаблона Singleton.
     */
    private Statistics() {}

    /**
     * Возвращает единственный экземпляр класса Statistics.
     */
    public static synchronized Statistics getInstance() {
        if (instance == null) {
            instance = new Statistics();
        }
        return instance;
    }

    /**
     * Устанавливает связь с главным контроллером и инициализирует медиаплеер.
     */
    public void setMainController(Controller mainController) {
        this.mainController = mainController;
        initializeMediaPlayer();
    }

    /**
     * Инициализирует медиаплеер и загружает аудиофайл.
     */
    private void initializeMediaPlayer() {
        try {
            URL musicUrl = getClass().getResource("/music.mp3");
            if (musicUrl != null) {
                Media media = new Media(musicUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(0.5);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Бесконечное воспроизведение
            } else {
                System.err.println("Файл музыки music.mp3 не найден в ресурсах.");
                mediaPlayer = null;
            }
        } catch (Exception e) {
            System.err.println("Ошибка инициализации медиаплеера: " + e.getMessage());
            mediaPlayer = null;
        }
    }

    /**
     * Возвращает ссылку на главный контроллер.
     */
    public Controller getMainController() {
        return mainController;
    }

    /**
     * Возвращает общее количество секунд, прошедших с начала или после загрузки.
     */
    public int getTime() {
        return minutes * 60 + Math.max(0, seconds);
    }

    /**
     * Устанавливает начальное время симуляции при загрузке сохранённого состояния.
     */
    public void setTimeFromLoad(int totalSeconds) {
        if (mainController == null) return;
        if (totalSeconds < 0) {
            this.minutes = 0;
            this.seconds = -1;
        } else {
            this.minutes = totalSeconds / 60;
            this.seconds = totalSeconds % 60;
        }
        Platform.runLater(this::updateTimer);
    }

    /**
     * Обновляет видимость таймера на экране в зависимости от флага timeFlag.
     */
    public void showTimer() {
        if (mainController != null && mainController.getLabelTextTIMER() != null && mainController.getLabelTimer() != null) {
            mainController.getLabelTextTIMER().setVisible(timeFlag);
            mainController.getLabelTimer().setVisible(timeFlag);
        }
    }

    /**
     * Обновляет отображаемое значение таймера на экране.
     */
    public void updateTimer() {
        if (mainController == null || mainController.getLabelTimer() == null) return;
        String min = String.format("%02d", minutes);
        String sec = String.format("%02d", Math.max(0, seconds));
        String time = min + ":" + sec;
        mainController.getLabelTimer().setText(time);
    }

    /**
     * Включает или отключает воспроизведение музыки.
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled && mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            pauseMusic();
        } else if (enabled && startFlag && musicPaused && mediaPlayer != null) {
            playMusic();
        }
    }

    /**
     * Возвращает текущий статус музыки.
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    /**
     * Воспроизводит музыку, если она разрешена.
     */
    public void playMusic() {
        if (musicEnabled && mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            if (musicPaused) {
                mediaPlayer.seek(pauseTime);
                musicPaused = false;
            }
            mediaPlayer.play();
        }
    }

    /**
     * Приостанавливает воспроизведение музыки.
     */
    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            pauseTime = mediaPlayer.getCurrentTime();
            mediaPlayer.pause();
            musicPaused = true;
        }
    }

    /**
     * Останавливает воспроизведение музыки и сбрасывает позицию.
     */
    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            pauseTime = Duration.ZERO;
            musicPaused = false;
        }
    }

    /**
     * Запускает или возобновляет симуляцию.
     */
    public void startAction() {
        if (mainController == null) {
            System.err.println("Statistics.startAction(): MainController не установлен!");
            return;
        }

        Habitat hab = Habitat.getInstance(); // Получаем среду обитания объектов

        if (restartFlag) {
            boolean objectsWereAlreadyCleared = (hab != null && hab.getObjCollection().isEmpty() && hab.getBornCollection().isEmpty());
            if (objectsWereAlreadyCleared) {
                System.out.println("Симуляция перезапущена (restartFlag=true), объекты были очищены ранее.");
            } else if (hab != null) {
                System.out.println("Симуляция перезапущена (restartFlag=true). Объекты НЕ были пусты, как ожидалось. Проверьте логику.");
            }
            setTimeFromLoad(-1);
            if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
                stopMusic();
            }
            System.out.println("Симуляция перезапущена (restartFlag=true). Таймер и музыка сброшены/проверены.");
        } else {
            System.out.println("Симуляция стартует/продолжается (restartFlag=false).");
        }

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer("SimulationTimer", true);
        restartFlag = false; // Сброс флага
        startFlag = true;    // Симуляция активна

        resumeAIThreads();    // Возобновляем AI потоки
        playMusic();           // Воспроизводим музыку

        Platform.runLater(() -> {
            mainController.setFieldsDisabled(true);
            mainController.btnStart.setDisable(true);
            mainController.btnStop.setDisable(false);
            mainController.menuStart.setDisable(true);
            mainController.menuStop.setDisable(false);
            mainController.btnDevIntellect.setDisable(false);
            mainController.btnManIntellect.setDisable(false);
            mainController.btnDevIntellect.setText(DeveloperAI.getInstance().isActive ? "ON" : "OFF");
            mainController.btnManIntellect.setText(ManagerAI.getInstance().isActive ? "ON" : "OFF");
        });

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
                        Habitat.getInstance().update(); // Обновляем состояние среды
                    });
                }
            }, 1000, 1000);
        } catch (IllegalStateException e) {
            System.err.println("Ошибка планирования задачи таймера (возможно, таймер уже отменен): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при планировании задачи таймера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Останавливает симуляцию.
     *
     * @param showStatsDialog Показать диалог со статистикой?
     * @param updateUI        Обновить элементы управления?
     */
    public void stopAction(boolean showStatsDialog, boolean updateUI) {
        if (!startFlag && !updateUI) return;

        if (!startFlag && updateUI && mainController != null) {
            Platform.runLater(this::confirmStopUIUpdate);
            return;
        }

        startFlag = false; // Главный флаг остановки

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
            alert.setHeaderText("OK - прекратить симуляцию\nCancel - продолжить");

            int totalTime = getTime();
            int cM = totalTime / 60;
            int cS = totalTime % 60;

            String stat = "Создано:\nРазработчики: " + Developer.spawnedCount +
                    "\nМенеджеры: " + Manager.spawnedCount +
                    "\nВремя: ";
            if (cM >= 1) stat += cM + " мин ";
            stat += cS + " сек";

            TextArea ta = new TextArea(stat);
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefColumnCount(25);
            ta.setPrefRowCount(5);

            alert.getDialogPane().setContent(ta);
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                confirmStop(updateUI);
            } else {
                resumeSimulation();
            }
        } else if (updateUI) {
            confirmStop(true);
        }
    }

    /**
     * Перегруженный метод для обратной совместимости.
     */
    public void stopAction(boolean showStatsDialog) {
        stopAction(showStatsDialog, true); // По умолчанию обновляем UI
    }

    /**
     * Выполняет действия при подтверждении полной остановки симуляции.
     */
    private void confirmStop(boolean updateUIControls) {
        this.restartFlag = true;
        if (updateUIControls) {
            Platform.runLater(this::confirmStopUIUpdate);
        }
        stopMusic();
        Habitat hab = Habitat.getInstance();
        if (hab != null) {
            hab.clearObjects();
            System.out.println("confirmStop: Объекты немедленно очищены.");
        } else {
            System.err.println("confirmStop: Ошибка - Habitat is null, не удалось очистить объекты.");
        }
        setTimeFromLoad(-1); // Сброс таймера
    }

    /**
     * Обновляет элементы интерфейса при завершении симуляции.
     */
    private void confirmStopUIUpdate() {
        if (mainController == null) return;
        mainController.setFieldsDisabled(false);
        mainController.btnStart.setDisable(false);
        mainController.btnStop.setDisable(true);
        mainController.menuStart.setDisable(false);
        mainController.menuStop.setDisable(true);
        mainController.btnDevIntellect.setDisable(true);
        mainController.btnManIntellect.setDisable(true);
    }

    /**
     * Возобновляет симуляцию после нажатия Cancel в диалоге статистики.
     */
    private void resumeSimulation() {
        startAction(); // Полностью возобновляем симуляцию
    }

    /**
     * Возобновляет работу AI-потоков в соответствии с текущими настройками UI.
     */
    private void resumeAIThreads() {
        if (mainController != null) {
            DeveloperAI devAI = DeveloperAI.getInstance();
            if (mainController.btnDevIntellect.getText().equals("ON")) {
                devAI.isActive = true;
                synchronized (devAI.monitor) { devAI.monitor.notify(); }
            } else {
                devAI.isActive = false;
            }

            ManagerAI manAI = ManagerAI.getInstance();
            if (mainController.btnManIntellect.getText().equals("ON")) {
                manAI.isActive = true;
                synchronized (manAI.monitor) { manAI.monitor.notify(); }
            } else {
                manAI.isActive = false;
            }
        }
    }
}
