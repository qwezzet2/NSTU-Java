package ru.nstu.laba1timp;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.locks.ReentrantLock;

public class Controller {
    @FXML
    private Label labelTextTIMER;
    @FXML
    private Label labelTimer;
    @FXML
    private Pane visualizationPane;
    @FXML
    private Pane leftPane;

    @FXML
    public Button btnStart, btnStop, btnCurrentObjects, btnDevIntellect, btnManIntellect;
    @FXML
    public CheckBox btnShowInfo;
    @FXML
    public RadioButton btnShowTime, btnHideTime, btnEnableSound, btnDisableSound;
    @FXML
    public TextField fieldN1, fieldN2, fieldLifeTimeDev, fieldLifeTimeMan, fieldMaxManagerPercent;
    @FXML
    public ComboBox<String> boxP1, boxP2;
    @FXML
    public ComboBox<Integer> boxDevPriority, boxManPriority;
    @FXML
    public MenuItem menuStart, menuStop, menuExit;
    @FXML
    public CheckMenuItem menuShowInfo;
    @FXML
    public RadioMenuItem menuShowTime, menuHideTime, menuEnableSound, menuDisableSound;

    @FXML
    void initialize() {
        btnStop.setDisable(true);
        menuStop.setDisable(true);
        btnShowTime.setSelected(true);
        menuShowTime.setSelected(true);
        btnEnableSound.setSelected(true);
        menuEnableSound.setSelected(true);
        fieldN1.setText("1");
        fieldN2.setText("2");
        fieldLifeTimeDev.setText("8");
        fieldLifeTimeMan.setText("10");
        fieldMaxManagerPercent.setText("40");

        for (int value = 0; value <= 100; value += 10) {
            boxP1.getItems().add(value + "%");
            boxP2.getItems().add(value + "%");
        }
        boxP1.setValue("80%");
        boxP2.setValue("100%");

        for (int value = Thread.MIN_PRIORITY; value <= Thread.MAX_PRIORITY; value++) {
            boxDevPriority.getItems().add(value);
            boxManPriority.getItems().add(value);
        }
        boxDevPriority.setValue(Thread.NORM_PRIORITY);
        boxManPriority.setValue(Thread.NORM_PRIORITY);

        btnDevIntellect.setDisable(true);
        btnManIntellect.setDisable(true);

    }

    public Label getLabelTimer() {
        return labelTimer;
    }

    public Label getLabelTextTIMER() {
        return labelTextTIMER;
    }

    public Pane getPane() {
        return visualizationPane;
    }

    @FXML
    private void clickStart() {
        Habitat hab = Habitat.getInstance();
        int n1 = 1, n2 = 1, lifeTimeDev = 1, lifeTimeMan = 1, maxManagerPercent = 0;
        try {
            n1 = Integer.parseInt(fieldN1.getText());
            n2 = Integer.parseInt(fieldN2.getText());
            lifeTimeDev = Integer.parseInt(fieldLifeTimeDev.getText());
            lifeTimeMan = Integer.parseInt(fieldLifeTimeMan.getText());
            maxManagerPercent = Integer.parseInt(fieldMaxManagerPercent.getText());
            if (n1 < 1 || n2 < 1 || lifeTimeDev < 1 || lifeTimeMan < 1 || maxManagerPercent < 0 || maxManagerPercent > 100) {
                throw new NumberFormatException("Invalid number");
            }

            hab.n1 = n1;
            hab.n2 = n2;
            hab.maxManagerPercent = maxManagerPercent;
            Developer.setLifeTime(lifeTimeDev);
            Manager.setLifeTime(lifeTimeMan);
            hab.p1 = Float.parseFloat(boxP1.getValue().replace("%", "")) / 100;
            hab.p2 = Float.parseFloat(boxP2.getValue().replace("%", "")) / 100;

            DeveloperAI.getInstance().setPriority(boxDevPriority.getValue());
            ManagerAI.getInstance().setPriority(boxManPriority.getValue());


            fieldN1.setDisable(true);
            fieldN2.setDisable(true);
            fieldLifeTimeDev.setDisable(true);
            fieldLifeTimeMan.setDisable(true);
            fieldMaxManagerPercent.setDisable(true);
            boxP1.setDisable(true);
            boxP2.setDisable(true);
            boxDevPriority.setDisable(true);
            boxManPriority.setDisable(true);

            btnStart.setDisable(true);
            btnStop.setDisable(false);
            menuStart.setDisable(true);
            menuStop.setDisable(false);

            btnDevIntellect.setDisable(false);
            btnManIntellect.setDisable(false);


            Statistics.getInstance().startAction();
        } catch (NumberFormatException ex) {
            if (!fieldN1.getText().matches("\\d+") || n1 < 1) fieldN1.setText("1");
            if (!fieldN2.getText().matches("\\d+") || n2 < 1) fieldN2.setText("2");
            if (!fieldLifeTimeDev.getText().matches("\\d+") || lifeTimeDev < 1) fieldLifeTimeDev.setText("8");
            if (!fieldLifeTimeMan.getText().matches("\\d+") || lifeTimeMan < 1) fieldLifeTimeMan.setText("30");
            if (!fieldMaxManagerPercent.getText().matches("\\d+") || maxManagerPercent < 0 || maxManagerPercent > 100) fieldMaxManagerPercent.setText("40");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Некорректное значение текстового поля");
            alert.setContentText("Требуется целое положительное число для периодов и времени жизни, и число от 0 до 100 для максимального процента менеджеров.");
            alert.showAndWait();
        }
    }

    @FXML
    private void clickStop() {
        Statistics st = Statistics.getInstance();
        btnStart.setDisable(false);
        btnStop.setDisable(true);
        menuStart.setDisable(false);
        menuStop.setDisable(true);

        btnDevIntellect.setDisable(true);
        btnManIntellect.setDisable(true);

        DeveloperAI.getInstance().isActive = false;
        ManagerAI.getInstance().isActive = false;

        if (!btnShowInfo.isSelected()) {
            st.restartFlag = true;
            fieldN1.setDisable(false);
            fieldN2.setDisable(false);
            fieldLifeTimeDev.setDisable(false);
            fieldLifeTimeMan.setDisable(false);
            fieldMaxManagerPercent.setDisable(false);
            boxP1.setDisable(false);
            boxP2.setDisable(false);
            boxDevPriority.setDisable(false);
            boxManPriority.setDisable(false);
        }
        st.stopAction();
    }

    @FXML
    private void clickInfo() {
        menuShowInfo.setSelected(!menuShowInfo.isSelected());
    }

    @FXML
    private void menuClickInfo() {
        btnShowInfo.setSelected(!btnShowInfo.isSelected());
    }

    @FXML
    private void clickTimeSwitch() {
        Statistics st = Statistics.getInstance();
        if (btnShowTime.isSelected()) {
            st.timeFlag = false;
            menuShowTime.setSelected(true);
            menuHideTime.setSelected(false);
        } else if (btnHideTime.isSelected()) {
            st.timeFlag = true;
            menuShowTime.setSelected(false);
            menuHideTime.setSelected(true);
        }
        st.showTimer();
    }

    @FXML
    private void menuClickTimeSwitch() {
        Statistics st = Statistics.getInstance();
        if (menuShowTime.isSelected()) {
            st.timeFlag = false;
            btnShowTime.setSelected(true);
            btnHideTime.setSelected(false);
        } else if (menuHideTime.isSelected()) {
            st.timeFlag = true;
            btnShowTime.setSelected(false);
            btnHideTime.setSelected(true);
        }
        st.showTimer();
    }

    @FXML
    private void clickSoundSwitch() {
        Statistics st = Statistics.getInstance();
        if (btnEnableSound.isSelected()) {
            st.setMusicEnabled(true);
            menuEnableSound.setSelected(true); // Синхронизация с меню
            menuDisableSound.setSelected(false); // Синхронизация с меню
            if (st.startFlag && (st.mediaPlayer == null || st.mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING)) {
                st.playMusic();
            }
        } else if (btnDisableSound.isSelected()) {
            st.setMusicEnabled(false);
            menuEnableSound.setSelected(false); // Синхронизация с меню
            menuDisableSound.setSelected(true); // Синхронизация с меню
            if (st.mediaPlayer != null && st.mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                st.pauseMusic();
            }
        }
    }

    @FXML
    private void menuClickSoundSwitch() {
        Statistics st = Statistics.getInstance();
        if (menuEnableSound.isSelected()) {
            st.setMusicEnabled(true);
            btnEnableSound.setSelected(true); // Синхронизация с кнопкой
            btnDisableSound.setSelected(false); // Синхронизация с кнопкой
            if (st.startFlag && (st.mediaPlayer == null || st.mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING)) {
                st.playMusic();
            }
        } else if (menuDisableSound.isSelected()) {
            st.setMusicEnabled(false);
            btnEnableSound.setSelected(false); // Синхронизация с кнопкой
            btnDisableSound.setSelected(true); // Синхронизация с кнопкой
            if (st.mediaPlayer != null && st.mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                st.pauseMusic();
            }
        }
    }

    @FXML
    public void menuClickExit() {
        System.exit(0);
    }

    @FXML
    public void clickCurrentObjects() {
        Statistics st = Statistics.getInstance();
        Habitat hab = Habitat.getInstance();
        boolean wasRunning = st.startFlag;
        Duration currentTime = Duration.ZERO;
        boolean musicWasPlayingInitially = false;
        if (st.isMusicEnabled() && st.mediaPlayer != null && st.mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            musicWasPlayingInitially = true;
            currentTime = st.mediaPlayer.getCurrentTime();
            st.pauseMusic();
        }

        if (!st.firstActionFlag) {
            st.timer.cancel();
            st.timer = new Timer();
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText("Живые объекты");
        String statistic = "";

        synchronized (hab.getBornCollection()) {
            for (Map.Entry<Integer, Integer> entry : hab.getBornCollection().entrySet()) {
                int id = entry.getKey();
                int bornTime = entry.getValue();
                statistic += "ID = " + id + "\tВремя рождения = ";
                if (bornTime < 60) statistic += bornTime + " сек\n";
                else statistic += (bornTime / 60) + " мин " + (bornTime % 60) + " сек\n";
            }
        }


        TextArea textArea = new TextArea(statistic);
        textArea.setPrefColumnCount(25);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();

        if (wasRunning && musicWasPlayingInitially && st.isMusicEnabled()) {
            st.mediaPlayer.seek(currentTime);
            st.playMusic();
        }

        if (st.startFlag) st.startAction();
    }

    @FXML
    public void clickDevIntellect() {
        DeveloperAI ai = DeveloperAI.getInstance();
        if (btnDevIntellect.getText().equals("ON")) {
            ai.isActive = false;
            btnDevIntellect.setText("OFF");
        } else {
            ai.isActive = true;
            synchronized (ai.monitor) {
                ai.monitor.notify();
            }
            btnDevIntellect.setText("ON");
        }
    }

    @FXML
    public void clickManIntellect() {
        ManagerAI ai = ManagerAI.getInstance();
        if (btnManIntellect.getText().equals("ON")) {
            ai.isActive = false;
            btnManIntellect.setText("OFF");
        } else {
            ai.isActive = true;
            synchronized (ai.monitor) {
                ai.monitor.notify();
            }
            btnManIntellect.setText("ON");
        }
    }


    @FXML
    void keyPressed(KeyEvent keyEvent) {
        keyEvent.consume();
        Statistics st = Statistics.getInstance();
        switch (keyEvent.getCode()) {
            case KeyCode.T:
                st.showTimer();
                if (btnShowTime.isSelected()) {
                    btnShowTime.setSelected(false);
                    menuShowTime.setSelected(false);
                    btnHideTime.setSelected(true);
                    menuHideTime.setSelected(true);
                } else if (btnHideTime.isSelected()) {
                    btnShowTime.setSelected(true);
                    menuShowTime.setSelected(true);
                    btnHideTime.setSelected(false);
                    menuHideTime.setSelected(false);
                }
                break;
            case KeyCode.B:
                if (!st.startFlag) {
                    clickStart();
                }
                break;
            case KeyCode.E:
                if (st.startFlag) {
                    clickStop();
                }
                break;
        }
    }
}