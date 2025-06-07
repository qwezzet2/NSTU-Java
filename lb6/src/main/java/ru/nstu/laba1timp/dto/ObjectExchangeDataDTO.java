package ru.nstu.laba1timp.dto;

import ru.nstu.laba1timp.model.Person; // Убедитесь, что импорт корректен
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * DTO для передачи фактического списка объектов и их времен рождения при обмене.
 * Передает ВСЕ объекты ОДНОГО типа.
 */
public class ObjectExchangeDataDTO extends DTO implements Serializable {
    private static final long serialVersionUID = 104L; // Можно оставить прежний или изменить
    private final String sourceUser; // Пользователь, ОТПРАВЛЯЮЩИЙ эти данные
    private final String targetUser; // Пользователь, которому ПРЕДНАЗНАЧЕНЫ эти данные
    // Список объектов Person (ОДНОГО типа - Developer или Manager)
    private final List<Person> persons;
    // Карта времен рождения <ID объекта, Время рождения>
    private final Map<Integer, Integer> birthTimes;
    // Тип отправляемых объектов (0 = Developer, 1 = Manager)
    private final int typeSent;

    /**
     * Конструктор для данных обмена.
     * @param sourceUser Имя пользователя, отправляющего данные.
     * @param targetUser Имя пользователя, получающего данные.
     * @param persons Список объектов Person для отправки (все одного типа!).
     * @param birthTimes Карта времен рождения для отправляемых объектов.
     * @param typeSent Тип отправляемых объектов (0 или 1).
     */
    public ObjectExchangeDataDTO(String sourceUser, String targetUser, List<Person> persons, Map<Integer, Integer> birthTimes, int typeSent) {
        if (typeSent != 0 && typeSent != 1) {
            throw new IllegalArgumentException("typeSent должен быть 0 (Developer) или 1 (Manager)");
        }
        this.sourceUser = sourceUser;
        this.targetUser = targetUser;
        // Создаем копии коллекций
        this.persons = new ArrayList<>(persons);
        this.birthTimes = new HashMap<>(birthTimes);
        this.typeSent = typeSent;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public String getTargetUser() {
        return targetUser;
    }

    /**
     * Возвращает список передаваемых объектов Person.
     * @return Копия списка объектов.
     */
    public List<Person> getPersons() {
        return new ArrayList<>(persons);
    }

    /**
     * Возвращает карту времен рождения для передаваемых объектов.
     * @return Копия карты <ID, ВремяРождения>.
     */
    public Map<Integer, Integer> getBirthTimes() {
        return new HashMap<>(birthTimes);
    }

    /**
     * Возвращает тип отправленных объектов.
     * @return 0 для Developer, 1 для Manager.
     */
    public int getTypeSent() {
        return typeSent;
    }
}