// ru.nstu.laba1timp.Controller.java
package ru.nstu.laba1timp.controllers; // <<< ИСПРАВЛЕН ПАКЕТ

// Импорты
import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Window;
import javafx.util.Duration;
import ru.nstu.laba1timp.console.ConsoleWindow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.io.IOException;

public class Controller {

    // --- Элементы FXML ---
    @FXML public MenuItem menuLoad; @FXML public MenuItem menuSave;
    @FXML private Label labelTextTIMER; @FXML private Label labelTimer;
    @FXML private Pane visualizationPane; @FXML private Pane leftPane;
    @FXML public Button btnStart, btnStop, btnCurrentObjects, btnDevIntellect, btnManIntellect;
    @FXML public CheckBox btnShowInfo;
    @FXML public RadioButton btnShowTime, btnHideTime, btnEnableSound, btnDisableSound;
    @FXML public TextField fieldN1, fieldN2, fieldLifeTimeDev, fieldLifeTimeMan, fieldMaxManagerPercent;
    @FXML public ComboBox<String> boxP1, boxP2; @FXML public ComboBox<Integer> boxDevPriority, boxManPriority;
    @FXML public MenuItem menuStart, menuStop, menuExit; @FXML public CheckMenuItem menuShowInfo;
    @FXML public RadioMenuItem menuShowTime, menuHideTime, menuEnableSound, menuDisableSound;
    @FXML public MenuItem menuOpenConsole;

    // --- Методы ---

    @FXML
    void initialize() {
        // ... Начальная настройка UI ...
        btnStop.setDisable(true); menuStop.setDisable(true); btnShowTime.setSelected(true); menuShowTime.setSelected(true);
        btnEnableSound.setSelected(true); menuEnableSound.setSelected(true); fieldN1.setText("1"); fieldN2.setText("2");
        fieldLifeTimeDev.setText("8"); fieldLifeTimeMan.setText("10"); fieldMaxManagerPercent.setText("40");
        for(int v=0;v<=100;v+=10){boxP1.getItems().add(v+"%");boxP2.getItems().add(v+"%");} boxP1.setValue("80%");boxP2.setValue("100%");
        for(int v=Thread.MIN_PRIORITY;v<=Thread.MAX_PRIORITY;v++){boxDevPriority.getItems().add(v);boxManPriority.getItems().add(v);} boxDevPriority.setValue(Thread.NORM_PRIORITY);boxManPriority.setValue(Thread.NORM_PRIORITY);
        btnDevIntellect.setDisable(true);btnManIntellect.setDisable(true);

        // === Загрузка конфигурации ===
        try {
            Statistics stats = Statistics.getInstance(); if (stats != null) { stats.setMainController(this); }
            FileMaster.loadConfig(this);
            if (stats != null) { stats.timeFlag = btnShowTime.isSelected(); stats.showTimer(); }
        } catch (Exception e) {
            System.err.println("Не удалось загрузить начальную конфигурацию: " + e.getMessage()); Alert alert = new Alert(Alert.AlertType.WARNING); alert.setTitle("Предупреждение загрузки"); alert.setHeaderText("Не удалось загрузить настройки."); alert.setContentText("Используются значения по умолчанию. Ошибка: " + e.getMessage()); alert.showAndWait();
            if(Statistics.getInstance() != null) { Statistics.getInstance().timeFlag = true; Statistics.getInstance().showTimer(); }
        }
    }

    // Геттеры
    public Label getLabelTimer() { return labelTimer; } public Label getLabelTextTIMER() { return labelTextTIMER; } public Pane getPane() { return visualizationPane; }

    // Обработчик кнопки Старт
    @FXML private void clickStart() { /* ... прежний код ... */ Habitat hab = Habitat.getInstance(); int n1=1, n2=1, lifeTimeDev=1, lifeTimeMan=1, maxManagerPercent=0; try { n1 = Integer.parseInt(fieldN1.getText()); n2 = Integer.parseInt(fieldN2.getText()); lifeTimeDev = Integer.parseInt(fieldLifeTimeDev.getText()); lifeTimeMan = Integer.parseInt(fieldLifeTimeMan.getText()); maxManagerPercent = Integer.parseInt(fieldMaxManagerPercent.getText()); if (n1<1||n2<1||lifeTimeDev<1||lifeTimeMan<1||maxManagerPercent<0||maxManagerPercent>100) throw new NumberFormatException("Invalid number"); hab.n1=n1; hab.n2=n2; hab.maxManagerPercent=maxManagerPercent; Developer.setLifeTime(lifeTimeDev); Manager.setLifeTime(lifeTimeMan); hab.p1=Float.parseFloat(boxP1.getValue().replace("%",""))/100; hab.p2=Float.parseFloat(boxP2.getValue().replace("%",""))/100; DeveloperAI.getInstance().setPriority(boxDevPriority.getValue()); ManagerAI.getInstance().setPriority(boxManPriority.getValue()); setFieldsDisabled(true); btnStart.setDisable(true); btnStop.setDisable(false); menuStart.setDisable(true); menuStop.setDisable(false); btnDevIntellect.setDisable(false); btnManIntellect.setDisable(false); Statistics.getInstance().startAction(); } catch (NumberFormatException ex) { if (!fieldN1.getText().matches("\\d+")||n1<1) fieldN1.setText("1"); if (!fieldN2.getText().matches("\\d+")||n2<1) fieldN2.setText("2"); if (!fieldLifeTimeDev.getText().matches("\\d+")||lifeTimeDev<1) fieldLifeTimeDev.setText("8"); if (!fieldLifeTimeMan.getText().matches("\\d+")||lifeTimeMan<1) fieldLifeTimeMan.setText("10"); if (!fieldMaxManagerPercent.getText().matches("\\d+")||maxManagerPercent<0||maxManagerPercent>100) fieldMaxManagerPercent.setText("40"); Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle("Ошибка"); alert.setHeaderText("Некорректное значение"); alert.setContentText("Требуется целое положительное число для периодов/времени жизни, 0-100 для %."); alert.showAndWait(); } }

    // Блокировка/разблокировка полей
    public void setFieldsDisabled(boolean disabled) { /* ... прежний код ... */ fieldN1.setDisable(disabled); fieldN2.setDisable(disabled); fieldLifeTimeDev.setDisable(disabled); fieldLifeTimeMan.setDisable(disabled); fieldMaxManagerPercent.setDisable(disabled); boxP1.setDisable(disabled); boxP2.setDisable(disabled); boxDevPriority.setDisable(disabled); boxManPriority.setDisable(disabled); }

    // Обработчик кнопки Стоп
    @FXML private void clickStop() { Statistics.getInstance().stopAction(true); }

    // Обработчики переключателей таймера
    @FXML private void clickTimeSwitch() { Statistics st = Statistics.getInstance(); if(st==null) return; st.timeFlag = btnShowTime.isSelected(); menuShowTime.setSelected(st.timeFlag); menuHideTime.setSelected(!st.timeFlag); st.showTimer(); }
    @FXML private void menuClickTimeSwitch() { Statistics st = Statistics.getInstance(); if(st==null) return; st.timeFlag = menuShowTime.isSelected(); btnShowTime.setSelected(st.timeFlag); btnHideTime.setSelected(!st.timeFlag); st.showTimer(); }

    // Обработчики информации
    @FXML private void clickInfo() { menuShowInfo.setSelected(!menuShowInfo.isSelected()); }
    @FXML private void menuClickInfo() { btnShowInfo.setSelected(!btnShowInfo.isSelected()); }

    // Обработчики звука
    @FXML private void clickSoundSwitch() { /* ... прежний код ... */ Statistics st=Statistics.getInstance(); if(st==null) return; if (btnEnableSound.isSelected()){st.setMusicEnabled(true); menuEnableSound.setSelected(true); menuDisableSound.setSelected(false); if(st.startFlag&&(st.mediaPlayer==null||st.mediaPlayer.getStatus()!=MediaPlayer.Status.PLAYING)){st.playMusic();}} else if(btnDisableSound.isSelected()){st.setMusicEnabled(false); menuEnableSound.setSelected(false); menuDisableSound.setSelected(true); if(st.mediaPlayer!=null&&st.mediaPlayer.getStatus()==MediaPlayer.Status.PLAYING){st.pauseMusic();}} }
    @FXML private void menuClickSoundSwitch() { /* ... прежний код ... */ Statistics st=Statistics.getInstance(); if(st==null) return; if(menuEnableSound.isSelected()){st.setMusicEnabled(true); btnEnableSound.setSelected(true); btnDisableSound.setSelected(false); if(st.startFlag&&(st.mediaPlayer==null||st.mediaPlayer.getStatus()!=MediaPlayer.Status.PLAYING)){st.playMusic();}} else if(menuDisableSound.isSelected()){st.setMusicEnabled(false); btnEnableSound.setSelected(false); btnDisableSound.setSelected(true); if(st.mediaPlayer!=null&&st.mediaPlayer.getStatus()==MediaPlayer.Status.PLAYING){st.pauseMusic();}} }

    // Обработчик меню Выход
    @FXML public void menuClickExit() { FileMaster.saveConfig(this); System.exit(0); }

    // Обработчик меню Сохранить
    @FXML public void clickSave() { FileMaster.saveState(this, Habitat.getInstance(), Statistics.getInstance()); }

    // Обработчик меню Загрузить
    @FXML public void clickLoad() { FileMaster.loadState(this, Habitat.getInstance(), Statistics.getInstance()); }

    // Обработчик кнопки Текущие объекты (с полной паузой через stopAction/startAction)
    @FXML
    public void clickCurrentObjects() {
        Statistics st = Statistics.getInstance(); Habitat hab = Habitat.getInstance(); if (st == null || hab == null) return;

        // --- Сохранение состояний перед паузой ---
        final boolean simulationWasRunning = st.startFlag; // Используем final
        boolean devAiWasActiveBeforePause = false; // Переименуем для ясности
        boolean manAiWasActiveBeforePause = false;

        // --- Пауза (только если симуляция была запущена) ---
        if (simulationWasRunning) {
            System.out.println("Пауза симуляции для показа объектов...");
            devAiWasActiveBeforePause = DeveloperAI.getInstance().isActive;
            manAiWasActiveBeforePause = ManagerAI.getInstance().isActive;
            st.stopAction(false); // Полная остановка без диалога
        } else { System.out.println("Симуляция не запущена, показ объектов без паузы."); }

        // --- Показ диалога ---
        Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle("Информация"); alert.setHeaderText("Живые объекты"); String statistic = "";
        synchronized (hab.getObjCollection()) { synchronized (hab.getBornCollection()) { Map<Integer, Integer> bornCopy = new HashMap<>(hab.getBornCollection()); if (bornCopy.isEmpty()){ statistic = "Нет живых объектов."; } else { for (Map.Entry<Integer, Integer> entry : bornCopy.entrySet()) { int id = entry.getKey(); int bornTime = entry.getValue(); String type = hab.getObjCollection().stream().filter(p -> p != null && p.getId() == id).map(p -> p.getClass().getSimpleName()).findFirst().orElse("?"); statistic += "ID = " + id + " (" + type + ")\tВремя рождения = "; if (bornTime < 60) { statistic += bornTime + " сек\n"; } else { statistic += (bornTime / 60) + " мин " + (bornTime % 60) + " сек\n"; } } } } }
        TextArea textArea = new TextArea(statistic); textArea.setPrefColumnCount(30); textArea.setEditable(false); textArea.setWrapText(true); alert.getDialogPane().setContent(textArea); alert.getDialogPane().setMinWidth(400);
        alert.showAndWait();

        // --- Возобновление (только если симуляция была запущена) ---
        if (simulationWasRunning) {
            System.out.println("Возобновление симуляции после показа объектов...");
            // <<< Создаем final копии переменных для лямбды >>>
            final boolean finalDevAiWasActive = devAiWasActiveBeforePause;
            final boolean finalManAiWasActive = manAiWasActiveBeforePause;

            // Восстанавливаем флаги AI ПЕРЕД вызовом startAction
            DeveloperAI.getInstance().isActive = finalDevAiWasActive;
            ManagerAI.getInstance().isActive = finalManAiWasActive;
            // Устанавливаем флаг паузы музыки, чтобы startAction ее возобновил
            st.musicPaused = true;

            st.startAction(); // Перезапуск логики таймера и AI

            // Восстанавливаем состояние кнопок UI обратно на "запущено"
            Platform.runLater(() -> {
                // Используем final переменные внутри лямбды
                this.btnStart.setDisable(true);
                this.btnStop.setDisable(false);
                this.menuStart.setDisable(true);
                this.menuStop.setDisable(false);
                this.btnDevIntellect.setDisable(false);
                this.btnManIntellect.setDisable(false);
                this.btnDevIntellect.setText(finalDevAiWasActive ? "ON" : "OFF"); // <<< Используем final копию
                this.btnManIntellect.setText(finalManAiWasActive ? "ON" : "OFF"); // <<< Используем final копию
            });
        }
    }


    // Обработчики кнопок AI
    @FXML public void clickDevIntellect() { Statistics st = Statistics.getInstance(); if (st == null || !st.startFlag) return; DeveloperAI ai = DeveloperAI.getInstance(); if (btnDevIntellect.getText().equals("ON")) { ai.isActive = false; btnDevIntellect.setText("OFF"); } else { ai.isActive = true; synchronized (ai.monitor) { ai.monitor.notify(); } btnDevIntellect.setText("ON"); } }
    @FXML public void clickManIntellect() { Statistics st = Statistics.getInstance(); if (st == null || !st.startFlag) return; ManagerAI ai = ManagerAI.getInstance(); if (btnManIntellect.getText().equals("ON")) { ai.isActive = false; btnManIntellect.setText("OFF"); } else { ai.isActive = true; synchronized (ai.monitor) { ai.monitor.notify(); } btnManIntellect.setText("ON"); } }

    // Обработчик нажатия клавиш
    @FXML void keyPressed(KeyEvent keyEvent) { keyEvent.consume(); Statistics st = Statistics.getInstance(); if(st == null) return; switch (keyEvent.getCode()) { case T: st.timeFlag = !st.timeFlag; btnShowTime.setSelected(st.timeFlag); menuShowTime.setSelected(st.timeFlag); btnHideTime.setSelected(!st.timeFlag); menuHideTime.setSelected(!st.timeFlag); st.showTimer(); break; case B: if (!st.startFlag) { clickStart(); } break; case E: if (st.startFlag) { clickStop(); } break; } }

    // Обработчик для открытия консоли
    @FXML
    private void openConsoleWindow() {
        try {
            Window ownerWindow = leftPane != null && leftPane.getScene() != null ? leftPane.getScene().getWindow() : null;
            new ConsoleWindow(ownerWindow);
        } catch (IOException e) {
            System.err.println("Ошибка открытия окна консоли: " + e.getMessage()); e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR); errorAlert.setTitle("Ошибка консоли");
            errorAlert.setHeaderText("Не удалось загрузить интерфейс консоли.");
            errorAlert.setContentText("Подробности: " + e.getMessage()); errorAlert.showAndWait();
        }
    }
}