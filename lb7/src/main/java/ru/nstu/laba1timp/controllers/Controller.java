package ru.nstu.laba1timp.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
// import javafx.scene.media.MediaPlayer; // Закомментировано, т.к. MediaPlayer.Status используется ниже
import javafx.stage.Window;
import ru.nstu.laba1timp.console.ConsoleWindow;
import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;
import ru.nstu.laba1timp.model.DeveloperAI;
import ru.nstu.laba1timp.model.ManagerAI;
import javafx.scene.image.ImageView;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Основной контроллер приложения.
 * Связывает интерфейс (FXML) с логикой программы.
 */
public class Controller {
    private DatabaseManager dbManager;

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
    @FXML public MenuItem menuManageDBSaves;

    /**
     * Устанавливает менеджер базы данных.
     */
    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Инициализация FXML-элементов.
     */
    @FXML
    void initialize() {
        btnStop.setDisable(true); menuStop.setDisable(true); btnShowTime.setSelected(true); menuShowTime.setSelected(true);
        btnEnableSound.setSelected(true); menuEnableSound.setSelected(true); fieldN1.setText("1"); fieldN2.setText("2");
        fieldLifeTimeDev.setText("8"); fieldLifeTimeMan.setText("10"); fieldMaxManagerPercent.setText("40");

        for(int v=0;v<=100;v+=10){
            boxP1.getItems().add(v+"%");
            boxP2.getItems().add(v+"%");
        }
        boxP1.setValue("80%");boxP2.setValue("100%");

        for(int v=Thread.MIN_PRIORITY;v<=Thread.MAX_PRIORITY;v++){
            boxDevPriority.getItems().add(v);
            boxManPriority.getItems().add(v);
        }
        boxDevPriority.setValue(Thread.NORM_PRIORITY);
        boxManPriority.setValue(Thread.NORM_PRIORITY);

        btnDevIntellect.setDisable(true);
        btnManIntellect.setDisable(true);

        try {
            Statistics stats = Statistics.getInstance();
            if (stats != null) {
                stats.setMainController(this);
            }
            FileMaster.loadConfig(this);
            if (stats != null) {
                stats.timeFlag = btnShowTime.isSelected();
                stats.showTimer();
            }
        } catch (Exception e) {
            System.err.println("Не удалось загрузить начальную конфигурацию: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Предупреждение загрузки");
            alert.setHeaderText("Не удалось загрузить настройки.");
            alert.setContentText("Используются значения по умолчанию. Ошибка: " + e.getMessage());
            alert.showAndWait();

            if(Statistics.getInstance() != null) {
                Statistics.getInstance().timeFlag = true;
                Statistics.getInstance().showTimer();
            }
        }
    }

    /**
     * Возвращает панель визуализации.
     */
    public Pane getPane() { return visualizationPane; }

    /**
     * Возвращает метку времени.
     */
    public Label getLabelTimer() { return labelTimer; }

    /**
     * Возвращает текст метки времени.
     */
    public Label getLabelTextTIMER() { return labelTextTIMER; }

    /**
     * Обработчик кнопки "Старт".
     */
    @FXML
    private void clickStart() {
        Habitat hab = Habitat.getInstance();
        int n1=1, n2=1, lifeTimeDev=1, lifeTimeMan=1, maxManagerPercent=0;
        try {
            n1 = Integer.parseInt(fieldN1.getText());
            n2 = Integer.parseInt(fieldN2.getText());
            lifeTimeDev = Integer.parseInt(fieldLifeTimeDev.getText());
            lifeTimeMan = Integer.parseInt(fieldLifeTimeMan.getText());
            maxManagerPercent = Integer.parseInt(fieldMaxManagerPercent.getText());

            if (n1<1||n2<1||lifeTimeDev<1||lifeTimeMan<1||maxManagerPercent<0||maxManagerPercent>100)
                throw new NumberFormatException("Invalid number");

            hab.n1=n1;
            hab.n2=n2;
            hab.maxManagerPercent=maxManagerPercent;
            Developer.setLifeTime(lifeTimeDev);
            Manager.setLifeTime(lifeTimeMan);

            hab.p1=Float.parseFloat(boxP1.getValue().replace("%",""))/100;
            hab.p2=Float.parseFloat(boxP2.getValue().replace("%",""))/100;

            DeveloperAI.getInstance().setPriority(boxDevPriority.getValue());
            ManagerAI.getInstance().setPriority(boxManPriority.getValue());

            setFieldsDisabled(true);
            btnStart.setDisable(true);
            btnStop.setDisable(false);
            menuStart.setDisable(true);
            menuStop.setDisable(false);
            btnDevIntellect.setDisable(false);
            btnManIntellect.setDisable(false);

            Statistics.getInstance().startAction();
        } catch (NumberFormatException ex) {
            if (!fieldN1.getText().matches("\\d+")||n1<1) fieldN1.setText("1");
            if (!fieldN2.getText().matches("\\d+")||n2<1) fieldN2.setText("2");
            if (!fieldLifeTimeDev.getText().matches("\\d+")||lifeTimeDev<1) fieldLifeTimeDev.setText("8");
            if (!fieldLifeTimeMan.getText().matches("\\d+")||lifeTimeMan<1) fieldLifeTimeMan.setText("10");
            if (!fieldMaxManagerPercent.getText().matches("\\d+")||maxManagerPercent<0||maxManagerPercent>100) fieldMaxManagerPercent.setText("40");

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Некорректное значение");
            alert.setContentText("Требуется целое положительное число для периодов/времени жизни, 0-100 для %.");
            alert.showAndWait();
        }
    }

    /**
     * Блокирует или разблокирует поля ввода.
     */
    public void setFieldsDisabled(boolean disabled) {
        fieldN1.setDisable(disabled);
        fieldN2.setDisable(disabled);
        fieldLifeTimeDev.setDisable(disabled);
        fieldLifeTimeMan.setDisable(disabled);
        fieldMaxManagerPercent.setDisable(disabled);
        boxP1.setDisable(disabled);
        boxP2.setDisable(disabled);
        boxDevPriority.setDisable(disabled);
        boxManPriority.setDisable(disabled);
    }

    /**
     * Обработчик кнопки "Стоп".
     */
    @FXML
    private void clickStop() {
        Statistics.getInstance().stopAction(true); // Полная остановка, показать статистику
    }

    /**
     * Переключатель отображения времени.
     */
    @FXML
    private void clickTimeSwitch() {
        Statistics st = Statistics.getInstance();
        if(st==null) return;
        st.timeFlag = btnShowTime.isSelected();
        menuShowTime.setSelected(st.timeFlag);
        menuHideTime.setSelected(!st.timeFlag);
        st.showTimer();
    }

    /**
     * Переключатель отображения времени через меню.
     */
    @FXML
    private void menuClickTimeSwitch() {
        Statistics st = Statistics.getInstance();
        if(st==null) return;
        st.timeFlag = menuShowTime.isSelected();
        btnShowTime.setSelected(st.timeFlag);
        btnHideTime.setSelected(!st.timeFlag);
        st.showTimer();
    }

    /**
     * Переключатель отображения информации.
     */
    @FXML
    private void clickInfo() {
        menuShowInfo.setSelected(!menuShowInfo.isSelected());
    }

    /**
     * Переключатель отображения информации через меню.
     */
    @FXML
    private void menuClickInfo() {
        btnShowInfo.setSelected(!btnShowInfo.isSelected());
    }

    /**
     * Переключатель звука.
     */
    @FXML
    private void clickSoundSwitch() {
        Statistics st=Statistics.getInstance();
        if(st==null) return;
        if (btnEnableSound.isSelected()){
            st.setMusicEnabled(true);
            menuEnableSound.setSelected(true);
            menuDisableSound.setSelected(false);
            if(st.startFlag&&(st.mediaPlayer==null||st.mediaPlayer.getStatus()!=javafx.scene.media.MediaPlayer.Status.PLAYING)){
                st.playMusic();
            }
        } else if(btnDisableSound.isSelected()){
            st.setMusicEnabled(false);
            menuEnableSound.setSelected(false);
            menuDisableSound.setSelected(true);
            if(st.mediaPlayer!=null&&st.mediaPlayer.getStatus()==javafx.scene.media.MediaPlayer.Status.PLAYING){
                st.pauseMusic();
            }
        }
    }

    /**
     * Переключатель звука через меню.
     */
    @FXML
    private void menuClickSoundSwitch() {
        Statistics st=Statistics.getInstance();
        if(st==null) return;
        if(menuEnableSound.isSelected()){
            st.setMusicEnabled(true);
            btnEnableSound.setSelected(true);
            btnDisableSound.setSelected(false);
            if(st.startFlag&&(st.mediaPlayer==null||st.mediaPlayer.getStatus()!=javafx.scene.media.MediaPlayer.Status.PLAYING)){
                st.playMusic();
            }
        } else if(menuDisableSound.isSelected()){
            st.setMusicEnabled(false);
            btnEnableSound.setSelected(false);
            btnDisableSound.setSelected(true);
            if(st.mediaPlayer!=null&&st.mediaPlayer.getStatus()==javafx.scene.media.MediaPlayer.Status.PLAYING){
                st.pauseMusic();
            }
        }
    }

    /**
     * Выход из приложения.
     */
    @FXML
    public void menuClickExit() {
        FileMaster.saveConfig(this);
        System.exit(0);
    }

    /**
     * Сохраняет текущее состояние.
     */
    @FXML
    public void clickSave() {
        FileMaster.saveState(this, Habitat.getInstance(), Statistics.getInstance());
    }

    /**
     * Загружает сохранённое состояние.
     */
    @FXML
    public void clickLoad() {
        FileMaster.loadState(this, Habitat.getInstance(), Statistics.getInstance());
    }

    /**
     * Отображает информацию о текущих объектах.
     */
    @FXML
    public void clickCurrentObjects() {
        Statistics st = Statistics.getInstance();
        Habitat hab = Habitat.getInstance();
        if (st == null || hab == null) return;

        final boolean simulationWasRunning = st.startFlag;
        boolean devAiWasActiveBeforePause = false;
        boolean manAiWasActiveBeforePause = false;

        if (simulationWasRunning) {
            System.out.println("Пауза симуляции для показа объектов...");
            devAiWasActiveBeforePause = DeveloperAI.getInstance().isActive;
            manAiWasActiveBeforePause = ManagerAI.getInstance().isActive;
            st.stopAction(false, false);
        } else {
            System.out.println("Симуляция не запущена, показ объектов без паузы.");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText("Живые объекты");

        String statistic = "";
        synchronized (hab.getObjCollection()) {
            synchronized (hab.getBornCollection()) {
                Map<Integer, Integer> bornCopy = new HashMap<>(hab.getBornCollection());
                if (bornCopy.isEmpty()){
                    statistic = "Нет живых объектов.";
                } else {
                    for (Map.Entry<Integer, Integer> entry : bornCopy.entrySet()) {
                        int id = entry.getKey();
                        int bornTime = entry.getValue();
                        String type = hab.getObjCollection().stream()
                                .filter(p -> p != null && p.getId() == id)
                                .map(p -> p.getClass().getSimpleName())
                                .findFirst()
                                .orElse("?");

                        statistic += "ID = " + id + " (" + type + ")\tВремя рождения = ";
                        if (bornTime < 60) {
                            statistic += bornTime + " сек\n";
                        } else {
                            statistic += (bornTime / 60) + " мин " + (bornTime % 60) + " сек\n";
                        }
                    }
                }
            }
        }

        TextArea textArea = new TextArea(statistic);
        textArea.setPrefColumnCount(30);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setMinWidth(400);

        alert.showAndWait();

        if (simulationWasRunning) {
            System.out.println("Возобновление симуляции после показа объектов...");
            final boolean finalDevAiWasActive = devAiWasActiveBeforePause;
            final boolean finalManAiWasActive = manAiWasActiveBeforePause;

            DeveloperAI.getInstance().isActive = finalDevAiWasActive;
            ManagerAI.getInstance().isActive = finalManAiWasActive;

            st.startAction();

            Platform.runLater(() -> {
                this.btnStart.setDisable(true);
                this.btnStop.setDisable(false);
                this.menuStart.setDisable(true);
                this.menuStop.setDisable(false);
                this.btnDevIntellect.setDisable(false);
                this.btnManIntellect.setDisable(false);
                this.btnDevIntellect.setText(finalDevAiWasActive ? "ON" : "OFF");
                this.btnManIntellect.setText(finalManAiWasActive ? "ON" : "OFF");
            });
        }
    }

    /**
     * Переключатель интеллекта разработчика.
     */
    @FXML
    public void clickDevIntellect() {
        Statistics st = Statistics.getInstance();
        if (st == null || !st.startFlag) return;
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

    /**
     * Переключатель интеллекта менеджера.
     */
    @FXML
    public void clickManIntellect() {
        Statistics st = Statistics.getInstance();
        if (st == null || !st.startFlag) return;
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

    /**
     * Обработка нажатий клавиш.
     */
    @FXML
    void keyPressed(KeyEvent keyEvent) {
        keyEvent.consume();
        Statistics st = Statistics.getInstance();
        if(st == null) return;
        switch (keyEvent.getCode()) {
            case T:
                st.timeFlag = !st.timeFlag;
                btnShowTime.setSelected(st.timeFlag);
                menuShowTime.setSelected(st.timeFlag);
                btnHideTime.setSelected(!st.timeFlag);
                menuHideTime.setSelected(!st.timeFlag);
                st.showTimer();
                break;
            case B:
                if (!st.startFlag) {
                    clickStart();
                }
                break;
            case E:
                if (st.startFlag) {
                    clickStop();
                }
                break;
        }
    }

    /**
     * Открытие окна консоли.
     */
    @FXML
    private void openConsoleWindow() {
        try {
            Window ownerWindow = leftPane != null && leftPane.getScene() != null ? leftPane.getScene().getWindow() : null;
            new ConsoleWindow(ownerWindow);
        } catch (IOException e) {
            System.err.println("Ошибка открытия окна консоли: " + e.getMessage());
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Ошибка консоли");
            errorAlert.setHeaderText("Не удалось загрузить интерфейс консоли.");
            errorAlert.setContentText("Подробности: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * Управление сохранениями в БД.
     */
    @FXML
    private void openManageDBSavesWindow() {
        if (dbManager == null) {
            showError("Ошибка Базы Данных", "Менеджер базы данных не инициализирован.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Управление сохранениями в БД (v2)");
        dialog.setHeaderText("Сохранение и загрузка состояний симуляции");

        dialog.getDialogPane().setMinWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField saveSlotNameField = new TextField();
        saveSlotNameField.setPromptText("Имя нового/существующего слота");
        saveSlotNameField.setPrefWidth(200);

        ComboBox<DatabaseManager.SaveType> saveTypeComboBox = new ComboBox<>(
                FXCollections.observableArrayList(DatabaseManager.SaveType.values())
        );
        saveTypeComboBox.setValue(DatabaseManager.SaveType.ALL_PERSONS);

        Button saveButton = new Button("Сохранить в слот");

        ComboBox<String> loadSlotNameComboBox = new ComboBox<>();
        loadSlotNameComboBox.setPromptText("Выберите слот для загрузки");
        loadSlotNameComboBox.setPrefWidth(200);

        ComboBox<DatabaseManager.SaveType> loadTypeComboBox = new ComboBox<>(
                FXCollections.observableArrayList(DatabaseManager.SaveType.values())
        );
        loadTypeComboBox.setValue(DatabaseManager.SaveType.ALL_PERSONS);

        Button loadButton = new Button("Загрузить из слота");
        CheckBox combineLoadCheckBox = new CheckBox("Комбинировать с текущими");
        combineLoadCheckBox.setTooltip(new Tooltip("Если отмечено, объекты добавятся (того же типа будут заменены, если ID совпадет).\nЕсли не отмечено, соответствующие типы объектов будут очищены перед загрузкой."));

        ComboBox<String> manageSlotNameComboBox = new ComboBox<>();
        manageSlotNameComboBox.setPromptText("Выберите слот для управления");
        manageSlotNameComboBox.setPrefWidth(200);

        Button renameButton = new Button("Переименовать");
        Button deleteButton = new Button("Удалить");
        Button viewInfoButton = new Button("Инфо о слоте");

        Runnable refreshSlotComboBoxes = () -> {
            try {
                List<String> slotNames = dbManager.getSavedSlotNames();
                Platform.runLater(() -> {
                    String selectedLoad = loadSlotNameComboBox.getValue();
                    String selectedManage = manageSlotNameComboBox.getValue();
                    loadSlotNameComboBox.setItems(FXCollections.observableArrayList(slotNames));
                    manageSlotNameComboBox.setItems(FXCollections.observableArrayList(slotNames));
                    if (slotNames.contains(selectedLoad)) loadSlotNameComboBox.setValue(selectedLoad);
                    else loadSlotNameComboBox.setValue(null);
                    if (slotNames.contains(selectedManage)) manageSlotNameComboBox.setValue(selectedManage);
                    else manageSlotNameComboBox.setValue(null);
                });
            } catch (SQLException e) {
                showError("Ошибка БД", "Не удалось загрузить список слотов: " + e.getMessage());
            }
        };

        refreshSlotComboBoxes.run();

        grid.add(new Label("Имя слота для сохранения:"), 0, 0);
        grid.add(saveSlotNameField, 1, 0);
        grid.add(new Label("Тип сохранения:"), 0, 1);
        grid.add(saveTypeComboBox, 1, 1);
        grid.add(saveButton, 2, 1);
        grid.add(new Separator(), 0, 2, 3, 1);

        grid.add(new Label("Загрузить из слота:"), 0, 3);
        grid.add(loadSlotNameComboBox, 1, 3);
        grid.add(new Label("Тип загрузки:"), 0, 4);
        grid.add(loadTypeComboBox, 1, 4);
        grid.add(loadButton, 2, 3);
        grid.add(combineLoadCheckBox, 2, 4);

        grid.add(new Separator(), 0, 5, 3, 1);

        grid.add(new Label("Управление слотом:"), 0, 6);
        grid.add(manageSlotNameComboBox, 1, 6);

        HBox manageButtons = new HBox(5, viewInfoButton, renameButton, deleteButton);
        grid.add(manageButtons, 0, 7, 3, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        saveButton.setOnAction(event -> {
            String slotName = saveSlotNameField.getText();
            DatabaseManager.SaveType saveType = saveTypeComboBox.getValue();
            if (slotName == null || slotName.trim().isEmpty() || saveType == null) {
                showError("Ошибка ввода", "Укажите имя слота и тип сохранения.");
                return;
            }

            try {
                List<String> existingSlots = dbManager.getSavedSlotNames();
                if (existingSlots.contains(slotName)) {
                    Map<String, String> metadata = dbManager.getSlotMetadata(slotName);
                    DatabaseManager.SaveType existingSaveType = metadata.containsKey("save_type") ?
                            DatabaseManager.SaveType.valueOf(metadata.get("save_type")) : null;
                    String message = "Слот '" + slotName + "' уже существует.";
                    if (existingSaveType != null) {
                        message += "\nТекущий тип данных в слоте: " + existingSaveType.name() + ".";
                    }
                    message += "\nПерезаписать данные для типа '" + saveType.name() + "' в этом слоте?";
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> res = confirm.showAndWait();
                    if (res.isEmpty() || res.get() == ButtonType.NO) return;
                }
            } catch (SQLException e) {
                showError("Ошибка БД", "Не удалось проверить имя слота: " + e.getMessage());
                return;
            }

            performSaveToDBSlot(slotName, saveType);
            refreshSlotComboBoxes.run();
        });

        loadButton.setOnAction(event -> {
            String slotName = loadSlotNameComboBox.getValue();
            DatabaseManager.SaveType loadType = loadTypeComboBox.getValue();
            boolean combine = combineLoadCheckBox.isSelected();
            if (slotName == null || slotName.trim().isEmpty() || loadType == null) {
                showError("Ошибка выбора", "Выберите слот и тип для загрузки.");
                return;
            }

            performLoadFromDBSlot(slotName, loadType, combine);
        });

        deleteButton.setOnAction(event -> {
            String slotName = manageSlotNameComboBox.getValue();
            if (slotName == null || slotName.trim().isEmpty()) {
                showError("Ошибка выбора", "Выберите слот для удаления.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Уверены, что хотите удалить слот '" + slotName + "' со всеми его данными?",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    try {
                        dbManager.deleteSlot(slotName);
                        showInfo("Успех", "Слот '" + slotName + "' удален.");
                        refreshSlotComboBoxes.run();
                    } catch (SQLException e) {
                        showError("Ошибка БД", "Не удалось удалить слот: " + e.getMessage());
                    }
                }
            });
        });

        renameButton.setOnAction(event -> {
            String oldName = manageSlotNameComboBox.getValue();
            if (oldName == null || oldName.trim().isEmpty()) {
                showError("Ошибка выбора", "Выберите слот для переименования.");
                return;
            }

            TextInputDialog tid = new TextInputDialog(oldName);
            tid.setTitle("Переименовать слот");
            tid.setHeaderText("Переименование слота '" + oldName + "'");
            tid.setContentText("Новое имя:");
            tid.showAndWait().ifPresent(newName -> {
                if (newName.trim().isEmpty() || newName.trim().equals(oldName)) return;
                try {
                    dbManager.renameSlot(oldName, newName);
                    showInfo("Успех", "Слот переименован в '" + newName + "'.");
                    refreshSlotComboBoxes.run();
                    manageSlotNameComboBox.setValue(newName);
                } catch (SQLException e) {
                    showError("Ошибка БД", "Не удалось переименовать слот: " + e.getMessage());
                }
            });
        });

        viewInfoButton.setOnAction(event -> {
            String slotName = manageSlotNameComboBox.getValue();
            if (slotName == null || slotName.trim().isEmpty()) {
                showError("Ошибка выбора", "Выберите слот для просмотра информации.");
                return;
            }

            try {
                Map<String, String> metadata = dbManager.getSlotMetadata(slotName);
                if (metadata.isEmpty()) {
                    showInfo("Информация о слоте",
                            "Нет метаданных для слота '" + slotName + "'.\nВозможно, слот был создан частично или поврежден.");
                } else {
                    String info = "Слот: " + slotName + "\n" +
                            "Тип данных в слоте: " + metadata.getOrDefault("save_type", "N/A") + "\n" +
                            "Сохранено разработчиков: " + metadata.getOrDefault("developer_count", "0") + "\n" +
                            "Сохранено менеджеров: " + metadata.getOrDefault("manager_count", "0") + "\n" +
                            "Дата создания/обновления: " + metadata.getOrDefault("created_at", "N/A");

                    Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                    infoAlert.setTitle("Информация о слоте '" + slotName + "'");
                    infoAlert.setHeaderText(null);
                    infoAlert.setContentText(info);
                    infoAlert.showAndWait();
                }
            } catch (SQLException e) {
                showError("Ошибка БД", "Не удалось получить информацию о слоте: " + e.getMessage());
            }
        });

        Runnable updateManageButtonsState = () -> {
            boolean noSlotSelected = manageSlotNameComboBox.getValue() == null;
            deleteButton.setDisable(noSlotSelected);
            renameButton.setDisable(noSlotSelected);
            viewInfoButton.setDisable(noSlotSelected);
        };

        manageSlotNameComboBox.valueProperty().addListener((obs,ov,nv) -> updateManageButtonsState.run());
        manageSlotNameComboBox.itemsProperty().addListener((obs, oldList, newList) -> updateManageButtonsState.run());
        updateManageButtonsState.run();

        Runnable updateLoadButtonState = () -> {
            loadButton.setDisable(loadSlotNameComboBox.getValue() == null || loadTypeComboBox.getValue() == null);
        };

        loadSlotNameComboBox.valueProperty().addListener((obs,ov,nv) -> updateLoadButtonState.run());
        loadSlotNameComboBox.itemsProperty().addListener((obs, oldList, newList) -> updateLoadButtonState.run());
        loadTypeComboBox.valueProperty().addListener((obs,ov,nv) -> updateLoadButtonState.run());
        updateLoadButtonState.run();

        dialog.showAndWait();
    }

    /**
     * Сохраняет данные в БД в указанный слот.
     */
    private void performSaveToDBSlot(String slotName, DatabaseManager.SaveType saveType) {
        if (dbManager == null) {
            showError("Ошибка", "DatabaseManager не инициализирован.");
            return;
        }

        Habitat hab = Habitat.getInstance();
        Statistics stats = Statistics.getInstance();
        List<Person> allPersonsInHabitat = new ArrayList<>(hab.getObjCollection());

        List<Person> personsToSaveInDB;
        switch (saveType) {
            case DEVELOPERS_ONLY:
                personsToSaveInDB = allPersonsInHabitat.stream().filter(Developer.class::isInstance).collect(Collectors.toList());
                break;
            case MANAGERS_ONLY:
                personsToSaveInDB = allPersonsInHabitat.stream().filter(Manager.class::isInstance).collect(Collectors.toList());
                break;
            case ALL_PERSONS:
            default:
                personsToSaveInDB = allPersonsInHabitat;
                break;
        }

        Map<Integer, Integer> bornTimesToSave = new HashMap<>();
        Map<Integer, Integer> currentBornCollection = hab.getBornCollection();
        for (Person p : personsToSaveInDB) {
            bornTimesToSave.put(p.getId(), currentBornCollection.getOrDefault(p.getId(), stats.getTime()));
        }

        boolean simulationWasRunning = stats.startFlag;
        boolean devAiWasActiveBeforePause = false;
        boolean manAiWasActiveBeforePause = false;

        if (simulationWasRunning) {
            devAiWasActiveBeforePause = DeveloperAI.getInstance().isActive;
            manAiWasActiveBeforePause = ManagerAI.getInstance().isActive;
            stats.stopAction(false, false);
        }

        try {
            dbManager.savePersonsToSlot(personsToSaveInDB, slotName, saveType, bornTimesToSave);
            showInfo("Сохранение в БД", "Данные типа '" + saveType.name() + "' сохранены в слот '" + slotName + "'.");
        } catch (SQLException e) {
            showError("Ошибка сохранения в БД", "Не удалось сохранить: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (simulationWasRunning) {
                DeveloperAI.getInstance().isActive = devAiWasActiveBeforePause;
                ManagerAI.getInstance().isActive = manAiWasActiveBeforePause;
                stats.startAction();
            }
        }
    }

    /**
     * Загружает данные из БД из указанного слота.
     */
    private void performLoadFromDBSlot(String slotName, DatabaseManager.SaveType loadType, boolean combineLoad) {
        if (dbManager == null) {
            showError("Ошибка", "DatabaseManager не инициализирован.");
            return;
        }

        Habitat hab = Habitat.getInstance();
        Statistics stats = Statistics.getInstance();

        if (stats.startFlag) {
            stats.stopAction(false, true);
        }

        stats.restartFlag = false;
        stats.setTimeFromLoad(0);

        if (!combineLoad) {
            if (loadType == DatabaseManager.SaveType.ALL_PERSONS) {
                hab.clearObjects();
            } else if (loadType == DatabaseManager.SaveType.DEVELOPERS_ONLY) {
                hab.removeAllDevelopers();
            } else if (loadType == DatabaseManager.SaveType.MANAGERS_ONLY) {
                hab.removeAllManagers();
            }
        }

        Platform.runLater(() -> {
            btnStart.setDisable(false);
            btnStop.setDisable(true);
            menuStart.setDisable(false);
            menuStop.setDisable(true);
            btnDevIntellect.setDisable(true);
            btnManIntellect.setDisable(true);
            btnDevIntellect.setText("ON");
            btnManIntellect.setText("ON");
            setFieldsDisabled(false);
        });

        try {
            Map<String, Object> loadedData = dbManager.loadPersonsFromSlot(slotName, loadType);
            if (loadedData != null && loadedData.containsKey("persons")) {
                @SuppressWarnings("unchecked")
                List<Person> loadedPersons = (List<Person>) loadedData.get("persons");

                @SuppressWarnings("unchecked")
                Map<Integer, Integer> loadedBornTimes = (Map<Integer, Integer>) loadedData.getOrDefault("bornTimes", new HashMap<Integer,Integer>());

                synchronized (hab.getObjCollection()) {
                    synchronized (hab.getBornCollection()) {
                        synchronized (hab.getIdCollection()) {
                            for (Person p : loadedPersons) {
                                if (combineLoad && hab.getIdCollection().contains(p.getId())) {
                                    Optional<Person> existingPersonOpt = hab.getObjCollection().stream()
                                            .filter(ep -> ep.getId() == p.getId())
                                            .findFirst();

                                    if (existingPersonOpt.isPresent()) {
                                        Person existingPerson = existingPersonOpt.get();
                                        final ImageView oldView = existingPerson.getImageView();
                                        if (oldView != null && visualizationPane != null) {
                                            Platform.runLater(() -> visualizationPane.getChildren().remove(oldView));
                                        }
                                        hab.getObjCollection().remove(existingPerson);
                                        hab.getBornCollection().remove(existingPerson.getId());
                                    }
                                    System.out.println("Комбинированная загрузка: Объект ID " + p.getId() + " заменен.");
                                }

                                hab.getObjCollection().add(p);
                                if(!hab.getIdCollection().contains(p.getId())){
                                    hab.getIdCollection().add(p.getId());
                                }
                                hab.getBornCollection().put(p.getId(), loadedBornTimes.getOrDefault(p.getId(),0));

                                final Person finalP = p;
                                Platform.runLater(() -> {
                                    finalP.recreateImageView(finalP.getCurrentX(), finalP.getCurrentY());
                                    if (finalP.getImageView() != null && visualizationPane != null) {
                                        visualizationPane.getChildren().add(finalP.getImageView());
                                    }
                                });
                            }
                        }
                    }
                }

                Developer.setLifeTime(Integer.parseInt(fieldLifeTimeDev.getText()));
                Manager.setLifeTime(Integer.parseInt(fieldLifeTimeMan.getText()));

                showInfo("Загрузка из БД", "Данные типа '" + loadType.name() + "' из слота '" + slotName + "' загружены.\nВремя сброшено. Нажмите 'Старт'.");
            } else {
                showError("Ошибка загрузки", "Не удалось загрузить данные для слота '" + slotName + "'.");
            }
        } catch (SQLException e) {
            showError("Ошибка загрузки из БД", "Не удалось загрузить: " + e.getMessage());
            e.printStackTrace();
            if(!combineLoad && (loadType == DatabaseManager.SaveType.ALL_PERSONS) ) {
                hab.clearObjects();
            } else if (!combineLoad && loadType == DatabaseManager.SaveType.DEVELOPERS_ONLY) {
                hab.removeAllDevelopers();
            } else if (!combineLoad && loadType == DatabaseManager.SaveType.MANAGERS_ONLY) {
                hab.removeAllManagers();
            }
        } catch (Exception e) {
            showError("Неожиданная ошибка", "Произошла ошибка при обработке загруженных данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Показывает ошибку.
     */
    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Показывает информационное сообщение.
     */
    private void showInfo(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
