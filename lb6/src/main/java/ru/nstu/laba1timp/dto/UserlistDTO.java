package ru.nstu.laba1timp.dto;

import java.io.Serializable;
// Используем массив строк, как в "Чужой лабе 6"
// import java.util.List;
// import java.util.ArrayList;

/**
 * DTO для передачи списка имен пользователей (в виде массива строк).
 * Аналогично "Чужой лабе 6".
 */
public class UserlistDTO extends DTO implements Serializable {
    private static final long serialVersionUID = 102L;
    private final String[] userList; // Используем массив String[]

    /**
     * Конструктор.
     * @param userList Массив имен пользователей.
     */
    public UserlistDTO(String[] userList) {
        // Создаем копию массива для безопасности
        this.userList = (userList != null) ? userList.clone() : new String[0];
    }

    public String[] getUserList() {
        // Возвращаем копию массива
        return (userList != null) ? userList.clone() : new String[0];
    }
}