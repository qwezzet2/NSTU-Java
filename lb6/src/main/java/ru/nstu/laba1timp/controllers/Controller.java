package ru.nstu.laba1timp.controllers;

// Существующие импорты
import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
import ru.nstu.laba1timp.model.Person;
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

// Сетевые импорты
import ru.nstu.laba1timp.api.Client; // Используем Client из пакета api
import javafx.stage.Stage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;     // Для диалога
import javafx.geometry.Insets;      // Для диалога
import javafx.scene.Node;         // Для диалога

// Стандартные импорты
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Controller {

    // --- Элементы FXML из твоей Лабы 5 ---
    @FXML public MenuItem menuLoad;
    @FXML public MenuItem menuSave;
    @FXML private Label labelTextTIMER;
    @FXML private Label labelTimer;
    @FXML private Pane visualizationPane;
    // @FXML private Pane leftPane; // Закомментировано, т.к. в FXML используется VBox с ID leftPaneVBox
    @FXML private VBox leftPaneVBox; // Используем VBox, если это контейнер левой панели
    @FXML public Button btnStart;
    @FXML public Button btnStop;
    @FXML public Button btnCurrentObjects;
    @FXML public Button btnDevIntellect;
    @FXML public Button btnManIntellect;
    @FXML public CheckBox btnShowInfo;
    @FXML public RadioButton btnShowTime;
    @FXML public RadioButton btnHideTime;
    @FXML public RadioButton btnEnableSound;
    @FXML public RadioButton btnDisableSound;
    @FXML public TextField fieldN1;
    @FXML public TextField fieldN2;
    @FXML public TextField fieldLifeTimeDev;
    @FXML public TextField fieldLifeTimeMan;
    @FXML public TextField fieldMaxManagerPercent;
    @FXML public ComboBox<String> boxP1;
    @FXML public ComboBox<String> boxP2;
    @FXML public ComboBox<Integer> boxDevPriority;
    @FXML public ComboBox<Integer> boxManPriority;
    @FXML public MenuItem menuStart;
    @FXML public MenuItem menuStop;
    @FXML public MenuItem menuExit;
    @FXML public CheckMenuItem menuShowInfo;
    @FXML public RadioMenuItem menuShowTime;
    @FXML public RadioMenuItem menuHideTime;
    @FXML public RadioMenuItem menuEnableSound;
    @FXML public RadioMenuItem menuDisableSound;
    @FXML public MenuItem menuOpenConsole;
    @FXML private GridPane gridPane; // Корневой элемент

    // --- НОВЫЕ ЭЛЕМЕНТЫ FXML для Сети ---
    @FXML private TextArea userListBox; // Поле для списка пользователей
    @FXML private ComboBox<String> exchangeUserBox; // <<<=== ComboBox для обмена
    @FXML private Button btnExchangeObjects; // <<<=== Кнопка обмена
    @FXML private MenuItem menuConnect; // Меню для подключения/отключения
    @FXML private Label labelConnectionStatus; // Статус сети
    // ---------------------------------------------

    public static final String NO_USER_SELECTED = "<Выберите пользователя>";

    @FXML
    void initialize() {
        btnStop.setDisable(true); menuStop.setDisable(true);
        btnShowTime.setSelected(true); menuShowTime.setSelected(true);
        btnEnableSound.setSelected(true); menuEnableSound.setSelected(true);
        fieldN1.setText("1"); fieldN2.setText("2");
        fieldLifeTimeDev.setText("8"); fieldLifeTimeMan.setText("10");
        fieldMaxManagerPercent.setText("40");
        for (int v = 0; v <= 100; v += 10) { boxP1.getItems().add(v + "%"); boxP2.getItems().add(v + "%"); }
        boxP1.setValue("80%"); boxP2.setValue("100%");
        for (int v = Thread.MIN_PRIORITY; v <= Thread.MAX_PRIORITY; v++) { boxDevPriority.getItems().add(v); boxManPriority.getItems().add(v); }
        boxDevPriority.setValue(Thread.NORM_PRIORITY); boxManPriority.setValue(Thread.NORM_PRIORITY);
        btnDevIntellect.setDisable(true); btnManIntellect.setDisable(true);

        if (userListBox != null) userListBox.setEditable(false);
        if (labelConnectionStatus != null) { labelConnectionStatus.setText("Оффлайн"); labelConnectionStatus.setStyle("-fx-text-fill: red;"); }
        if (exchangeUserBox != null) {
            exchangeUserBox.getItems().add(NO_USER_SELECTED);
            exchangeUserBox.setValue(NO_USER_SELECTED);
            exchangeUserBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> updateExchangeButtonState());
        }
        if (btnExchangeObjects != null) btnExchangeObjects.setDisable(true);
        if (menuConnect != null) menuConnect.setText("Подключиться к серверу...");

        try {
            Statistics stats = Statistics.getInstance(); if (stats != null) { stats.setMainController(this); }
            FileMaster.loadConfig(this);
            if (stats != null) { stats.timeFlag = btnShowTime.isSelected(); stats.showTimer(); stats.setMusicEnabled(btnEnableSound.isSelected()); }
        } catch (Exception e) {
            System.err.println("Не удалось загрузить начальную конфигурацию: " + e.getMessage());
            if (Statistics.getInstance() != null) { Statistics.getInstance().timeFlag = true; Statistics.getInstance().showTimer(); Statistics.getInstance().setMusicEnabled(true); }
            Platform.runLater(() -> showErrorDialog("Ошибка загрузки конфига", "Не удалось загрузить настройки, используются значения по умолчанию."));
        }
    }

    public Label getLabelTimer() { return labelTimer; }
    public Label getLabelTextTIMER() { return labelTextTIMER; }
    public Pane getPane() { return visualizationPane; }

    @FXML private void clickStart() {
        Habitat hab = Habitat.getInstance(); int n1 = 1, n2 = 1, lifeTimeDev = 1, lifeTimeMan = 1, maxManagerPercent = 0;
        try {
            n1 = Integer.parseInt(fieldN1.getText()); n2 = Integer.parseInt(fieldN2.getText()); lifeTimeDev = Integer.parseInt(fieldLifeTimeDev.getText()); lifeTimeMan = Integer.parseInt(fieldLifeTimeMan.getText()); maxManagerPercent = Integer.parseInt(fieldMaxManagerPercent.getText());
            if (n1 < 1 || n2 < 1 || lifeTimeDev < 1 || lifeTimeMan < 1 || maxManagerPercent < 0 || maxManagerPercent > 100) { throw new NumberFormatException("Invalid number"); }
            hab.n1 = n1; hab.n2 = n2; hab.maxManagerPercent = maxManagerPercent; Developer.setLifeTime(lifeTimeDev); Manager.setLifeTime(lifeTimeMan);
            hab.p1 = Float.parseFloat(boxP1.getValue().replace("%", "")) / 100.0f; hab.p2 = Float.parseFloat(boxP2.getValue().replace("%", "")) / 100.0f;
            DeveloperAI.getInstance().setPriority(boxDevPriority.getValue()); ManagerAI.getInstance().setPriority(boxManPriority.getValue());
            setFieldsDisabled(true); btnStart.setDisable(true); btnStop.setDisable(false); menuStart.setDisable(true); menuStop.setDisable(false); btnDevIntellect.setDisable(false); btnManIntellect.setDisable(false);
            Statistics.getInstance().startAction();
        } catch (NumberFormatException ex) {
            try { if (!fieldN1.getText().matches("\\d+") || Integer.parseInt(fieldN1.getText()) < 1) fieldN1.setText("1"); } catch(NumberFormatException ignored) { fieldN1.setText("1"); }
            try { if (!fieldN2.getText().matches("\\d+") || Integer.parseInt(fieldN2.getText()) < 1) fieldN2.setText("2"); } catch(NumberFormatException ignored) { fieldN2.setText("2"); }
            try { if (!fieldLifeTimeDev.getText().matches("\\d+") || Integer.parseInt(fieldLifeTimeDev.getText()) < 1) fieldLifeTimeDev.setText("8"); } catch(NumberFormatException ignored) { fieldLifeTimeDev.setText("8"); }
            try { if (!fieldLifeTimeMan.getText().matches("\\d+") || Integer.parseInt(fieldLifeTimeMan.getText()) < 1) fieldLifeTimeMan.setText("10"); } catch(NumberFormatException ignored) { fieldLifeTimeMan.setText("10"); }
            try { if (!fieldMaxManagerPercent.getText().matches("\\d+") || Integer.parseInt(fieldMaxManagerPercent.getText()) < 0 || Integer.parseInt(fieldMaxManagerPercent.getText()) > 100) fieldMaxManagerPercent.setText("40"); } catch(NumberFormatException ignored) { fieldMaxManagerPercent.setText("40"); }
            if (!boxP1.getItems().contains(boxP1.getValue())) boxP1.setValue("80%"); if (!boxP2.getItems().contains(boxP2.getValue())) boxP2.setValue("100%");
            showErrorDialog("Ошибка", "Некорректное значение. Требуется целое положительное число для периодов/времени жизни, 0-100 для %.");
        } catch (Exception e) { showErrorDialog("Критическая ошибка", "Произошла непредвиденная ошибка при запуске симуляции."); e.printStackTrace(); }
    }
    public void setFieldsDisabled(boolean disabled) {
        if(fieldN1 != null) fieldN1.setDisable(disabled); if(fieldN2 != null) fieldN2.setDisable(disabled);
        if(fieldLifeTimeDev != null) fieldLifeTimeDev.setDisable(disabled); if(fieldLifeTimeMan != null) fieldLifeTimeMan.setDisable(disabled);
        if(fieldMaxManagerPercent != null) fieldMaxManagerPercent.setDisable(disabled); if(boxP1 != null) boxP1.setDisable(disabled);
        if(boxP2 != null) boxP2.setDisable(disabled); if(boxDevPriority != null) boxDevPriority.setDisable(disabled);
        if(boxManPriority != null) boxManPriority.setDisable(disabled);
    }
    @FXML private void clickStop() { Statistics.getInstance().stopAction(true); }
    @FXML private void clickTimeSwitch() { Statistics st = Statistics.getInstance(); if (st == null) return; st.timeFlag = btnShowTime.isSelected(); menuShowTime.setSelected(st.timeFlag); menuHideTime.setSelected(!st.timeFlag); st.showTimer(); }
    @FXML private void menuClickTimeSwitch() { Statistics st = Statistics.getInstance(); if (st == null) return; st.timeFlag = menuShowTime.isSelected(); btnShowTime.setSelected(st.timeFlag); btnHideTime.setSelected(!st.timeFlag); st.showTimer(); }
    @FXML private void clickInfo() { if(menuShowInfo!=null) menuShowInfo.setSelected(!menuShowInfo.isSelected()); }
    @FXML private void menuClickInfo() { if(btnShowInfo!=null) btnShowInfo.setSelected(!btnShowInfo.isSelected()); }
    @FXML private void clickSoundSwitch() { Statistics st = Statistics.getInstance(); if (st == null) return; boolean enable = btnEnableSound.isSelected(); st.setMusicEnabled(enable); menuEnableSound.setSelected(enable); menuDisableSound.setSelected(!enable); if (enable && st.startFlag) { st.playMusic(); } else if (!enable) { st.pauseMusic(); } }
    @FXML private void menuClickSoundSwitch() { Statistics st = Statistics.getInstance(); if (st == null) return; boolean enable = menuEnableSound.isSelected(); st.setMusicEnabled(enable); btnEnableSound.setSelected(enable); btnDisableSound.setSelected(!enable); if (enable && st.startFlag) { st.playMusic(); } else if (!enable) { st.pauseMusic(); } }
    @FXML public void menuClickExit() { System.out.println("Выход из приложения через меню..."); if(Client.isConnected()) { Client.disconnectFromServer(); } FileMaster.saveConfig(this); Platform.exit(); System.exit(0); }
    @FXML public void clickSave() { FileMaster.saveState(this, Habitat.getInstance(), Statistics.getInstance()); }
    @FXML public void clickLoad() { FileMaster.loadState(this, Habitat.getInstance(), Statistics.getInstance()); }
    @FXML public void clickCurrentObjects() {
        Statistics st = Statistics.getInstance(); Habitat hab = Habitat.getInstance(); if (st == null || hab == null) return;
        final boolean simulationWasRunning = st.startFlag; boolean devAiWasActiveBeforePause = DeveloperAI.getInstance().isActive; boolean manAiWasActiveBeforePause = ManagerAI.getInstance().isActive; Duration musicTimeBeforePause = Duration.ZERO; boolean musicWasPlaying = false; if(st.mediaPlayer != null) { musicTimeBeforePause = st.mediaPlayer.getCurrentTime(); musicWasPlaying = st.mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING; }
        if (simulationWasRunning) { System.out.println("Пауза симуляции для показа объектов..."); st.stopAction(false); } else { System.out.println("Симуляция не запущена, показ объектов без паузы."); }
        Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle("Информация"); alert.setHeaderText("Живые объекты"); initOwnerForDialog(alert); StringBuilder statistic = new StringBuilder();
        synchronized (hab.getObjCollection()) { synchronized (hab.getBornCollection()) { List<Person> objCopy = new ArrayList<>(hab.getObjCollection()); Map<Integer, Integer> bornCopy = new HashMap<>(hab.getBornCollection());
            if (objCopy.isEmpty()) { statistic.append("Нет живых объектов."); } else { Map<Integer, String> typeMap = objCopy.stream().filter(Objects::nonNull).collect(Collectors.toMap(Person::getId, p -> p.getClass().getSimpleName()));
                for (Map.Entry<Integer, Integer> entry : bornCopy.entrySet()) { int id = entry.getKey(); int bornTime = entry.getValue(); String type = typeMap.getOrDefault(id, "?");
                    statistic.append("ID = ").append(id).append(" (").append(type).append(")\tВремя рождения = "); if (bornTime < 60) { statistic.append(bornTime).append(" сек\n"); } else { statistic.append(bornTime / 60).append(" мин ").append(bornTime % 60).append(" сек\n"); } } } } }
        TextArea textArea = new TextArea(statistic.toString()); textArea.setPrefColumnCount(35); textArea.setEditable(false); textArea.setWrapText(true); alert.getDialogPane().setContent(textArea); alert.getDialogPane().setMinWidth(450); alert.showAndWait();
        if (simulationWasRunning) { System.out.println("Возобновление симуляции после показа объектов..."); DeveloperAI.getInstance().isActive = devAiWasActiveBeforePause; ManagerAI.getInstance().isActive = manAiWasActiveBeforePause; st.pauseTime = musicTimeBeforePause; st.musicPaused = !musicWasPlaying; st.startAction();
            Platform.runLater(() -> { if(this.btnStart != null) { this.btnStart.setDisable(true); this.btnStop.setDisable(false); this.menuStart.setDisable(true); this.menuStop.setDisable(false); this.btnDevIntellect.setDisable(false); this.btnManIntellect.setDisable(false); this.btnDevIntellect.setText(devAiWasActiveBeforePause ? "ON" : "OFF"); this.btnManIntellect.setText(manAiWasActiveBeforePause ? "ON" : "OFF"); } }); }
    }
    @FXML public void clickDevIntellect() { Statistics st = Statistics.getInstance(); if (st == null || !st.startFlag) return; DeveloperAI ai = DeveloperAI.getInstance(); boolean shouldBeActive = btnDevIntellect.getText().equals("OFF"); ai.isActive = shouldBeActive; btnDevIntellect.setText(shouldBeActive ? "ON" : "OFF"); if (shouldBeActive) { synchronized (ai.monitor) { ai.monitor.notify(); } } }
    @FXML public void clickManIntellect() { Statistics st = Statistics.getInstance(); if (st == null || !st.startFlag) return; ManagerAI ai = ManagerAI.getInstance(); boolean shouldBeActive = btnManIntellect.getText().equals("OFF"); ai.isActive = shouldBeActive; btnManIntellect.setText(shouldBeActive ? "ON" : "OFF"); if (shouldBeActive) { synchronized (ai.monitor) { ai.monitor.notify(); } } }
    @FXML void keyPressed(KeyEvent keyEvent) {
        Statistics st = Statistics.getInstance(); if (st == null) return;
        switch (keyEvent.getCode()) {
            case T: st.timeFlag = !st.timeFlag; btnShowTime.setSelected(st.timeFlag); menuShowTime.setSelected(st.timeFlag); btnHideTime.setSelected(!st.timeFlag); menuHideTime.setSelected(!st.timeFlag); st.showTimer(); keyEvent.consume(); break;
            case B: if (!st.startFlag && !btnStart.isDisabled()) { clickStart(); keyEvent.consume(); } break;
            case E: if (st.startFlag && !btnStop.isDisabled()) { clickStop(); keyEvent.consume(); } break;
            default: break;
        }
    }
    @FXML private void openConsoleWindow() { try { Window ownerWindow = getWindow(); new ConsoleWindow(ownerWindow); } catch (IOException e) { System.err.println("Ошибка открытия окна консоли: " + e.getMessage()); e.printStackTrace(); showErrorDialog("Ошибка консоли", "Не удалось загрузить интерфейс консоли.\nПодробности: " + e.getMessage()); } }


    // === ИЗМЕНЕННЫЙ ОБРАБОТЧИК ДЛЯ СЕТИ ===
    @FXML
    private void handleConnectDisconnect() {
        if (Client.isConnected()) {
            Client.disconnectFromServer();
        } else {
            // Вместо showServerConnectionDialog(), теперь вызываем общий диалог
            boolean connectionSucceeded = Client.showLoginDialogAndAttemptConnect();

            // Client.setController(this) должен был быть вызван при старте приложения
            // или при предыдущем успешном подключении через этот же метод.
            // Убедимся, что Client знает о текущем контроллере.
            Client.setController(this);


            if (connectionSucceeded) {
                // Если showLoginDialogAndAttemptConnect вернул true, значит подключение успешно
                // и Client.connectToServer уже обновил статус.
                // Дополнительно можно обновить здесь, если есть специфичная логика для этого контроллера.
                Client.updateConnectionStatusUI(true, "Подключен как " + Client.getUserName());
            } else {
                // Если вернул false, значит либо пользователь выбрал оффлайн,
                // либо подключение не удалось (ошибка уже должна была быть показана).
                // В любом из этих случаев статус должен быть "Оффлайн".
                Client.updateConnectionStatusUI(false, "Оффлайн");
            }
        }
    }

    @FXML
    private void handleExchangeObjects() {
        if (exchangeUserBox == null) return;
        String targetUser = exchangeUserBox.getValue();
        int typeToReceive = 0; // Хотим получить Developer (значит, отдаем Manager)
        Client.sendObjectExchangeRequest(targetUser, typeToReceive);
    }

    public void updateUserList(List<String> users) {
        if(userListBox == null || exchangeUserBox == null) { System.err.println("Controller: UI для списка пользователей null."); return; }
        userListBox.clear(); String currentSelection = exchangeUserBox.getValue(); exchangeUserBox.getItems().clear(); exchangeUserBox.getItems().add(NO_USER_SELECTED);
        String ownUsernameMarker = " (You)"; StringBuilder sb = new StringBuilder(); boolean selfFound = false;
        String clientUserName = Client.getUserName(); // Получаем имя один раз
        for (String user : users) { if (user == null) continue;
            // От сервера может прийти имя с маркером (You), если сервер так формирует список
            // Или может прийти чистое имя, тогда мы добавляем маркер сами
            if (user.equals(clientUserName) || user.equals(clientUserName + ownUsernameMarker)) {
                sb.append(clientUserName).append(ownUsernameMarker).append("\n");
                selfFound = true;
            } else {
                sb.append(user).append("\n");
                exchangeUserBox.getItems().add(user);
            }
        }
        // Если после цикла себя не нашли (например, сервер прислал список без нашего имени или маркера)
        // и имя клиента известно, добавляем его с маркером в начало.
        if (!selfFound && clientUserName != null) {
            userListBox.setText(clientUserName + ownUsernameMarker + "\n" + sb.toString());
        } else {
            userListBox.setText(sb.toString());
        }

        if (exchangeUserBox.getItems().contains(currentSelection)) { exchangeUserBox.setValue(currentSelection); }
        else { exchangeUserBox.setValue(NO_USER_SELECTED); }
        updateExchangeButtonState(); System.out.println("Controller: Список пользователей обновлен.");
    }
    public void addUserToList(String userName) {
        if(userListBox == null || exchangeUserBox == null || userName == null) return;
        String userLine = userName + "\n"; if (!userListBox.getText().contains(userLine)) { userListBox.appendText(userLine); System.out.println("Controller: Пользователь добавлен в TextArea: " + userName); }
        if (!userName.equals(Client.getUserName()) && !userName.equals(NO_USER_SELECTED) && !exchangeUserBox.getItems().contains(userName)) { exchangeUserBox.getItems().add(userName); System.out.println("Controller: Пользователь добавлен в ComboBox: " + userName); }
        updateExchangeButtonState();
    }
    public void removeUserFromList(String userName) {
        if(userListBox == null || exchangeUserBox == null || userName == null) return; String userLine = userName + "\n"; String currentText = userListBox.getText();
        if (currentText.contains(userLine)) { userListBox.setText(currentText.replace(userLine, "")); System.out.println("Controller: Пользователь удален из TextArea: " + userName); }
        boolean removed = exchangeUserBox.getItems().remove(userName); if(removed) { System.out.println("Controller: Пользователь удален из ComboBox: " + userName); }
        String currentSelection = exchangeUserBox.getValue();
        if (userName.equals(currentSelection)) { // Если удалили выбранного пользователя
            exchangeUserBox.setValue(NO_USER_SELECTED);
        } else if (currentSelection != null && !exchangeUserBox.getItems().contains(currentSelection) && !currentSelection.equals(NO_USER_SELECTED)) {
            // Если текущий выбор больше не валиден (но это не NO_USER_SELECTED)
            exchangeUserBox.setValue(NO_USER_SELECTED);
        }
        updateExchangeButtonState();
    }
    public void clearUserList() {
        if(userListBox != null) userListBox.clear(); if(exchangeUserBox != null) { exchangeUserBox.getItems().clear(); exchangeUserBox.getItems().add(NO_USER_SELECTED); exchangeUserBox.setValue(NO_USER_SELECTED); }
        updateExchangeButtonState(); System.out.println("Controller: Список пользователей очищен.");
    }
    public void updateConnectionStatus(boolean connected, String message) {
        if (labelConnectionStatus != null) { labelConnectionStatus.setText(message); labelConnectionStatus.setStyle(connected ? "-fx-text-fill: green;" : "-fx-text-fill: red;"); }
        if (menuConnect != null) { menuConnect.setText(connected ? "Отключиться от сервера" : "Подключиться к серверу..."); }
        updateExchangeButtonState();
    }
    public void updateExchangeButtonState() {
        if(btnExchangeObjects == null || exchangeUserBox == null) return;
        boolean connected = Client.isConnected(); boolean userSelected = exchangeUserBox.getValue() != null && !exchangeUserBox.getValue().equals(NO_USER_SELECTED);
        btnExchangeObjects.setDisable(!(connected && userSelected));
    }

    // Старый диалог подключения только по IP/порту, можно оставить для отладки или убрать
    private void showServerConnectionDialog() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Подключиться к серверу (IP/Порт)");
        dialog.setHeaderText("Введите IP и порт сервера."); initOwnerForDialog(dialog);
        ButtonType connectButtonType = new ButtonType("Подключиться", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField ipField = new TextField("localhost"); ipField.setPromptText("IP сервера");
        TextField portField = new TextField("8030"); portField.setPromptText("Порт сервера");
        grid.add(new Label("Сервер IP:"), 0, 0); grid.add(ipField, 1, 0);
        grid.add(new Label("Порт:"), 0, 1); grid.add(portField, 1, 1);
        dialog.getDialogPane().setContent(grid); Platform.runLater(ipField::requestFocus);
        Node connectButtonNode = dialog.getDialogPane().lookupButton(connectButtonType);
        connectButtonNode.setDisable(false); // Кнопка активна сразу

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                try {
                    String ip = ipField.getText().trim();
                    int port = Integer.parseInt(portField.getText().trim());
                    if (ip.isEmpty()) {
                        showErrorDialog("Ошибка ввода", "IP адрес не может быть пустым.");
                        return null;
                    }
                    // Передаем `this` в качестве контроллера, т.к. этот диалог вызывается из Controller
                    boolean success = Client.connectToServer(ip, port, Client.getUserName(), this);
                    if (success) {
                        Client.updateConnectionStatusUI(true, "Подключен как " + Client.getUserName());
                    } else {
                        Client.updateConnectionStatusUI(false, "Ошибка подключения");
                    }
                    return success ? connectButtonType : null; // Возвращаем ButtonType только при успехе
                } catch (NumberFormatException e) {
                    showErrorDialog("Ошибка ввода", "Неверный номер порта.");
                    return null;
                } catch (Exception e) {
                    showErrorDialog("Ошибка", "Произошла ошибка: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        dialog.showAndWait();
    }


    public void showErrorDialog(String title, String content) { if (!Platform.isFxApplicationThread()) { Platform.runLater(() -> showErrorDialogInternal(title, content)); } else { showErrorDialogInternal(title, content); } }
    private void showErrorDialogInternal(String title, String content) { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); initOwnerForDialog(alert); alert.showAndWait(); }
    public void showInfoDialog(String title, String content) { if (!Platform.isFxApplicationThread()) { Platform.runLater(() -> showInfoDialogInternal(title, content)); } else { showInfoDialogInternal(title, content); } }
    private void showInfoDialogInternal(String title, String content) { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); initOwnerForDialog(alert); alert.showAndWait(); }

    private Window getWindow() {
        // Пытаемся получить окно из разных корневых элементов
        if (gridPane != null && gridPane.getScene() != null) return gridPane.getScene().getWindow();
        if (leftPaneVBox != null && leftPaneVBox.getScene() != null) return leftPaneVBox.getScene().getWindow();
        if (visualizationPane != null && visualizationPane.getScene() != null) return visualizationPane.getScene().getWindow();
        // Fallback, если другие не сработали (маловероятно, если UI отображается)
        if (labelConnectionStatus != null && labelConnectionStatus.getScene() != null) return labelConnectionStatus.getScene().getWindow();
        return null;
    }

    private void initOwnerForDialog(Dialog<?> dialog) {
        Window owner = getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        } else {
            System.err.println("Предупреждение: Не удалось определить родительское окно для диалога '" + dialog.getTitle() + "'. Диалог будет без владельца.");
        }
    }
}