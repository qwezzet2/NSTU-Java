package ru.nstu.laba1timp.api;

import ru.nstu.laba1timp.dto.*; // Импортируем все DTO

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.Arrays; // Для логгирования массива

/**
 * Поток, непрерывно слушающий сообщения от сервера и вызывающий
 * соответствующие статические методы в классе Client для обработки.
 */
public class ServerListener extends Thread {
    private final ObjectInputStream ois; // Поток для чтения объектов от сервера

    public ServerListener(ObjectInputStream ois) {
        this.ois = ois;
        this.setDaemon(true); // Позволяет приложению завершиться, даже если поток активен
        this.setName("ClientApiServerListener"); // Имя для отладки
    }

    @Override
    public void run() {
        System.out.println("!!! ServerListener: Поток запущен.");
        try {
            // Цикл чтения сообщений от сервера
            while (!Thread.currentThread().isInterrupted()) {
                Object receivedObject = ois.readObject(); // Блокируется до получения объекта
                System.out.println("!!! ServerListener: Получен объект: " + (receivedObject != null ? receivedObject.getClass().getName() : "null"));

                // Обрабатываем только объекты типа DTO
                if (receivedObject instanceof DTO dto) {
                    System.out.println("!!! ServerListener: Получен DTO типа: " + dto.getClass().getSimpleName());
                    // Определяем тип DTO и вызываем соответствующий обработчик в Client
                    if (dto instanceof UserlistDTO userlistDTO) {
                        System.out.println("!!! ServerListener: Обработка UserlistDTO с данными: " + Arrays.toString(userlistDTO.getUserList())); // Лог содержимого
                        Client.handleUserList(userlistDTO);
                    } else if (dto instanceof UsernameDTO usernameDTO) {
                        System.out.println("!!! ServerListener: Обработка UsernameDTO для " + usernameDTO.getUserName() + " (статус: " + usernameDTO.getStatus() + ")");
                        Client.handleUserUpdate(usernameDTO);
                    } else if (dto instanceof ObjectExchangeRequestDTO requestDTO) {
                        System.out.println("!!! ServerListener: Обработка ObjectExchangeRequestDTO от " + requestDTO.getSourceUser() + " к " + requestDTO.getTargetUser());
                        Client.handleExchangeRequest(requestDTO);
                    } else if (dto instanceof ObjectExchangeDataDTO dataDTO) {
                        System.out.println("!!! ServerListener: Обработка ObjectExchangeDataDTO от " + dataDTO.getSourceUser() + " к " + dataDTO.getTargetUser() + " ("+dataDTO.getPersons().size()+" объектов)");
                        Client.handleExchangeData(dataDTO);
                    }
                    // TODO: Добавить обработку других типов DTO, если они появятся
                    else {
                        System.err.println("Слушатель: Получен неизвестный тип DTO: " + dto.getClass().getName());
                    }
                } else {
                    // Получен объект неожиданного типа
                    System.err.println("Слушатель: Получен не-DTO объект: " + (receivedObject != null ? receivedObject.getClass().getName() : "null"));
                }
            }
        } catch (SocketException e) {
            if (!Thread.currentThread().isInterrupted()) { System.err.println("!!! ServerListener: SocketException: " + e.getMessage()); Client.handleDisconnectError(e); }
            else { System.out.println("!!! ServerListener: SocketException во время прерывания."); }
        } catch (EOFException e) {
            if (!Thread.currentThread().isInterrupted()) { System.err.println("!!! ServerListener: EOFException."); Client.handleDisconnectError(e); }
            else { System.out.println("!!! ServerListener: EOFException во время прерывания."); }
        } catch (IOException | ClassNotFoundException e) {
            if (!Thread.currentThread().isInterrupted()) { System.err.println("!!! ServerListener: IOException/ClassNotFoundException: " + e.getMessage()); Client.handleDisconnectError(e); }
            else { System.out.println("!!! ServerListener: IOException/ClassNotFoundException во время прерывания."); }
        } catch (Exception e) { // Ловим остальные ошибки
            if (!Thread.currentThread().isInterrupted()) { System.err.println("!!! ServerListener: Неожиданная ошибка: " + e.getMessage()); e.printStackTrace(); Client.handleDisconnectError(e); }
            else { System.out.println("!!! ServerListener: Неожиданная ошибка во время прерывания."); }
        } finally {
            System.out.println("!!! ServerListener: Поток завершен.");
            // OIS закрывается в методе Client.disconnectFromServer или Client.closeResources
        }
    }
}