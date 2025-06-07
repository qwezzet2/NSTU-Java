package ru.nstu.laba1timp.controllers;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView; // Добавлен импорт для ImageView
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.nstu.laba1timp.model.*;

import java.io.*;
import java.util.*; // Импортируем List, Map, Set, LinkedList, HashMap, ArrayList, Objects

/**
 * Класс FileMaster отвечает за сохранение и загрузку конфигурации приложения
 * и состояния симуляции. Он взаимодействует с Controller для доступа к элементам интерфейса
 * и с Habitat и Statistics для получения данных симуляции.
 */
public class FileMaster {

    // --- Константы для конфигурации ---
    private static final String CONFIG_FILE = "config.properties";
    private static final String PROP_SHOW_INFO = "ui.showInfo";
    private static final String PROP_SHOW_TIME = "ui.showTime";
    private static final String PROP_ENABLE_SOUND = "ui.enableSound";
    private static final String PROP_N1 = "dev.period";
    private static final String PROP_P1 = "dev.probability";
    private static final String PROP_LIFE_TIME_DEV = "dev.lifeTime";
    private static final String PROP_DEV_PRIORITY = "dev.ai.priority";
    private static final String PROP_N2 = "man.period";
    private static final String PROP_P2 = "man.probability";
    private static final String PROP_LIFE_TIME_MAN = "man.lifeTime";
    private static final String PROP_MAX_MANAGER_PERCENT = "man.maxPercent";
    private static final String PROP_MAN_PRIORITY = "man.ai.priority";

    // --- Константы для сохранения/загрузки состояния ---
    private static final String SAVE_FILE_EXTENSION = "*.sim";
    private static final String SAVE_FILE_DESCRIPTION = "Состояние симуляции";

    // --- Методы для работы с конфигурацией ---
    public static void saveConfig(Controller controller) {
        Properties props = new Properties();
        if (controller == null) { System.err.println("Ошибка сохранения конфига: Controller is null."); return; }
        try {
            if(controller.btnShowInfo != null) props.setProperty(PROP_SHOW_INFO, String.valueOf(controller.btnShowInfo.isSelected()));
            if(controller.btnShowTime != null) props.setProperty(PROP_SHOW_TIME, String.valueOf(controller.btnShowTime.isSelected()));
            if(controller.btnEnableSound != null) props.setProperty(PROP_ENABLE_SOUND, String.valueOf(controller.btnEnableSound.isSelected()));
            if(controller.fieldN1 != null) props.setProperty(PROP_N1, controller.fieldN1.getText());
            if(controller.boxP1 != null && controller.boxP1.getValue() != null) props.setProperty(PROP_P1, controller.boxP1.getValue());
            if(controller.fieldLifeTimeDev != null) props.setProperty(PROP_LIFE_TIME_DEV, controller.fieldLifeTimeDev.getText());
            if(controller.boxDevPriority != null && controller.boxDevPriority.getValue() != null) props.setProperty(PROP_DEV_PRIORITY, String.valueOf(controller.boxDevPriority.getValue()));
            if(controller.fieldN2 != null) props.setProperty(PROP_N2, controller.fieldN2.getText());
            if(controller.boxP2 != null && controller.boxP2.getValue() != null) props.setProperty(PROP_P2, controller.boxP2.getValue());
            if(controller.fieldLifeTimeMan != null) props.setProperty(PROP_LIFE_TIME_MAN, controller.fieldLifeTimeMan.getText());
            if(controller.fieldMaxManagerPercent != null) props.setProperty(PROP_MAX_MANAGER_PERCENT, controller.fieldMaxManagerPercent.getText());
            if(controller.boxManPriority != null && controller.boxManPriority.getValue() != null) props.setProperty(PROP_MAN_PRIORITY, String.valueOf(controller.boxManPriority.getValue()));

            try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
                props.store(output, "Simulation Configuration");
                System.out.println("Конфигурация сохранена в " + CONFIG_FILE);
            } catch (IOException io) {
                System.err.println("Ошибка сохранения конфигурации: " + io.getMessage());
                showErrorDialog("Ошибка сохранения конфигурации", "Не удалось сохранить настройки в " + CONFIG_FILE);
            }
        } catch (NullPointerException npe) {
            System.err.println("Ошибка сохранения конфигурации: Элемент интерфейса null."); npe.printStackTrace();
            showErrorDialog("Ошибка сохранения", "Не удалось сохранить конфигурацию: ошибка интерфейса.");
        }
    }

    public static void loadConfig(Controller controller) {
        if (controller == null) { System.err.println("Ошибка загрузки конфига: Controller is null."); return; }
        Properties props = new Properties(); File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) { System.out.println("Файл конфигурации '" + CONFIG_FILE + "' не найден."); return; }
        try (InputStream input = new FileInputStream(configFile)) {
            props.load(input);
            // --- Применение настроек с проверками на null ---
            if(controller.btnShowInfo != null && controller.menuShowInfo != null) { boolean v = Boolean.parseBoolean(props.getProperty(PROP_SHOW_INFO, "false")); controller.btnShowInfo.setSelected(v); controller.menuShowInfo.setSelected(v); }
            if(controller.btnShowTime != null && controller.btnHideTime != null && controller.menuShowTime != null && controller.menuHideTime != null) { boolean v = Boolean.parseBoolean(props.getProperty(PROP_SHOW_TIME, "true")); controller.btnShowTime.setSelected(v); controller.btnHideTime.setSelected(!v); controller.menuShowTime.setSelected(v); controller.menuHideTime.setSelected(!v); }
            if(controller.btnEnableSound != null && controller.btnDisableSound != null && controller.menuEnableSound != null && controller.menuDisableSound != null) { boolean v = Boolean.parseBoolean(props.getProperty(PROP_ENABLE_SOUND, "true")); controller.btnEnableSound.setSelected(v); controller.btnDisableSound.setSelected(!v); controller.menuEnableSound.setSelected(v); controller.menuDisableSound.setSelected(!v); if (Statistics.getInstance() != null) Statistics.getInstance().setMusicEnabled(v); }
            if(controller.fieldN1 != null) controller.fieldN1.setText(props.getProperty(PROP_N1, "1"));
            if(controller.boxP1 != null) controller.boxP1.setValue(props.getProperty(PROP_P1, "80%"));
            if(controller.fieldLifeTimeDev != null) controller.fieldLifeTimeDev.setText(props.getProperty(PROP_LIFE_TIME_DEV, "8"));
            if(controller.boxDevPriority != null && controller.boxDevPriority.getItems() != null) { try { int v = Integer.parseInt(props.getProperty(PROP_DEV_PRIORITY, String.valueOf(Thread.NORM_PRIORITY))); if (v >= Thread.MIN_PRIORITY && v <= Thread.MAX_PRIORITY && controller.boxDevPriority.getItems().contains(v)) controller.boxDevPriority.setValue(v); else controller.boxDevPriority.setValue(Thread.NORM_PRIORITY); } catch (NumberFormatException e) { controller.boxDevPriority.setValue(Thread.NORM_PRIORITY); } }
            if(controller.fieldN2 != null) controller.fieldN2.setText(props.getProperty(PROP_N2, "2"));
            if(controller.boxP2 != null) controller.boxP2.setValue(props.getProperty(PROP_P2, "100%"));
            if(controller.fieldLifeTimeMan != null) controller.fieldLifeTimeMan.setText(props.getProperty(PROP_LIFE_TIME_MAN, "10"));
            if(controller.fieldMaxManagerPercent != null) controller.fieldMaxManagerPercent.setText(props.getProperty(PROP_MAX_MANAGER_PERCENT, "40"));
            if(controller.boxManPriority != null && controller.boxManPriority.getItems() != null) { try { int v = Integer.parseInt(props.getProperty(PROP_MAN_PRIORITY, String.valueOf(Thread.NORM_PRIORITY))); if (v >= Thread.MIN_PRIORITY && v <= Thread.MAX_PRIORITY && controller.boxManPriority.getItems().contains(v)) controller.boxManPriority.setValue(v); else controller.boxManPriority.setValue(Thread.NORM_PRIORITY); } catch (NumberFormatException e) { controller.boxManPriority.setValue(Thread.NORM_PRIORITY); } }
            System.out.println("Конфигурация загружена из " + CONFIG_FILE);
        } catch (IOException ex) { System.err.println("Ошибка загрузки конфигурации: " + ex.getMessage()); showErrorDialog("Ошибка загрузки конфигурации", "Не удалось загрузить настройки.");
        } catch (Exception e) { System.err.println("Ошибка применения конфигурации: " + e.getMessage()); e.printStackTrace(); showErrorDialog("Ошибка конфигурации", "Не удалось применить настройки."); }
    }

    // --- Методы для работы с состоянием ---
    public static void saveState(Controller controller, Habitat habitat, Statistics statistics) {
        if (controller == null || habitat == null || statistics == null) { showErrorDialog("Ошибка сохранения", "Компонент не найден."); return; }
        boolean wasRunning = statistics.startFlag; boolean devAiWasActive = DeveloperAI.getInstance().isActive; boolean manAiWasActive = ManagerAI.getInstance().isActive;
        if (wasRunning) { statistics.stopAction(false); DeveloperAI.getInstance().isActive = false; ManagerAI.getInstance().isActive = false; }
        FileChooser fileChooser = createFileChooser(SAVE_FILE_DESCRIPTION, SAVE_FILE_EXTENSION); fileChooser.setTitle("Сохранить состояние симуляции"); File file = fileChooser.showSaveDialog(getStage(controller));
        if (file == null) { System.out.println("Сохранение отменено."); if (wasRunning) { /* ... перезапуск ... */ DeveloperAI.getInstance().isActive = devAiWasActive; ManagerAI.getInstance().isActive = manAiWasActive; statistics.startAction(); Platform.runLater(() -> { controller.btnStart.setDisable(true); /*...*/ }); } return; }
        String filePath = file.getPath(); if (!filePath.toLowerCase().endsWith(".sim")) { file = new File(filePath + ".sim"); }

        try (FileOutputStream fos = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            // === Используем LinkedList ===
            LinkedList<Person> objCollection = habitat.getObjCollection();
            // ===========================
            Map<Integer, Integer> bornCollection = habitat.getBornCollection();
            synchronized (habitat.getObjCollection()) { synchronized (habitat.getBornCollection()) {
                LinkedList<Person> objCopy = new LinkedList<>(objCollection); // Копируем в LinkedList
                Map<Integer, Integer> bornCopy = new HashMap<>(bornCollection);
                oos.writeInt(objCopy.size());
                for (Person obj : objCopy) { if(obj == null) continue; oos.writeObject(obj); oos.writeInt(bornCopy.getOrDefault(obj.getId(), -1)); }
            }
            }
            oos.writeInt(Developer.spawnedCount); oos.writeInt(Manager.spawnedCount); oos.writeInt(statistics.getTime());
            System.out.println("Состояние симуляции сохранено в " + file.getAbsolutePath()); showInfoDialog("Сохранение успешно", "Состояние симуляции сохранено в:\n" + file.getName());
        } catch (IOException e) { System.err.println("Ошибка сохранения состояния: " + e.getMessage()); e.printStackTrace(); showErrorDialog("Ошибка сохранения", "Не удалось сохранить состояние.\n" + e.getMessage());
        } finally { if (wasRunning) { /* ... перезапуск ... */ DeveloperAI.getInstance().isActive = devAiWasActive; ManagerAI.getInstance().isActive = manAiWasActive; statistics.startAction(); Platform.runLater(() -> { controller.btnStart.setDisable(true); /*...*/ }); } }
    }

    public static void loadState(Controller controller, Habitat habitat, Statistics statistics) {
        if (controller == null || habitat == null || statistics == null) { showErrorDialog("Ошибка загрузки", "Компонент не найден."); return; }
        if (statistics.startFlag) { statistics.stopAction(false); DeveloperAI.getInstance().isActive = false; ManagerAI.getInstance().isActive = false; }
        statistics.restartFlag = false;
        Platform.runLater(() -> { controller.setFieldsDisabled(false); controller.btnStart.setDisable(false); /*...*/ });
        FileChooser fileChooser = createFileChooser(SAVE_FILE_DESCRIPTION, SAVE_FILE_EXTENSION); fileChooser.setTitle("Загрузить состояние симуляции"); File file = fileChooser.showOpenDialog(getStage(controller));
        if (file == null) { System.out.println("Загрузка отменена."); return; }

        habitat.clearObjects(); // Очищаем перед загрузкой

        try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
            // Получаем ссылки на коллекции Habitat
            LinkedList<Person> objCollection = habitat.getObjCollection(); // <<<=== LinkedList
            Map<Integer, Integer> bornCollection = habitat.getBornCollection();
            Set<Integer> idCollection = habitat.getIdCollection();

            int objectCount = ois.readInt();
            List<Person> loadedPersons = new ArrayList<>(objectCount); // Временный список для объектов
            Map<Integer, Integer> loadedBirthTimes = new HashMap<>(); // Временная карта для времен

            // Читаем объекты во временные списки
            for (int i = 0; i < objectCount; i++) {
                try {
                    Person obj = (Person) ois.readObject(); int bornTime = ois.readInt();
                    if(obj != null) { loadedPersons.add(obj); loadedBirthTimes.put(obj.getId(), bornTime); }
                } catch (ClassNotFoundException | ClassCastException | IOException e) { System.err.println("Ошибка десериализации объекта: " + e.getMessage()); }
            }

            // Добавляем загруженные объекты в Habitat и UI
            if (!loadedPersons.isEmpty()) {
                // Добавляем в коллекции Habitat (синхронизировано внутри addPersons)
                habitat.addPersons(loadedPersons, loadedBirthTimes); // Используем существующий метод
            }

            Developer.spawnedCount = ois.readInt(); Manager.spawnedCount = ois.readInt(); int loadedTime = ois.readInt(); statistics.setTimeFromLoad(loadedTime);

            // Применяем время жизни из UI
            try { Developer.setLifeTime(Integer.parseInt(controller.fieldLifeTimeDev.getText())); Manager.setLifeTime(Integer.parseInt(controller.fieldLifeTimeMan.getText())); }
            catch (NumberFormatException e) { Developer.setLifeTime(8); Manager.setLifeTime(10); }

            System.out.println("Состояние загружено: " + file.getAbsolutePath()); showInfoDialog("Загрузка успешна", "Состояние загружено:\n" + file.getName() + "\nНажмите Старт для продолжения.");
        } catch (FileNotFoundException e) { showErrorDialog("Ошибка загрузки", "Файл не найден:\n" + file.getName()); habitat.clearObjects(); statistics.setTimeFromLoad(-1);
        } catch (IOException | ClassCastException e) { System.err.println("Ошибка загрузки состояния: " + e.getMessage()); e.printStackTrace(); showErrorDialog("Ошибка загрузки", "Не удалось загрузить состояние.\n" + e.getMessage()); habitat.clearObjects(); statistics.setTimeFromLoad(-1);
        } catch (Exception e) { System.err.println("Неожиданная ошибка при загрузке: " + e.getMessage()); e.printStackTrace(); showErrorDialog("Ошибка загрузки", "Непредвиденная ошибка."); habitat.clearObjects(); statistics.setTimeFromLoad(-1); }
    }

    // --- Вспомогательные методы ---
    private static FileChooser createFileChooser(String description, String extension) { FileChooser fc = new FileChooser(); fc.setInitialDirectory(new File(System.getProperty("user.dir"))); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extension)); return fc; }
    private static void showErrorDialog(String title, String content) { Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait(); }); }
    private static void showInfoDialog(String title, String content) { Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait(); }); }
    private static Stage getStage(Controller controller) { if (controller != null && controller.getPane() != null && controller.getPane().getScene() != null) { return (Stage) controller.getPane().getScene().getWindow(); } return null; }
}