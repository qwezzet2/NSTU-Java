package ru.nstu.laba1timp.main.server;

// Импортируем DTO из твоего клиентского проекта
import ru.nstu.laba1timp.dto.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Обработчик для одного подключенного клиента.
 * Работает с ЭКЗЕМПЛЯРОМ Server для управления списком и пересылки сообщений.
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;
    private final Server server; // <<<=== Ссылка на ЭКЗЕМПЛЯР сервера
    private String userName;

    /**
     * Конструктор, принимающий экземпляр Server.
     */
    public ClientHandler(Socket socket, ObjectInputStream ois, ObjectOutputStream oos, Server server) { // <<<=== Принимаем Server
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        this.server = server; // <<<=== Сохраняем ссылку
        this.setName("ClientHandler-" + socket.getInetAddress() + ":" + socket.getPort());
    }

    @Override
    public void run() {
        System.out.println("Обработчик клиента запущен: " + getName());
        try {
            // 1. Читаем имя пользователя
            Object firstObject = ois.readObject();
            if (firstObject instanceof UsernameDTO initialDto) {
                this.userName = initialDto.getUserName().trim();
                // === Вызываем НЕСТАТИЧЕСКИЕ методы Server через ссылку 'server' ===
                try {
                    server.addUserToList(this.userName, this.oos);
                    // Методы userlistUnicast и userJoinedOrLeftBroadcast вызываются внутри addUserToList
                    System.out.println("Пользователь '" + userName + "' зарегистрирован на сервере.");
                } catch (IllegalArgumentException e) {
                    // Имя невалидно или уже занято - addUserToList выбросит исключение
                    System.err.println("Ошибка регистрации клиента: " + e.getMessage());
                    closeResources(); // Закрываем соединение
                    return; // Завершаем поток обработчика
                }
                // ==============================================================
            } else {
                System.err.println("Ошибка: Первый объект не UsernameDTO. Закрытие соединения.");
                closeResources();
                return;
            }

            // 2. Цикл чтения сообщений
            while (socket.isConnected() && !socket.isClosed() && !isInterrupted()) {
                Object receivedObject = ois.readObject(); // Читаем следующее сообщение
                if (receivedObject instanceof DTO dto) {
                    // === Обработка DTO обмена через НЕСТАТИЧЕСКИЕ методы ===
                    if (dto instanceof ObjectExchangeRequestDTO requestDto) {
                        if (requestDto.getSourceUser().equals(this.userName)) {
                            server.forwardExchangeRequest(requestDto); // <<<=== Вызов нестатического метода
                        } else { System.err.println("Security Warning: ExchangeRequest от чужого имени ("+requestDto.getSourceUser()+") для "+this.userName); }
                    } else if (dto instanceof ObjectExchangeDataDTO dataDto) {
                        if (dataDto.getSourceUser().equals(this.userName)) {
                            server.forwardExchangeData(dataDto); // <<<=== Вызов нестатического метода
                        } else { System.err.println("Security Warning: ExchangeData от чужого имени ("+dataDto.getSourceUser()+") для "+this.userName); }
                    }
                    // ====================================================
                    else { System.err.println("Предупреждение: Получен неизвестный тип DTO от " + userName + ": " + dto.getClass().getName()); }
                } else { System.err.println("Предупреждение: Получен не-DTO объект от " + userName + ": " + (receivedObject != null ? receivedObject.getClass().getName() : "null")); }
            }
        } catch (EOFException e) { System.out.println("Клиент " + (userName != null ? userName : "[не зарег.]") + " отключился (EOF).");
        } catch (SocketException e) { if (!socket.isClosed()) { System.out.println("Соединение с клиентом " + (userName != null ? userName : "[не зарег.]") + " потеряно (SocketException): " + e.getMessage()); }
        } catch (IOException | ClassNotFoundException e) { // Убрали IllegalArgumentException, он ловится выше
            if (!socket.isClosed()) { System.err.println("Ошибка при обработке клиента " + (userName != null ? userName : "[не зарег.]") + ": " + e.getMessage()); }
        } catch (Exception e){ if (!socket.isClosed()) { System.err.println("Непредвиденная ошибка в обработчике клиента " + (userName != null ? userName : "[не зарег.]") + ": " + e.getMessage()); e.printStackTrace(); } }
        finally {
            System.out.println("Завершение обработчика для " + (userName != null ? userName : "[неудачное подключение]"));
            if (userName != null) {
                // === Вызываем НЕСТАТИЧЕСКИЙ метод Server для удаления ===
                server.removeUserFromList(userName, this.oos);
                // Уведомление broadcastUserStatus вызывается внутри removeUserFromList
                // ========================================
            }
            closeResources(); // Закрываем сокет и потоки в любом случае
        }
    }

    /** Закрывает ресурсы клиента. */
    private void closeResources() {
        try { if (ois != null) ois.close(); } catch (IOException e) {}
        try { if (oos != null) oos.close(); } catch (IOException e) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
        // System.out.println("Ресурсы для клиента " + (userName != null ? userName : "[неудачное подключение]") + " закрыты.");
    }
}