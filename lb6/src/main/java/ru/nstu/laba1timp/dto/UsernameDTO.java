package ru.nstu.laba1timp.dto;

import java.io.Serializable;

/**
 * DTO для передачи имени пользователя и статуса его подключения.
 * Аналогично "Чужой лабе 6".
 */
public class UsernameDTO extends DTO implements Serializable {
    private static final long serialVersionUID = 101L;
    private final String userName;
    // Статус: true = пользователь подключился, false = пользователь отключился
    private final boolean status; // Добавлено поле status как в "Чужой лабе"

    /**
     * Конструктор для отправки имени при первом подключении.
     */
    public UsernameDTO(String userName) {
        this.userName = userName;
        this.status = true; // Подразумеваем подключение при отправке только имени
    }

    /**
     * Конструктор для рассылки уведомлений о подключении/отключении.
     */
    public UsernameDTO(String userName, boolean status) {
        this.userName = userName;
        this.status = status;
    }

    public String getUserName() {
        return userName;
    }

    public boolean getStatus() {
        return status;
    }
}