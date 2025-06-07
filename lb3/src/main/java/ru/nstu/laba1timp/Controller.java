// ru.nstu.laba1timp.Controller.java
package ru.nstu.laba1timp;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
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

public class Controller {
    @FXML
    private Label labelTextTIMER;
    @FXML
    private Label labelTimer;
    @FXML
    private Pane visualizationPane;
    @FXML
    private Pane leftPane; // Added to potentially manage focus

    @FXML
    public Button btnStart, btnStop, btnCurrentObjects;
    @FXML
    public CheckBox btnShowInfo;
    @FXML
    public RadioButton btnShowTime, btnHideTime, btnEnableSound, btnDisableSound;
    @FXML
    public TextField fieldN1, fieldN2, fieldLifeTimeDev, fieldLifeTimeMan, fieldMaxManagerPercent;
    @FXML
    public ComboBox<String> boxP1, boxP2;
    @FXML
    public MenuItem menuStart, menuStop, menuExit;
    @FXML
    public CheckMenuItem menuShowInfo;
    @FXML
    public RadioMenuItem menuShowTime, menuHideTime, menuEnableSound, menuDisableSound;

    @FXML
    void initialize() {
        btnStop.setDisable(true); // Кнопка "Стоп" изначально заблокирована
        menuStop.setDisable(true);
        btnShowTime.setSelected(true); // Переключатель "Показать время" изначально выбран
        menuShowTime.setSelected(true);
        btnEnableSound.setSelected(true); // Кнопка "Включить звук" изначально выбрана
        menuEnableSound.setSelected(true); // Пункт меню "Включить звук" изначально выбран
        fieldN1.setText("1"); // Значения для текстовых полей по умолчанию
        fieldN2.setText("2");
        fieldLifeTimeDev.setText("8");
        fieldLifeTimeMan.setText("30");
        fieldMaxManagerPercent.setText("40");
        // Дальше идёт заполнение комбобоксов
        for (int value = 0; value <= 100; value += 10) {
            boxP1.getItems().add(value + "%");
            boxP2.getItems().add(value + "%");
        }
        boxP1.setValue("80%"); // Значения для комбобоксов по умолчанию
        boxP2.setValue("40%");
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
                throw new NumberFormatException("Слишком маленькое число");
            }
            // Если данные некорректны, работает блок catch, иначе идём дальше
            hab.n1 = n1; // Установление периодов рождения
            hab.n2 = n2;
            hab.maxManagerPercent = maxManagerPercent;
            Developer.setLifeTime(lifeTimeDev); // Установление времени жизни
            Manager.setLifeTime(lifeTimeMan);
            hab.p1 = Float.parseFloat(boxP1.getValue().replace("%", "")) / 100; // Установление вероятностей рождения
            hab.p2 = Float.parseFloat(boxP2.getValue().replace("%", "")) / 100;
            fieldN1.setDisable(true);
            fieldN2.setDisable(true);
            fieldLifeTimeDev.setDisable(true);
            fieldLifeTimeMan.setDisable(true);
            fieldMaxManagerPercent.setDisable(true);
            boxP1.setDisable(true);
            boxP2.setDisable(true);
            btnStart.setDisable(true);
            btnStop.setDisable(false);
            menuStart.setDisable(true);
            menuStop.setDisable(false);
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
            alert.setContentText("Требуется целое положительное число, не превышающее 2^31-1 для периодов и времени жизни, и число от 0 до 100 для максимального процента менеджеров.");
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
        if (!btnShowInfo.isSelected()) {
            st.restartFlag = true;
            fieldN1.setDisable(false);
            fieldN2.setDisable(false);
            fieldLifeTimeDev.setDisable(false);
            fieldLifeTimeMan.setDisable(false);
            fieldMaxManagerPercent.setDisable(false);
            boxP1.setDisable(false);
            boxP2.setDisable(false);
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
            if (st.startFlag && (st.mediaPlayer == null || st.mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING)) {
                st.playMusic();
            }
        } else if (btnDisableSound.isSelected()) {
            st.setMusicEnabled(false);
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
            if (st.startFlag && (st.mediaPlayer == null || st.mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING)) {
                st.playMusic();
            }
        } else if (menuDisableSound.isSelected()) {
            st.setMusicEnabled(false);
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
        boolean musicWasPlayingInitially = false; // Переименовали для ясности

        // Проверяем, играла ли музыка ИЗНАЧАЛЬНО
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

        for (Map.Entry<Integer, Integer> entry : hab.getBornCollection().entrySet()) {
            int id = entry.getKey();
            int bornTime = entry.getValue();
            statistic += "ID = " + id + "\tВремя рождения = ";
            if (bornTime < 60) statistic += bornTime + " сек\n";
            else statistic += (bornTime / 60) + " мин " + (bornTime % 60) + " сек\n";
        }

        TextArea textArea = new TextArea(statistic);
        textArea.setPrefColumnCount(25);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();

        // Возобновляем музыку, только если она играла изначально и звук включен
        if (wasRunning && musicWasPlayingInitially && st.isMusicEnabled()) {
            st.mediaPlayer.seek(currentTime);
            st.playMusic();
        }

        if (st.startFlag) st.startAction();
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