package ru.nstu.laba1timp.main.server;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // Потокобезопасная карта
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Простой сервер регистрации для игровых серверов.
 * Хранит список активных серверов (IP:порт).
 */
public class RegistryServer {

    private static final int REGISTRY_PORT = 8029; // Фиксированный порт для регистратора
    private static final long SERVER_TIMEOUT_MS = 30000; // Таймаут неактивности сервера (30 сек)

    // Потокобезопасная карта: "ip:port" -> время последней активности (или регистрации)
    private final Map<String, Long> activeServers = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private final ExecutorService clientExecutor;
    private volatile boolean running = true;
    private Timer cleanupTimer; // Таймер для удаления неактивных серверов

    public RegistryServer() {
        clientExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Запускает сервер регистрации.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(REGISTRY_PORT);
            running = true;
            System.out.println("Registry Server запущен на порту: " + REGISTRY_PORT);

            startCleanupTask();

            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientExecutor.submit(() -> handleRegistryRequest(clientSocket));
                } catch (SocketException e) {
                    if (running) System.err.println("Registry: Ошибка сокета при приеме: " + e.getMessage());
                } catch (IOException e) {
                    if (running) System.err.println("Registry: Ошибка I/O при приеме: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Registry: КРИТИЧЕСКАЯ ОШИБКА: Не удалось запустить сервер регистрации на порту " + REGISTRY_PORT + ": " + e.getMessage());
            running = false;
        } finally {
            System.out.println("Registry Server: Цикл приема подключений завершен.");
            stopServer();
        }
    }

    /**
     * Обрабатывает запрос от клиента или игрового сервера.
     */
    private void handleRegistryRequest(Socket clientSocket) {
        String clientIpAddress = clientSocket.getInetAddress().getHostAddress(); // IP подключившегося

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true))
        {
            String requestLine = reader.readLine();
            if (requestLine == null) return;

            System.out.println("Registry: Получен запрос [" + requestLine + "] от " + clientIpAddress);
            String[] parts = requestLine.split(":", 3);
            String command = parts[0].toUpperCase();

            switch (command) {
                case "REGISTER": // Формат: REGISTER:ip:port или REGISTER:port
                    if (parts.length >= 2) {
                        String ipToRegister;
                        int portToRegister;
                        try {
                            if (parts.length == 3) { // REGISTER:ip:port (ip предоставлен сервером)
                                ipToRegister = parts[1];
                                portToRegister = Integer.parseInt(parts[2]);
                            } else { // REGISTER:port (ip берем от подключения)
                                ipToRegister = clientIpAddress; // Используем IP самого подключения
                                portToRegister = Integer.parseInt(parts[1]);
                            }
                            if (portToRegister < 1 || portToRegister > 65535) throw new NumberFormatException("Неверный диапазон порта");
                            if ("0.0.0.0".equals(ipToRegister) || "127.0.0.1".equals(ipToRegister) || "localhost".equalsIgnoreCase(ipToRegister)) {
                                ipToRegister = clientIpAddress; // Если сервер прислал localhost или 0.0.0.0, используем реальный IP подключения
                                System.out.println("Registry: IP для регистрации скорректирован на " + ipToRegister + " для порта " + portToRegister);
                            }


                            String serverAddress = ipToRegister + ":" + portToRegister;
                            registerServer(serverAddress);
                            writer.println("OK Registered " + serverAddress);
                            System.out.println("Registry: Зарегистрирован сервер " + serverAddress);
                        } catch (NumberFormatException e) {
                            writer.println("ERROR Invalid port format: " + e.getMessage());
                            System.err.println("Registry: Неверный формат порта в запросе REGISTER: " + requestLine + " от " + clientIpAddress);
                        }
                    } else { writer.println("ERROR Invalid REGISTER format"); }
                    break;

                case "UNREGISTER": // Формат: UNREGISTER:ip:port или UNREGISTER:port
                    if (parts.length >= 2) {
                        String ipToUnregister; int portToUnregister;
                        try {
                            if (parts.length == 3) { ipToUnregister = parts[1]; portToUnregister = Integer.parseInt(parts[2]); }
                            else { ipToUnregister = clientIpAddress; portToUnregister = Integer.parseInt(parts[1]); }
                            if ("0.0.0.0".equals(ipToUnregister) || "127.0.0.1".equals(ipToUnregister) || "localhost".equalsIgnoreCase(ipToUnregister)) {
                                ipToUnregister = clientIpAddress;
                            }
                            String serverAddress = ipToUnregister + ":" + portToUnregister;
                            unregisterServer(serverAddress);
                            writer.println("OK Unregistered " + serverAddress);
                            System.out.println("Registry: Разрегистрирован сервер " + serverAddress);
                        } catch (NumberFormatException e) { writer.println("ERROR Invalid port format"); }
                    } else { writer.println("ERROR Invalid UNREGISTER format"); }
                    break;

                case "GET_SERVERS":
                    List<String> serverList = getActiveServersList();
                    String response = serverList.stream().collect(Collectors.joining(","));
                    writer.println("SERVERS:" + response);
                    System.out.println("Registry: Отправлен список серверов (" + serverList.size() + " шт.) клиенту " + clientIpAddress);
                    break;

                case "PING": // Формат: PING:ip:port или PING:port
                    if (parts.length >= 2) {
                        String ipToPing; int portToPing;
                        try {
                            if (parts.length == 3) { ipToPing = parts[1]; portToPing = Integer.parseInt(parts[2]); }
                            else { ipToPing = clientIpAddress; portToPing = Integer.parseInt(parts[1]); }

                            if ("0.0.0.0".equals(ipToPing) || "127.0.0.1".equals(ipToPing) || "localhost".equalsIgnoreCase(ipToPing)) {
                                ipToPing = clientIpAddress;
                            }
                            String serverAddress = ipToPing + ":" + portToPing;

                            if (activeServers.containsKey(serverAddress)) {
                                activeServers.put(serverAddress, System.currentTimeMillis());
                                writer.println("PONG");
                            } else {
                                writer.println("ERROR Server not registered, cannot PONG");
                                System.err.println("Registry: Получен PING от незарегистрированного сервера " + serverAddress + " (от " + clientIpAddress + ")");
                            }
                        } catch (NumberFormatException e) { writer.println("ERROR Invalid port format for PING"); }
                    } else { writer.println("ERROR Invalid PING format"); }
                    break;

                default:
                    writer.println("ERROR Unknown command");
                    System.err.println("Registry: Неизвестная команда: " + command + " от " + clientIpAddress);
                    break;
            }

        } catch (IOException e) {
            System.err.println("Registry: Ошибка обработки запроса от " + clientIpAddress + ": " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException e) { /* игнорируем */ }
        }
    }


    private void registerServer(String serverAddress) {
        activeServers.put(serverAddress, System.currentTimeMillis());
    }

    private void unregisterServer(String serverAddress) {
        activeServers.remove(serverAddress);
    }

    private List<String> getActiveServersList() {
        return new ArrayList<>(activeServers.keySet());
    }

    private void startCleanupTask() {
        if (cleanupTimer != null) { cleanupTimer.cancel(); }
        cleanupTimer = new Timer("RegistryCleanupTimer", true);
        cleanupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int removedCount = 0;
                // Собираем ключи для удаления, чтобы избежать ConcurrentModificationException
                List<String> serversToRemove = new ArrayList<>();
                for(Map.Entry<String, Long> entry : activeServers.entrySet()){
                    if((now - entry.getValue()) > SERVER_TIMEOUT_MS){
                        serversToRemove.add(entry.getKey());
                    }
                }
                for(String serverKey : serversToRemove){
                    activeServers.remove(serverKey);
                    removedCount++;
                }
                if (removedCount > 0) {
                    System.out.println("Registry Cleanup: Удалено " + removedCount + " неактивных серверов. Активных: " + activeServers.size());
                }
            }
        }, SERVER_TIMEOUT_MS, SERVER_TIMEOUT_MS / 2);
        System.out.println("Registry: Задача очистки неактивных серверов запущена.");
    }

    public void stopServer() {
        running = false;
        System.out.println("Registry Server: Остановка...");
        if (cleanupTimer != null) { cleanupTimer.cancel(); cleanupTimer = null; }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); System.out.println("Registry: Серверный сокет закрыт.");}
            catch (IOException e) { System.err.println("Registry: Ошибка закрытия сокета: " + e.getMessage());}
        }
        clientExecutor.shutdown();
        try {
            if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                clientExecutor.shutdownNow();
            }
        } catch (InterruptedException e) { clientExecutor.shutdownNow(); Thread.currentThread().interrupt(); }
        System.out.println("Registry Server: Полностью остановлен.");
    }

    public static void main(String[] args) {
        try {
            RegistryServer registry = new RegistryServer();
            registry.start();
        } catch (Exception e) {
            System.err.println("Registry: Не удалось запустить сервер регистрации.");
            e.printStackTrace();
        }
    }
}