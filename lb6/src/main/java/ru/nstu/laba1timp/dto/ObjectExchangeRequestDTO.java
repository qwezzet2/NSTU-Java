package ru.nstu.laba1timp.dto;

import java.io.Serializable;

/**
 * DTO для отправки запроса на обмен объектами между клиентами.
 * Указывает тип, который запрашивающая сторона хочет ПОЛУЧИТЬ.
 */
public class ObjectExchangeRequestDTO extends DTO implements Serializable {
    private static final long serialVersionUID = 103L; // Можно оставить прежний или изменить
    private final String sourceUser; // Пользователь, инициирующий запрос
    private final String targetUser; // Пользователь, которому адресован запрос

    // Тип объектов, который ОТПРАВИТЕЛЬ ЗАПРОСА хочет ПОЛУЧИТЬ от ЦЕЛИ.
    // Соглашение: 0 = Developer, 1 = Manager
    // Зная это, цель поймет, какой тип она должна отправить взамен (противоположный).
    private final int requestedTypeToReceive;

    /**
     * Конструктор запроса на обмен.
     * @param sourceUser Имя отправителя запроса.
     * @param targetUser Имя получателя запроса.
     * @param requestedTypeToReceive Тип объектов, которые отправитель хочет получить (0 или 1).
     */
    public ObjectExchangeRequestDTO(String sourceUser, String targetUser, int requestedTypeToReceive) {
        if (requestedTypeToReceive != 0 && requestedTypeToReceive != 1) {
            throw new IllegalArgumentException("requestedTypeToReceive должен быть 0 (Developer) или 1 (Manager)");
        }
        this.sourceUser = sourceUser;
        this.targetUser = targetUser;
        this.requestedTypeToReceive = requestedTypeToReceive;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public String getTargetUser() {
        return targetUser;
    }

    /**
     * Возвращает тип объектов, которые источник (отправитель запроса) хочет получить взамен.
     * @return 0 для Developer, 1 для Manager.
     */
    public int getRequestedTypeToReceive() {
        return requestedTypeToReceive;
    }

    /**
     * Вспомогательный метод, определяющий тип, который ОТПРАВИТЕЛЬ ЗАПРОСА должен ОТДАТЬ.
     * Он всегда противоположен тому, что запрашивается.
     * @return 0 для Developer, 1 для Manager.
     */
    public int getTypeToSendFromSource() {
        return (requestedTypeToReceive == 0) ? 1 : 0; // Если просит 0 (Dev), отдает 1 (Man), и наоборот
    }
}