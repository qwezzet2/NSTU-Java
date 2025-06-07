package ru.nstu.laba1timp.api;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.nstu.laba1timp.controllers.Controller;
import ru.nstu.laba1timp.controllers.Habitat;
import ru.nstu.laba1timp.controllers.Statistics;
import ru.nstu.laba1timp.dto.*;
import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Client {
    private static String userName;
    private static Socket socket;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static ServerListener listener;
    private static volatile Controller controller; // Ссылка на контроллер UI (остается приватным)

    // --- Константы для Registry ---
    private static final String REGISTRY_HOST = "192.168.56.1"; // Используйте ваш актуальный IP или localhost для теста
    private static final int REGISTRY_PORT = 8029;
    private static final int REGISTRY_TIMEOUT_MS = 3000;

    // === НОВЫЕ ПОЛЯ для отложенного обновления UI ===
    private static volatile boolean controllerReady = false; // Флаг готовности контроллера
    // Потокобезопасное хранилище для списка, полученного до готовности контроллера
    private static final AtomicReference<List<String>> pendingUserList = new AtomicReference<>(null);

    // Приватный конструктор
    private Client() {}

    /**
     * Устанавливает ссылку на контроллер и обрабатывает ожидающий список пользователей.
     * Этот метод является единственной точкой для установки контроллера извне.
     */
    public static void setController(Controller mainController) {
        System.out.println("!!! Client: Установка контроллера: " + (mainController != null ? mainController.getClass().getSimpleName() : "null"));
        controller = mainController; // Присваиваем приватной статической переменной
        if (controller != null) {
            controllerReady = true; // Устанавливаем флаг готовности
            List<String> users = pendingUserList.getAndSet(null); // Получаем и очищаем атомно
            if (users != null) {
                System.out.println("!!! Client: Обработка отложенного списка пользователей: " + users);
                // Вызываем напрямую, так как мы уже в контексте установки контроллера
                controller.updateUserList(users); // Здесь controller уже не null
            }
        } else {
            controllerReady = false; // Сбрасываем флаг, если контроллер null
        }
    }

    public static String getUserName() {
        return userName;
    }

    /**
     * Показывает модальный диалог для ввода имени и выбора активного сервера.
     * @return true, если пользователь нажал "Подключиться" И подключение было УСПЕШНЫМ.
     *         false, если пользователь нажал "Работать оффлайн", закрыл диалог, или подключение не удалось.
     */
    public static boolean showLoginDialogAndAttemptConnect() {
        AtomicBoolean connectionSucceeded = new AtomicBoolean(false); // Флаг УСПЕШНОГО подключения
        AtomicBoolean dialogCompleted = new AtomicBoolean(false);

        System.out.println("!!! Client: Запрос списка активных серверов...");
        List<String> activeServersList = queryActiveServers();
        System.out.println("!!! Client: Получен список серверов: " + activeServersList);
        ObservableList<String> activeServersObservable = FXCollections.observableArrayList(activeServersList);

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Подключение к серверу");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        Label titleLabel = new Label("Вход и подключение");
        titleLabel.setFont(new Font("Arial Bold", 20));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        root.setTop(titleLabel);
        BorderPane.setMargin(titleLabel, new Insets(0, 0, 15, 0));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        TextField usernameField = new TextField("User" + (int) (Math.random() * 1000));
        usernameField.setPromptText("Ваше имя");

        ComboBox<String> serverComboBox = new ComboBox<>(activeServersObservable);
        serverComboBox.setPromptText("Выберите сервер");
        if (!activeServersObservable.isEmpty()) {
            serverComboBox.setValue(activeServersObservable.get(0));
        } else {
            serverComboBox.setPromptText("Серверы не найдены");
            serverComboBox.setDisable(true);
        }
        serverComboBox.setPrefWidth(200);

        grid.add(new Label("Имя:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Сервер:"), 0, 1);
        grid.add(serverComboBox, 1, 1);
        root.setCenter(grid);

        Button connectButton = new Button("Подключиться");
        connectButton.setDefaultButton(true);
        Button cancelButton = new Button("Работать оффлайн");
        HBox buttonBox = new HBox(10, cancelButton, connectButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        BorderPane.setAlignment(buttonBox, Pos.CENTER_RIGHT);
        root.setBottom(buttonBox);
        BorderPane.setMargin(buttonBox, new Insets(15, 0, 0, 0));
        connectButton.setDisable(activeServersObservable.isEmpty());

        connectButton.setOnAction(event -> {
            String name = usernameField.getText().trim();
            String selectedServer = serverComboBox.getValue();
            System.out.println("!!! Client: Нажата кнопка 'Подключиться'. Имя: " + name + ", Сервер: " + selectedServer);
            if (name.isEmpty() || selectedServer == null || selectedServer.equals("Серверы не найдены")) {
                showDialogError(dialogStage, "Ошибка ввода", "Введите имя и выберите активный сервер.");
                return;
            }
            String[] parts = selectedServer.split(":");
            if (parts.length != 2) {
                showDialogError(dialogStage, "Ошибка данных", "Неверный формат адреса сервера.");
                return;
            }
            String ip = parts[0];
            int port;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                showDialogError(dialogStage, "Ошибка данных", "Неверный порт.");
                return;
            }

            Client.userName = name; // Устанавливаем имя пользователя перед попыткой подключения
            System.out.println("!!! Client: Попытка подключения к " + ip + ":" + port + " с именем " + Client.userName);

            // Передаем null в качестве контроллера, т.к. главный контроллер еще не установлен в Client
            boolean success = connectToServer(ip, port, Client.userName, null);

            if (success) {
                connectionSucceeded.set(true);
                dialogCompleted.set(true);
                dialogStage.close();
            } else {
                // Ошибка уже была обработана и показана в connectToServer -> handleConnectionError
                // или будет показана сейчас, если connectToServer вернул false без показа диалога
                // showDialogError(dialogStage, "Ошибка подключения", "Не удалось подключиться к " + ip + ":" + port + ".\nПроверьте доступность сервера и настройки сети.");
                connectionSucceeded.set(false);
                // Диалог не закрываем, чтобы пользователь мог попробовать еще раз или выбрать оффлайн
            }
        });

        cancelButton.setOnAction(event -> {
            Client.userName = usernameField.getText().trim();
            if (Client.userName.isEmpty()) {
                Client.userName = "OfflineUser" + (int) (Math.random() * 100);
            }
            System.out.println("!!! Client: Пользователь выбрал 'Работать оффлайн'. Имя: " + Client.userName);
            connectionSucceeded.set(false);
            dialogCompleted.set(true);
            dialogStage.close();
        });

        dialogStage.setOnCloseRequest(event -> {
            if (!dialogCompleted.get()) {
                Client.userName = usernameField.getText().trim();
                if (Client.userName.isEmpty()) {
                    Client.userName = "OfflineUser" + (int) (Math.random() * 100);
                }
                System.out.println("!!! Client: Диалог входа закрыт крестиком. Имя: " + Client.userName + ". Режим оффлайн.");
                connectionSucceeded.set(false);
                dialogCompleted.set(true);
            }
        });

        dialogStage.setScene(new Scene(root));
        dialogStage.setResizable(false);
        Platform.runLater(usernameField::requestFocus);
        dialogStage.showAndWait();

        System.out.println("!!! Client: Диалог входа завершен. Подключение успешно: " + connectionSucceeded.get());
        return connectionSucceeded.get();
    }

    private static List<String> queryActiveServers() {
        List<String> serverList = new ArrayList<>();
        System.out.println("!!! Client.queryActiveServers: Подключение к Registry " + REGISTRY_HOST + ":" + REGISTRY_PORT);
        try (Socket registrySocket = new Socket()) {
            registrySocket.connect(new InetSocketAddress(REGISTRY_HOST, REGISTRY_PORT), REGISTRY_TIMEOUT_MS);
            System.out.println("!!! Client.queryActiveServers: Подключено к Registry.");
            try (PrintWriter writer = new PrintWriter(registrySocket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(registrySocket.getInputStream()))) {
                writer.println("GET_SERVERS");
                System.out.println("!!! Client.queryActiveServers: Отправлен GET_SERVERS.");
                String response = reader.readLine();
                System.out.println("!!! Client.queryActiveServers: Ответ от Registry: " + response);
                if (response != null && response.startsWith("SERVERS:")) {
                    String serverData = response.substring("SERVERS:".length());
                    if (!serverData.isEmpty()) {
                        serverList.addAll(Arrays.asList(serverData.split(",")));
                    }
                } else {
                    System.err.println("!!! Client.queryActiveServers: Некорректный ответ от Registry: " + response);
                }
            }
        } catch (IOException e) {
            System.err.println("!!! Client.queryActiveServers: Ошибка подключения к Registry: " + e.getMessage());
        }
        System.out.println("!!! Client.queryActiveServers: Возвращаемый список: " + serverList);
        return serverList;
    }

    private static void showDialogError(Window owner, String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static boolean connectToServer(String ip, int port, String name, Controller mainController) {
        if (isConnected()) {
            System.out.println("!!! Client.connectToServer: Уже подключен.");
            return true;
        }
        if (name != null && !name.isEmpty()) {
            userName = name; // Обновляем userName здесь, если передано новое
        }

        // Если mainController передан (например, при переподключении из меню),
        // и он не совпадает с текущим Client.controller, то вызываем setController.
        // Если mainController равен null (как при первом вызове из диалога),
        // то Client.controller не меняется (он будет установлен позже из MainApplication).
        if (mainController != null && Client.controller != mainController) {
            System.out.println("!!! Client.connectToServer: Устанавливается новый контроллер из аргумента: " + mainController.getClass().getSimpleName());
            setController(mainController); // Используем публичный сеттер
        } else if (mainController == null && Client.controller == null) {
            System.out.println("!!! Client.connectToServer: Вызван с mainController = null, и Client.controller также null.");
        }


        if (userName == null || userName.isEmpty()) {
            System.err.println("!!! Client.connectToServer: Ошибка - имя пользователя не установлено.");
            // Показываем ошибку, если контроллер уже установлен и готов
            if (controllerReady && Client.controller != null) {
                Platform.runLater(() -> Client.controller.showErrorDialog("Ошибка подключения", "Имя пользователя не установлено."));
            }
            return false;
        }

        try {
            System.out.println("!!! Client.connectToServer: Подключение к " + ip + ":" + port + "...");
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 5000); // 5 сек таймаут
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
            System.out.println("!!! Client.connectToServer: Соединение установлено.");
            listener = new ServerListener(ois);
            listener.start();
            System.out.println("!!! Client.connectToServer: Слушатель запущен.");
            UsernameDTO output = new UsernameDTO(userName);
            oos.writeObject(output);
            oos.flush();
            System.out.println("!!! Client.connectToServer: Имя '" + userName + "' отправлено.");
            // UI статус будет обновлен, если controllerReady и controller не null,
            // что произойдет после вызова setController из MainApplication или если он уже был установлен.
            updateConnectionStatusUI(true, "Подключен как " + userName);
            return true;
        } catch (ConnectException e) {
            handleConnectionError("Сервер недоступен.", ip + ":" + port + " (ConnectException)");
            return false;
        } catch (UnknownHostException e) {
            handleConnectionError("Неизвестный хост.", ip);
            return false;
        } catch (SocketException e){
            handleConnectionError("Ошибка сокета.", ip + ":" + port + " (" + e.getMessage() + ")");
            return false;
        }
        catch (IOException e) {
            handleConnectionError("Ошибка ввода/вывода.", ip + ":" + port + " (" + e.getMessage() + ")");
            // e.printStackTrace(); // Можно раскомментировать для детальной отладки
            return false;
        } catch (Exception e) {
            handleConnectionError("Непредвиденная ошибка.", ip + ":" + port + " (" + e.getMessage() + ")");
            // e.printStackTrace(); // Можно раскомментировать для детальной отладки
            return false;
        }
    }

    public static boolean connectToServer(String ip, int port) {
        // Этот метод вызывается из меню Controller, где Client.controller уже должен быть установлен
        return connectToServer(ip, port, userName, Client.controller);
    }

    private static void handleConnectionError(String baseMessage, String details) {
        System.err.println("!!! Client.handleConnectionError: " + baseMessage + " (" + details + ")");
        if (controllerReady && controller != null) {
            final String fullMessage = baseMessage + "\nДетали: " + details;
            Platform.runLater(() -> {
                if (controller != null) {
                    controller.showErrorDialog("Ошибка подключения", fullMessage);
                    // Обновляем статус только если ошибка произошла при попытке подключения,
                    // а не, например, при запросе списка серверов.
                    // В Client.connectToServer мы обновляем статус через updateConnectionStatusUI если controller установлен.
                    // Здесь можно убедиться, что статус обновлен на "ошибку" если он еще не такой.
                    if (isConnected()) { // Если было подключение и оно оборвалось
                        updateConnectionStatusUI(false, "Ошибка сети");
                    } else { // Если подключения не было
                        updateConnectionStatusUI(false, "Ошибка подключения");
                    }
                }
            });
        }
        closeResources();
    }

    public static void disconnectFromServer() {
        System.out.println("!!! Client.disconnectFromServer: Начало отключения...");
        // controllerReady сбрасывается в setController(null), если это нужно,
        // но при дисконнекте важнее остановить listener и закрыть ресурсы.
        // Флаг controllerReady в основном для отложенного обновления списка пользователей.
        // controllerReady = false; // Можно оставить, если хотим прервать любые попытки обновления UI из Client
        pendingUserList.set(null);
        if (listener != null) {
            listener.interrupt();
            listener = null;
        }
        closeResources();
        updateConnectionStatusUI(false, "Отключен");
        if (controllerReady && controller != null) {
            Platform.runLater(controller::clearUserList);
        }
        System.out.println("!!! Client.disconnectFromServer: Отключено.");
    }

    private static void closeResources() {
        try { if (ois != null) ois.close(); } catch (IOException e) { /* ignore */ }
        try { if (oos != null) oos.close(); } catch (IOException e) { /* ignore */ }
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) { /* ignore */ }
        ois = null; oos = null; socket = null;
    }

    public static boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    static void handleDisconnectError(Exception cause) {
        if (!isConnected() && listener == null) return; // Если уже отключены или listener не был запущен
        System.err.println("!!! Client.handleDisconnectError: Сетевая ошибка (" + cause.getClass().getSimpleName() + "). Отключение...");
        disconnectFromServer();
    }

    static void handleUserList(UserlistDTO dto) {
        System.out.println("!!! Client.handleUserList: Получен список из DTO: " + Arrays.toString(dto.getUserList()));
        if (dto == null || dto.getUserList() == null) {
            System.err.println("!!! Client.handleUserList: DTO null.");
            return;
        }
        List<String> usersCopy = new ArrayList<>(Arrays.asList(dto.getUserList()));
        if (controllerReady && controller != null) {
            System.out.println("!!! Client.handleUserList: Контроллер готов, обновляем UI.");
            Platform.runLater(() -> {
                if (controller != null) controller.updateUserList(usersCopy);
            });
        } else {
            System.out.println("!!! Client.handleUserList: Контроллер НЕ готов, сохраняем список.");
            pendingUserList.set(usersCopy);
        }
    }

    static void handleUserUpdate(UsernameDTO dto) {
        System.out.println("!!! Client.handleUserUpdate: Получен статус для " + dto.getUserName() + ": " + dto.getStatus());
        if (controllerReady && controller != null && dto != null && dto.getUserName() != null) {
            final String name = dto.getUserName();
            final boolean status = dto.getStatus();
            Platform.runLater(() -> {
                if (controller != null) {
                    if (status) controller.addUserToList(name);
                    else controller.removeUserFromList(name);
                }
            });
        } else {
            System.out.println("!!! Client.handleUserUpdate: Контроллер НЕ готов или DTO null, обновление статуса пропущено.");
        }
    }

    public static void updateConnectionStatusUI(boolean connected, String message) {
        if (controllerReady && controller != null) {
            Platform.runLater(() -> {
                if (controller != null) controller.updateConnectionStatus(connected, message);
            });
        } else {
            System.out.println("!!! Client.updateConnectionStatusUI: Контроллер не готов или null. Статус: " + connected + ", Сообщение: " + message);
        }
    }
    // --- МЕТОДЫ ДЛЯ ОБМЕНА ОБЪЕКТАМИ ---
    public static void sendObjectExchangeRequest(String targetUser, int typeToReceive) {
        if (!checkConnectionAndNotify("отправить запрос на обмен")) return;
        if (!validateTargetUser(targetUser)) return;
        try {
            ObjectExchangeRequestDTO request = new ObjectExchangeRequestDTO(userName, targetUser, typeToReceive);
            oos.reset(); oos.writeObject(request); oos.flush(); System.out.println("API Client: Запрос на обмен (получить тип " + typeToReceive + ") отправлен к " + targetUser);
            if (controllerReady && controller != null) { Platform.runLater(() -> controller.showInfoDialog("Запрос отправлен", "Запрос на обмен отправлен пользователю " + targetUser)); }
        } catch (IOException e) { handleSendError("отправки запроса на обмен", e); }
    }

    private static void sendObjectData(String targetUser, List<Person> persons, Map<Integer, Integer> birthTimes, int typeSent) {
        if (!checkConnectionAndNotify("отправить данные обмена")) return;
        try { ObjectExchangeDataDTO dataDto = new ObjectExchangeDataDTO(userName, targetUser, persons, birthTimes, typeSent);
            oos.reset(); oos.writeObject(dataDto); oos.flush(); System.out.println("API Client: Данные обмена (" + persons.size() + " объектов типа " + typeSent + ") отправлены к " + targetUser);
        } catch (IOException e) { handleSendError("отправки данных обмена", e); }
    }

    static void handleExchangeRequest(ObjectExchangeRequestDTO request) {
        if (!controllerReady || controller == null || request == null) { System.err.println("API Client: Не могу обработать запрос на обмен - контроллер не готов/null или запрос null"); return; }
        System.out.println("!!! Client.handleExchangeRequest: Получен запрос от " + request.getSourceUser() + " на получение типа " + request.getRequestedTypeToReceive());
        Platform.runLater(() -> {
            String sender = request.getSourceUser(); int typeWeWillReceive = request.getTypeToSendFromSource(); int typeWeMustSend = request.getRequestedTypeToReceive();
            String typeReceiveStr = (typeWeWillReceive == 0) ? "Разработчиков" : "Менеджеров"; String typeSendStr = (typeWeMustSend == 0) ? "Разработчиков" : "Менеджеров";
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION); confirmation.setTitle("Запрос на обмен"); confirmation.setHeaderText("Запрос на обмен от " + sender);
            confirmation.setContentText("Принять обмен?\n\n" + "Вы ПОЛУЧИТЕ: их " + typeReceiveStr + ".\n" + "Вы ОТПРАВИТЕ: ваших " + typeSendStr + ".");
            try { if (controller.getPane() != null && controller.getPane().getScene() != null) confirmation.initOwner(controller.getPane().getScene().getWindow()); } catch (Exception e) {/*игнор*/}
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("API Client: Запрос на обмен от " + sender + " принят."); Habitat hab = Habitat.getInstance(); if (hab == null) { controller.showErrorDialog("Ошибка", "Habitat недоступен."); return; }
                Class<?> classToSend = (typeWeMustSend == 0) ? Developer.class : Manager.class; List<Person> objectsToSend; Map<Integer, Integer> timesToSend = new HashMap<>();
                synchronized (hab.getObjCollection()){ synchronized(hab.getBornCollection()){ objectsToSend = hab.getPersonsByType(classToSend); for(Person p : objectsToSend){ if(p != null) timesToSend.put(p.getId(), hab.getBornCollection().getOrDefault(p.getId(), -1)); } } }
                System.out.println("API Client: Подготовлено к отправке " + objectsToSend.size() + " объектов типа " + typeWeMustSend);
                sendObjectData(sender, objectsToSend, timesToSend, typeWeMustSend);
                System.out.println("API Client: Удаление отправленных объектов типа " + typeWeMustSend);
                hab.removePersons(objectsToSend);
                controller.showInfoDialog("Обмен", "Ваши " + typeSendStr + " отправлены.\nОжидание объектов от " + sender + "...");
            } else { System.out.println("API Client: Запрос на обмен от " + sender + " отклонен."); }
        });
    }

    static void handleExchangeData(ObjectExchangeDataDTO data) {
        if (!controllerReady || controller == null || data == null || data.getPersons() == null || data.getBirthTimes() == null) { System.err.println("API Client: Получен невалидный ObjectExchangeDataDTO или контроллер не готов"); return; }
        System.out.println("!!! Client.handleExchangeData: Получены данные от " + data.getSourceUser());
        Platform.runLater(() -> {
            String typeReceivedStr = (data.getTypeSent() == 0) ? "Разработчики" : "Менеджеры"; System.out.println("API Client: Обработка " + data.getPersons().size() + " " + typeReceivedStr);
            Habitat hab = Habitat.getInstance(); if (hab == null) { controller.showErrorDialog("Ошибка", "Habitat недоступен."); return; }
            boolean wasRunning = Statistics.getInstance() != null && Statistics.getInstance().startFlag; if (wasRunning) { Statistics.getInstance().stopAction(false); }
            hab.addPersons(data.getPersons(), data.getBirthTimes());
            if (wasRunning) { Statistics.getInstance().startAction(); }
            controller.showInfoDialog("Обмен", "Получено " + data.getPersons().size() + " " + typeReceivedStr + " от " + data.getSourceUser() + ".");
        });
    }

    private static boolean checkConnectionAndNotify(String action) {
        if (!isConnected() || oos == null) {
            if (controllerReady && controller != null) {
                Platform.runLater(() -> controller.showErrorDialog("Ошибка сети", "Невозможно " + action + ".\nНет подключения."));
            } else {
                System.err.println("Ошибка сети: Нет подключения для действия '" + action + "', контроллер не готов.");
            }
            return false;
        }
        return true;
    }
    private static boolean validateTargetUser(String targetUser){
        if (targetUser == null || targetUser.isEmpty() || targetUser.equals(userName) || targetUser.equals(Controller.NO_USER_SELECTED)) {
            System.err.println("Неверный целевой пользователь для обмена: '" + targetUser + "'");
            if(controllerReady && controller != null) Platform.runLater(() -> controller.showErrorDialog("Ошибка обмена", "Выберите другого пользователя для обмена."));
            return false;
        } return true;
    }
    private static void handleSendError(String action, IOException e) {
        System.err.println("Ошибка " + action + ": " + e.getMessage());
        if (controllerReady && controller != null) {
            Platform.runLater(() -> controller.showErrorDialog("Сетевая ошибка", "Не удалось " + action + ".\n" + e.getMessage()));
        }
        handleDisconnectError(e);
    }
}