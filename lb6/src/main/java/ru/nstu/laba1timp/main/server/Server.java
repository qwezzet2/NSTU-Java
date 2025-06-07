package ru.nstu.laba1timp.main.server;

import ru.nstu.laba1timp.dto.*; // Импортируем DTO из твоего клиентского проекта

import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.*; // List, Map, Collections, ArrayList, HashMap, Scanner, Timer, TimerTask
import java.util.concurrent.*; // ExecutorService, Executors, TimeUnit

/**
 * Главный класс TCP-сервера для симуляции.
 * Ожидает подключения клиентов, управляет списком пользователей,
 * рассылает уведомления, пересылает сообщения обмена и регистрируется на RegistryServer.
 * Использует НЕСТАТИЧЕСКИЕ методы и коллекции для управления клиентами этого экземпляра сервера.
 */
public class Server {
    // --- Константы ---
    private static final int DEFAULT_PORT = 8030; // Порт по умолчанию для игрового сервера
    private static final int MAX_PORT_ATTEMPTS = 10; // Макс. попыток найти порт
    private static final String REGISTRY_HOST = "192.168.0.18"; // Адрес регистратора <<<< ИЗМЕНЕНО
    private static final int REGISTRY_PORT = 8029;         // Порт регистратора
    private static final long PING_INTERVAL_MS = 15000;   // Интервал PING (15 сек)
    private static final int REGISTRY_TIMEOUT_MS = 3000;   // Таймаут подключения к регистратору (увеличено)

    // --- Поля Экземпляра (НЕ static) ---
    private ServerSocket serverSocket;
    private final ExecutorService clientExecutor; // Пул потоков для обработчиков
    private volatile boolean running = true;     // Флаг работы сервера
    private int port;                            // Актуальный порт этого экземпляра сервера
    private String serverAddressForRegistry;     // Адрес этого сервера для регистрации (ip:port)
    private Timer pingTimer;                     // Таймер для отправки PING регистратору
    // Коллекции НЕ СТАТИЧЕСКИЕ, но потокобезопасные
    private final List<String> userList = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, ObjectOutputStream> oosOfEveryUser = Collections.synchronizedMap(new HashMap<>());

    /**
     * Конструктор сервера. Инициализирует пул потоков.
     */
    public Server() {
        clientExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Пытается определить IP-адрес хоста в локальной сети.
     * @return Строка с IP-адресом или "localhost", если определить не удалось.
     */
    private String getLocalLanIp() {
        try {
            // Пробуем получить IP через NetworkInterface (предпочтительно для LAN)
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Исключаем loopback, виртуальные и неактивные интерфейсы
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Ищем IPv4 адрес, который не является loopback и является site-local
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        System.out.println("Определен LAN IP: " + addr.getHostAddress());
                        return addr.getHostAddress();
                    }
                }
            }
            // Если не нашли site-local, пробуем getHostAddress() у InetAddress.getLocalHost()
            // Это может вернуть 127.0.0.1, если hostname разрешается в loopback
            String hostIp = InetAddress.getLocalHost().getHostAddress();
            if (!hostIp.equals("127.0.0.1") && !hostIp.equals("localhost")) {
                System.out.println("Определен IP через getLocalHost(): " + hostIp);
                return hostIp;
            }
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Ошибка определения LAN IP: " + e.getMessage());
        }
        System.out.println("Не удалось определить LAN IP, используется 'localhost'");
        return "localhost"; // Fallback
    }


    /** Запускает поток для команды 'stop'. */
    private void startConsoleListener() {
        Thread consoleThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Сервер (Порт: " + this.port + ", Адрес для регистрации: " + this.serverAddressForRegistry + ") ожидает команды 'stop' для остановки.");
                while (running) {
                    try {
                        if (scanner.hasNextLine()) {
                            if ("stop".equalsIgnoreCase(scanner.nextLine())) {
                                shutdownServer(); break;
                            }
                        } else {
                            try { Thread.sleep(500); } catch (InterruptedException interruptedException) { Thread.currentThread().interrupt(); break;}
                        }
                    } catch (Exception e) {
                        if (running) System.err.println("Ошибка чтения консоли (Порт: " + this.port + "): " + e.getMessage());
                        running = false;
                        shutdownServer();
                        break;
                    }
                }
            } catch (Exception e) {
                if (running) System.err.println("Критическая ошибка инициализации консоли: " + e.getMessage());
                running = false; shutdownServer();
            } finally {
                System.out.println("Поток консоли (Порт: " + this.port + ") завершен.");
            }
        }, "ServerConsoleListener-" + this.port);
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    /**
     * Пытается запустить сервер, начиная с заданного порта,
     * регистрируется на RegistryServer и запускает цикл приема подключений.
     * @param startPort Начальный порт для попытки запуска.
     * @return true, если сервер успешно запущен, иначе false.
     */
    public boolean launch(int startPort) {
        int currentPort = startPort;
        String lanIp = getLocalLanIp(); // Определяем IP один раз

        for (int i = 0; i < MAX_PORT_ATTEMPTS; i++) {
            try {
                serverSocket = new ServerSocket(currentPort);
                this.port = currentPort;
                this.serverAddressForRegistry = lanIp + ":" + this.port; // Используем LAN IP

                running = true;
                System.out.println("Сервер УСПЕШНО запущен на порту: " + this.port + " (будет регистрироваться как " + this.serverAddressForRegistry + ")");

                if (registerWithRegistry()) {
                    System.out.println("Успешно зарегистрирован на Registry Server как " + this.serverAddressForRegistry);
                    startPingTask();
                } else {
                    System.err.println("ПРЕДУПРЕЖДЕНИЕ: Не удалось зарегистрироваться на Registry Server. Сервер будет работать, но клиенты не смогут его обнаружить автоматически.");
                }

                Thread acceptThread = new Thread(this::acceptConnectionsLoop, "ServerAcceptThread-" + this.port);
                acceptThread.start();
                startConsoleListener();
                return true;

            } catch (BindException e) {
                System.err.println("Порт " + currentPort + " занят. Пробуем следующий...");
                currentPort++;
            } catch (IOException e) {
                System.err.println("КРИТИЧЕСКАЯ ОШИБКА: Не удалось запустить сервер на порту " + currentPort + ": " + e.getMessage());
                running = false; return false;
            }
        }
        System.err.println("Не удалось найти свободный порт в диапазоне [" + startPort + "..." + (currentPort - 1) + "]");
        running = false; return false;
    }

    /** Основной цикл приема подключений клиентов. */
    private void acceptConnectionsLoop() {
        System.out.println("Сервер (Порт: " + this.port + ") начал прием подключений.");
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Клиент подключен: " + clientSocket.getInetAddress() + " к серверу на порту " + this.port);
                ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                ClientHandler handler = new ClientHandler(clientSocket, ois, oos, this);
                clientExecutor.submit(handler);
            } catch (SocketException e) {
                if (running) System.err.println("Ошибка сокета при приеме (сервер порт " + this.port + "): " + e.getMessage());
            } catch (IOException e) {
                if (running) System.err.println("Ошибка I/O при подключении клиента (сервер порт " + this.port + "): " + e.getMessage());
            }
        }
        System.out.println("Цикл приема подключений сервера (порт " + this.port + ") завершен.");
    }

    // --- Методы управления клиентами (НЕ СТАТИЧЕСКИЕ) ---
    /**
     * Добавляет нового клиента в систему этого сервера.

     * @throws IllegalArgumentException Если имя пользователя некорректно или уже занято.
     */
    public synchronized void addUserToList(String userName, ObjectOutputStream userOos) throws IllegalArgumentException {
        if (userName == null || userName.trim().isEmpty() || oosOfEveryUser.containsKey(userName)) {
            try { if (userOos != null) userOos.close(); } catch (IOException e) {/* ignore */}
            throw new IllegalArgumentException("Недопустимое или дублирующееся имя пользователя: " + userName);
        }
        userList.add(userName);
        oosOfEveryUser.put(userName, userOos);
        System.out.println("addUserToList: Пользователь добавлен '" + userName + "' (Всего: " + userList.size() + " на сервере " + this.serverAddressForRegistry + ")");
        try {
            sendUserListToOne(userOos, userName);
        } catch(IOException e){
            System.err.println("addUserToList: Ошибка отправки списка новому клиенту " + userName + " на сервере " + this.serverAddressForRegistry + ": " + e.getMessage());
            removeUserFromList(userName, userOos);
            throw new IllegalArgumentException("Ошибка отправки начального списка пользователю " + userName);
        }
        broadcastUserStatus(userName, true);
    }

    /**
     * Удаляет клиента из системы этого сервера.
     * @param userName Имя удаляемого клиента.
     * @param userOos Поток вывода удаляемого клиента (для информации, закрывается в Handler).
     */
    public synchronized void removeUserFromList(String userName, ObjectOutputStream userOos) {
        if (userName == null) return;
        ObjectOutputStream removedOos = oosOfEveryUser.remove(userName);
        boolean removed = userList.remove(userName);
        if (removed) {
            System.out.println("removeUserFromList: Пользователь удален '" + userName + "' (Всего: " + userList.size() + " на сервере " + this.serverAddressForRegistry + ")");
            broadcastUserStatus(userName, false);
        } else {
            System.err.println("removeUserFromList: Попытка удалить несуществующего пользователя: " + userName + " на сервере " + this.serverAddressForRegistry);
        }
    }

    // --- Методы рассылки (НЕ СТАТИЧЕСКИЕ) ---
    /** Отправляет список пользователей одному клиенту. */
    private synchronized void sendUserListToOne(ObjectOutputStream targetOos, String targetUsername) throws IOException {
        List<String> listToSend = new ArrayList<>(userList.size());
        for(String name : userList) {
            if(name.equals(targetUsername)) { listToSend.add(name + " (You)"); }
            else { listToSend.add(name); }
        }
        UserlistDTO userlistDto = new UserlistDTO(listToSend.toArray(new String[0]));
        try {
            synchronized(targetOos) {
                targetOos.reset();
                targetOos.writeObject(userlistDto);
                targetOos.flush();
            }
            System.out.println("sendUserListToOne: Список отправлен к " + targetUsername + " с сервера " + this.serverAddressForRegistry);
        } catch (IOException e) {
            System.err.println("sendUserListToOne: Ошибка отправки к " + targetUsername + " с сервера " + this.serverAddressForRegistry + ": " + e.getMessage());
            throw e;
        }
    }

    /** Рассылает статус подключения/отключения всем остальным клиентам этого сервера. */
    private synchronized void broadcastUserStatus(String changedUsername, boolean joined) {
        if (changedUsername == null) return;
        UsernameDTO statusDto = new UsernameDTO(changedUsername, joined);
        String action = joined ? "присоединился" : "отключился";
        System.out.println("broadcastUserStatus: Рассылка статуса '" + changedUsername + " " + action + "' от сервера " + this.serverAddressForRegistry);
        List<Map.Entry<String, ObjectOutputStream>> currentClients = new ArrayList<>(oosOfEveryUser.entrySet());
        for (Map.Entry<String, ObjectOutputStream> entry : currentClients) {
            String username = entry.getKey(); ObjectOutputStream oos = entry.getValue();
            if (!username.equals(changedUsername)) {
                try {
                    synchronized(oos) {
                        oos.reset();
                        oos.writeObject(statusDto);
                        oos.flush();
                    }
                } catch (IOException e) { System.err.println("broadcastUserStatus: Ошибка отправки статуса клиенту " + username + " от сервера " + this.serverAddressForRegistry + ": " + e.getMessage()); }
            }
        }
    }

    // --- Методы для обмена (НЕ СТАТИЧЕСКИЕ) ---
    /** Пересылает запрос на обмен указанному клиенту, подключенному к этому серверу. */
    public synchronized void forwardExchangeRequest(ObjectExchangeRequestDTO requestDto) {
        String targetUser = requestDto.getTargetUser();
        ObjectOutputStream targetOos = oosOfEveryUser.get(targetUser);
        if (targetOos != null) {
            try {
                synchronized(targetOos){
                    targetOos.reset();
                    targetOos.writeObject(requestDto);
                    targetOos.flush();
                }
                System.out.println("Переслан запрос обмена от " + requestDto.getSourceUser() + " к " + targetUser + " (сервер " + this.serverAddressForRegistry + ")");
            } catch (IOException e) { System.err.println("Ошибка пересылки запроса обмена к " + targetUser + " на сервере " + this.serverAddressForRegistry + ": " + e.getMessage()); }
        } else { System.err.println("Не удалось переслать запрос обмена: пользователь " + targetUser + " не найден на сервере " + this.serverAddressForRegistry + "."); }
    }
    /** Пересылает данные обмена указанному клиенту, подключенному к этому серверу. */
    public synchronized void forwardExchangeData(ObjectExchangeDataDTO dataDto) {
        String targetUser = dataDto.getTargetUser();
        ObjectOutputStream targetOos = oosOfEveryUser.get(targetUser);
        if (targetOos != null) {
            try {
                synchronized(targetOos){
                    targetOos.reset();
                    targetOos.writeObject(dataDto);
                    targetOos.flush();
                }
                System.out.println("Пересланы данные обмена от " + dataDto.getSourceUser() + " к " + targetUser + " (сервер " + this.serverAddressForRegistry + ")");
            } catch (IOException e) { System.err.println("Ошибка пересылки данных обмена к " + targetUser + " на сервере " + this.serverAddressForRegistry + ": " + e.getMessage()); }
        } else { System.err.println("Не удалось переслать данные обмена: пользователь " + targetUser + " не найден на сервере " + this.serverAddressForRegistry + "."); }
    }

    // --- Методы для Registry Server ---
    /** Отправляет запрос на регистрацию серверу RegistryServer. */
    private boolean registerWithRegistry() {
        System.out.println("Регистрация на Registry Server (" + REGISTRY_HOST + ":" + REGISTRY_PORT + ") как " + this.serverAddressForRegistry);
        return sendRegistryCommand("REGISTER:" + this.serverAddressForRegistry);
    }
    /** Отправляет запрос на разрегистрацию серверу RegistryServer. */
    private void unregisterFromRegistry() {
        System.out.println("Разрегистрация с Registry Server (" + this.serverAddressForRegistry + ")");
        sendRegistryCommand("UNREGISTER:" + this.serverAddressForRegistry);
    }
    /** Запускает периодическую отправку PING на RegistryServer. */
    private void startPingTask() {
        if (pingTimer != null) { pingTimer.cancel(); }
        pingTimer = new Timer("ServerPingTimer-" + this.port, true);
        pingTimer.schedule(new TimerTask() {
            @Override public void run() { if (!running) { cancel(); return; } sendPingToRegistry(); }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS);
        System.out.println("Задача PING для Registry Server (" + this.serverAddressForRegistry + ") запущена.");
    }
    /** Отправляет PING на RegistryServer. */
    private void sendPingToRegistry() {
        sendRegistryCommand("PING:" + this.serverAddressForRegistry);
    }
    /** Отправляет команду Registry серверу и проверяет ответ. */
    private boolean sendRegistryCommand(String command) {
        try (Socket registrySocket = new Socket()) {
            registrySocket.connect(new InetSocketAddress(REGISTRY_HOST, REGISTRY_PORT), REGISTRY_TIMEOUT_MS);
            try (PrintWriter writer = new PrintWriter(registrySocket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(registrySocket.getInputStream())))
            {
                writer.println(command); String response = reader.readLine();
                System.out.println("Ответ Registry на [" + command + "] (для " + this.serverAddressForRegistry + "): " + response);
                if (command.startsWith("REGISTER") || command.startsWith("UNREGISTER")) return response != null && response.startsWith("OK");
                else if (command.startsWith("PING")) return response != null && response.startsWith("PONG");
                return false;
            }
        } catch (IOException e) { System.err.println("Ошибка при отправке команды ["+command+"] на Registry Server (для " + this.serverAddressForRegistry + "): " + e.getMessage()); return false; }
    }

    // --- Остановка сервера ---
    /** Корректно останавливает сервер. */
    private void shutdownServer() {
        if (!running) return; running = false; System.out.println("Остановка сервера на порту " + this.port + " (адрес " + this.serverAddressForRegistry + ")...");
        unregisterFromRegistry();
        if (pingTimer != null) { pingTimer.cancel(); pingTimer = null; }
        if (serverSocket != null && !serverSocket.isClosed()) { try { serverSocket.close(); System.out.println("Серверный сокет (порт " + this.port + ") закрыт."); } catch (IOException e) { /*...*/ } }
        synchronized (userList) { synchronized (oosOfEveryUser) {
            System.out.println("Закрытие " + oosOfEveryUser.size() + " клиентских соединений для сервера " + this.serverAddressForRegistry + "...");
            List<ObjectOutputStream> streamsToClose = new ArrayList<>(oosOfEveryUser.values());
            for (ObjectOutputStream oos : streamsToClose) { try { oos.close(); } catch (IOException e) {/*...*/} }
            oosOfEveryUser.clear(); userList.clear();
        }
        }
        clientExecutor.shutdown(); try { if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) { clientExecutor.shutdownNow(); } } catch (InterruptedException e) { clientExecutor.shutdownNow(); Thread.currentThread().interrupt(); }
        System.out.println("Сервер на порту " + this.port + " (адрес " + this.serverAddressForRegistry + ") полностью остановлен.");
    }


    // --- Точка входа ---
    public static void main(String[] args) {
        int startPort = DEFAULT_PORT;
        if (args.length > 0) { try { startPort = Integer.parseInt(args[0]); if (startPort < 1 || startPort > 65535) throw new NumberFormatException(); System.out.println("Начальный порт из аргументов: " + startPort); } catch (NumberFormatException e) { System.err.println("Неверный порт '" + args[0] + "'. Используется " + DEFAULT_PORT); startPort = DEFAULT_PORT; } }
        else { System.out.println("Используется порт по умолчанию: " + DEFAULT_PORT); }
        Server server = new Server();
        boolean launched = server.launch(startPort);
        if (!launched) { System.err.println("Не удалось запустить сервер!"); System.exit(1); }
        // Не выводим "Метод main сервера завершен", т.к. сервер продолжает работать в других потоках.
        // Консольный поток теперь тоже daemon, так что main может завершиться,
        // но JVM не закроется пока есть не-daemon потоки (например, AcceptThread).
    }
}