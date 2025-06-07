package ru.nstu.laba1timp.controllers; // <<< ИСПРАВЛЕН ПАКЕТ

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.nstu.laba1timp.model.*;

import java.io.*;
import java.util.*;

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

    // --- Константы для сериализации состояния ---
    private static final String SAVE_FILE_EXTENSION = "*.sim";
    private static final String SAVE_FILE_DESCRIPTION = "Состояние симуляции";

    // --- Методы для работы с конфигурацией ---

    public static void saveConfig(Controller controller) {
        Properties props = new Properties();
        props.setProperty(PROP_SHOW_INFO, String.valueOf(controller.btnShowInfo.isSelected()));
        props.setProperty(PROP_SHOW_TIME, String.valueOf(controller.btnShowTime.isSelected()));
        props.setProperty(PROP_ENABLE_SOUND, String.valueOf(controller.btnEnableSound.isSelected()));
        props.setProperty(PROP_N1, controller.fieldN1.getText());
        props.setProperty(PROP_P1, controller.boxP1.getValue());
        props.setProperty(PROP_LIFE_TIME_DEV, controller.fieldLifeTimeDev.getText());
        props.setProperty(PROP_DEV_PRIORITY, String.valueOf(controller.boxDevPriority.getValue()));
        props.setProperty(PROP_N2, controller.fieldN2.getText());
        props.setProperty(PROP_P2, controller.boxP2.getValue());
        props.setProperty(PROP_LIFE_TIME_MAN, controller.fieldLifeTimeMan.getText());
        props.setProperty(PROP_MAX_MANAGER_PERCENT, controller.fieldMaxManagerPercent.getText());
        props.setProperty(PROP_MAN_PRIORITY, String.valueOf(controller.boxManPriority.getValue()));
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "Simulation Configuration");
            System.out.println("Конфигурация сохранена в " + CONFIG_FILE);
        } catch (IOException io) {
            System.err.println("Ошибка сохранения конфигурации: " + io.getMessage());
            showErrorDialog("Ошибка сохранения конфигурации", "Не удалось сохранить настройки в " + CONFIG_FILE);
        }
    }

    public static void loadConfig(Controller controller) {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            System.out.println("Файл конфигурации не найден. Используются значения по умолчанию.");
            return;
        }
        try (InputStream input = new FileInputStream(configFile)) {
            props.load(input);
            controller.btnShowInfo.setSelected(Boolean.parseBoolean(props.getProperty(PROP_SHOW_INFO, "false")));
            controller.menuShowInfo.setSelected(controller.btnShowInfo.isSelected());
            boolean showTime = Boolean.parseBoolean(props.getProperty(PROP_SHOW_TIME, "true"));
            controller.btnShowTime.setSelected(showTime);
            controller.btnHideTime.setSelected(!showTime);
            controller.menuShowTime.setSelected(showTime);
            controller.menuHideTime.setSelected(!showTime);
            // Флаг timeFlag будет установлен в Controller.initialize после этого вызова
            // if (Statistics.getInstance() != null) { Statistics.getInstance().timeFlag = showTime; }
            boolean enableSound = Boolean.parseBoolean(props.getProperty(PROP_ENABLE_SOUND, "true"));
            controller.btnEnableSound.setSelected(enableSound);
            controller.btnDisableSound.setSelected(!enableSound);
            controller.menuEnableSound.setSelected(enableSound);
            controller.menuDisableSound.setSelected(!enableSound);
            if (Statistics.getInstance() != null) {
                Statistics.getInstance().setMusicEnabled(enableSound);
            }
            controller.fieldN1.setText(props.getProperty(PROP_N1, "1"));
            controller.boxP1.setValue(props.getProperty(PROP_P1, "80%"));
            controller.fieldLifeTimeDev.setText(props.getProperty(PROP_LIFE_TIME_DEV, "8"));
            try {
                int dP = Integer.parseInt(props.getProperty(PROP_DEV_PRIORITY, String.valueOf(Thread.NORM_PRIORITY)));
                if (dP >= 1 && dP <= 10 && controller.boxDevPriority.getItems().contains(dP))
                    controller.boxDevPriority.setValue(dP);
                else controller.boxDevPriority.setValue(5);
            } catch (NumberFormatException e) {
                controller.boxDevPriority.setValue(5);
            }
            controller.fieldN2.setText(props.getProperty(PROP_N2, "2"));
            controller.boxP2.setValue(props.getProperty(PROP_P2, "100%"));
            controller.fieldLifeTimeMan.setText(props.getProperty(PROP_LIFE_TIME_MAN, "10"));
            controller.fieldMaxManagerPercent.setText(props.getProperty(PROP_MAX_MANAGER_PERCENT, "40"));
            try {
                int mP = Integer.parseInt(props.getProperty(PROP_MAN_PRIORITY, String.valueOf(Thread.NORM_PRIORITY)));
                if (mP >= 1 && mP <= 10 && controller.boxManPriority.getItems().contains(mP))
                    controller.boxManPriority.setValue(mP);
                else controller.boxManPriority.setValue(5);
            } catch (NumberFormatException e) {
                controller.boxManPriority.setValue(5);
            }
            System.out.println("Конфигурация загружена из " + CONFIG_FILE);
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки конфигурации: " + ex.getMessage());
            showErrorDialog("Ошибка загрузки конфигурации", "Не удалось загрузить настройки.");
        } catch (Exception e) {
            System.err.println("Ошибка применения конфигурации: " + e.getMessage());
            showErrorDialog("Ошибка конфигурации", "Не удалось применить настройки.");
        }
    }

    // --- Методы для работы с состоянием ---

    public static void saveState(Controller controller, Habitat habitat, Statistics statistics) {
        boolean wasRunning = statistics.startFlag;
        boolean devAiWasActive = DeveloperAI.getInstance().isActive;
        boolean manAiWasActive = ManagerAI.getInstance().isActive;
        if (wasRunning) {
            statistics.stopAction(false, false);
            DeveloperAI.getInstance().isActive = false;
            ManagerAI.getInstance().isActive = false;
        }
        FileChooser fileChooser = createFileChooser(SAVE_FILE_DESCRIPTION, SAVE_FILE_EXTENSION);
        fileChooser.setTitle("Сохранить состояние симуляции");
        File file = fileChooser.showSaveDialog(getStage(controller));
        if (file == null) {
            System.out.println("Сохранение отменено.");
            if (wasRunning) { // Перезапускаем, если было запущено
                DeveloperAI.getInstance().isActive = devAiWasActive;
                ManagerAI.getInstance().isActive = manAiWasActive;
                statistics.startAction();
                // Восстанавливаем кнопки
                controller.btnStart.setDisable(true);
                controller.btnStop.setDisable(false);
                controller.menuStart.setDisable(true);
                controller.menuStop.setDisable(false);
                controller.btnDevIntellect.setText(devAiWasActive ? "ON" : "OFF");
                controller.btnManIntellect.setText(manAiWasActive ? "ON" : "OFF");
                controller.btnDevIntellect.setDisable(false);
                controller.btnManIntellect.setDisable(false);
            }
            return;
        }
        String filePath = file.getPath();
        if (!filePath.toLowerCase().endsWith(".sim")) {
            file = new File(filePath + ".sim");
        }
        try (FileOutputStream fos = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            LinkedList<Person> objCollection = habitat.getObjCollection();
            HashMap<Integer, Integer> bornCollection = habitat.getBornCollection();
            synchronized (objCollection) {
                synchronized (bornCollection) {
                    oos.writeInt(objCollection.size());
                    for (Person obj : objCollection) {
                        oos.writeObject(obj);
                        oos.writeInt(bornCollection.getOrDefault(obj.getId(), -1));
                    }
                }
            }
            oos.writeInt(Developer.spawnedCount);
            oos.writeInt(Manager.spawnedCount);
            oos.writeInt(statistics.getTime());
            System.out.println("Состояние симуляции сохранено в " + file.getAbsolutePath());
            showInfoDialog("Сохранение успешно", "Состояние симуляции сохранено в:\n" + file.getName());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения состояния симуляции: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Ошибка сохранения", "Не удалось сохранить состояние симуляции.\n" + e.getMessage());
        } finally {
            if (wasRunning) { // Перезапускаем, если было запущено
                DeveloperAI.getInstance().isActive = devAiWasActive;
                ManagerAI.getInstance().isActive = manAiWasActive;
                statistics.startAction();
                // Восстанавливаем кнопки
                controller.btnStart.setDisable(true);
                controller.btnStop.setDisable(false);
                controller.menuStart.setDisable(true);
                controller.menuStop.setDisable(false);
                controller.btnDevIntellect.setText(devAiWasActive ? "ON" : "OFF");
                controller.btnManIntellect.setText(manAiWasActive ? "ON" : "OFF");
                controller.btnDevIntellect.setDisable(false);
                controller.btnManIntellect.setDisable(false);
            }
        }
    }

    public static void loadState(Controller controller, Habitat habitat, Statistics statistics) {
        // 1. Останавливаем текущую симуляцию
        if (statistics.startFlag) {
            statistics.stopAction(false); // Без диалога, НЕ устанавливает restartFlag
            DeveloperAI.getInstance().isActive = false;
            ManagerAI.getInstance().isActive = false;
        }
        // <<< СБРАСЫВАЕМ restartFlag ЯВНО >>>
        statistics.restartFlag = false;

        // 2. Обновляем UI
        Platform.runLater(() -> {
            controller.btnStart.setDisable(false);
            controller.btnStop.setDisable(true);
            controller.menuStart.setDisable(false);
            controller.menuStop.setDisable(true);
            controller.btnDevIntellect.setDisable(true);
            controller.btnManIntellect.setDisable(true);
            controller.btnDevIntellect.setText("ON"); // Сброс текста кнопок
            controller.btnManIntellect.setText("ON");
        });

        // 3. Выбираем файл
        FileChooser fileChooser = createFileChooser(SAVE_FILE_DESCRIPTION, SAVE_FILE_EXTENSION);
        fileChooser.setTitle("Загрузить состояние симуляции");
        File file = fileChooser.showOpenDialog(getStage(controller));
        if (file == null) {
            System.out.println("Загрузка отменена.");
            return;
        }

        // 4. Очищаем текущее состояние
        habitat.clearObjects();
        Platform.runLater(() -> controller.getPane().getChildren().clear());

        // 5. Загружаем данные
        try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
            LinkedList<Person> objCollection = habitat.getObjCollection();
            HashMap<Integer, Integer> bornCollection = habitat.getBornCollection();
            TreeSet<Integer> idCollection = habitat.getIdCollection();
            int objectCount = ois.readInt();
            List<Person> loadedPersons = new ArrayList<>(objectCount);
            Map<Integer, Integer> loadedBirthTimes = new HashMap<>();
            for (int i = 0; i < objectCount; i++) {
                Person obj = (Person) ois.readObject();
                int bornTime = ois.readInt();
                loadedPersons.add(obj);
                loadedBirthTimes.put(obj.getId(), bornTime);
            }
            synchronized (objCollection) {
                synchronized (bornCollection) {
                    synchronized (idCollection) {
                        for (Person obj : loadedPersons) {
                            if (!idCollection.contains(obj.getId())) {
                                objCollection.add(obj);
                                bornCollection.put(obj.getId(), loadedBirthTimes.get(obj.getId()));
                                idCollection.add(obj.getId());
                                Platform.runLater(() -> {
                                    obj.recreateImageView(obj.getCurrentX(), obj.getCurrentY());
                                    if (obj.getImageView() != null) {
                                        controller.getPane().getChildren().add(obj.getImageView());
                                    } else {
                                        System.err.println("ImageView null для ID: " + obj.getId());
                                    }
                                });
                            } else {
                                System.err.println("ID " + obj.getId() + " уже существует.");
                            }
                        }
                    }
                }
            }
            Developer.spawnedCount = ois.readInt();
            Manager.spawnedCount = ois.readInt();
            int loadedTime = ois.readInt();
            statistics.setTimeFromLoad(loadedTime); // Установка времени

            // Установка времени жизни
            try {
                Developer.setLifeTime(Integer.parseInt(controller.fieldLifeTimeDev.getText()));
                Manager.setLifeTime(Integer.parseInt(controller.fieldLifeTimeMan.getText()));
            } catch (NumberFormatException e) {
                System.err.println("Ошибка парсинга времени жизни при загрузке.");
                Developer.setLifeTime(8);
                Manager.setLifeTime(10);
            }

            System.out.println("Состояние загружено: " + file.getAbsolutePath());
            showInfoDialog("Загрузка успешна", "Состояние загружено:\n" + file.getName() + "\nНажмите Старт для продолжения.");
        } catch (FileNotFoundException e) {
            showErrorDialog("Ошибка загрузки", "Файл не найден:\n" + file.getName());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка загрузки состояния: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Ошибка загрузки", "Не удалось загрузить состояние.\n" + e.getMessage());
            habitat.clearObjects();
            Platform.runLater(() -> controller.getPane().getChildren().clear());
            statistics.setTimeFromLoad(-1); // Сброс времени
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при загрузке: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Ошибка загрузки", "Непредвиденная ошибка.");
            habitat.clearObjects();
            Platform.runLater(() -> controller.getPane().getChildren().clear());
            statistics.setTimeFromLoad(-1); // Сброс времени
        }
    }

    // --- Вспомогательные методы ---
    private static FileChooser createFileChooser(String description, String extension) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extension));
        return fc;
    }

    private static void showErrorDialog(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private static void showInfoDialog(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // Вспомогательный метод для получения Stage из контроллера
    private static Stage getStage(Controller controller) {
        if (controller != null && controller.getPane() != null && controller.getPane().getScene() != null) {
            return (Stage) controller.getPane().getScene().getWindow();
        }
        return null; // Возвращаем null, если окно недоступно
    }
}