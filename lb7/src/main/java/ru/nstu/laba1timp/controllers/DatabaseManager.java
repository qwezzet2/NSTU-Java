package ru.nstu.laba1timp.controllers;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс DatabaseManager отвечает за взаимодействие с базой данных SQLite.
 * Предоставляет методы для сохранения, загрузки, удаления и управления состояниями симуляции.
 */
public class DatabaseManager {
    /**
     * Имя файла базы данных SQLite.
     */
    private static final String DB_FILENAME = "simulation_slots_v2.db"; // Новое имя для новой версии структуры

    /**
     * URL подключения к базе данных.
     */
    private static final String URL = "jdbc:sqlite:" + DB_FILENAME;

    /**
     * Соединение с базой данных.
     */
    private Connection connection;

    /**
     * Перечисление типов сохраняемых данных.
     */
    public enum SaveType {
        ALL_PERSONS,
        DEVELOPERS_ONLY,
        MANAGERS_ONLY
    }

    /**
     * Конструктор. Устанавливает соединение с БД и создаёт таблицы при необходимости.
     *
     * @throws SQLException если произошла ошибка базы данных
     */
    public DatabaseManager() throws SQLException {
        try {
            connection = DriverManager.getConnection(URL);
            createPersonTableIfNotExist();
            createSlotsMetadataTableIfNotExists();
            System.out.println("Подключено к SQLite базе данных (v2): " + URL);
        } catch (SQLException e) {
            System.err.println("Критическая ошибка подключения к БД или инициализации таблиц: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Создаёт таблицу для хранения объектов Person, если она не существует.
     *
     * @throws SQLException если произошла ошибка базы данных
     */
    private void createPersonTableIfNotExist() throws SQLException {
        String createPersonsTable = "CREATE TABLE IF NOT EXISTS persons_data (" +
                "id INTEGER NOT NULL, " +
                "slot_name TEXT NOT NULL, " +
                "person_type TEXT NOT NULL, " +
                "birth_time INTEGER, " +
                "current_x REAL, " +
                "current_y REAL, " +
                "dev_dx REAL, " +
                "dev_dy REAL, " +
                "dev_direction_change_counter INTEGER, " +
                "man_circle_center_x REAL, " +
                "man_circle_center_y REAL, " +
                "man_angle REAL, " +
                "PRIMARY KEY (slot_name, id))";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPersonsTable);
            System.out.println("Таблица 'persons_data' проверена/создана.");
        }
    }

    /**
     * Создаёт таблицу метаданных слотов, если она не существует.
     *
     * @throws SQLException если произошла ошибка базы данных
     */
    private void createSlotsMetadataTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS slot_metadata (" +
                "slot_name TEXT PRIMARY KEY, " +
                "save_type TEXT NOT NULL, " +
                "developer_count INTEGER DEFAULT 0, " +
                "manager_count INTEGER DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Таблица 'slot_metadata' проверена/создана.");
        }
    }

    /**
     * Сохраняет указанные объекты Person в указанный слот.
     *
     * @param personsToSave Список объектов Person для сохранения
     * @param slotName      Имя слота
     * @param saveType      Тип сохранения (ALL_PERSONS, DEVELOPERS_ONLY, MANAGERS_ONLY)
     * @param bornTimes     Карта <ID, время рождения> для сохраняемых объектов
     * @throws SQLException если произошла ошибка базы данных
     */
    public void savePersonsToSlot(List<Person> personsToSave, String slotName, SaveType saveType, Map<Integer, Integer> bornTimes) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        int devCountInSave = 0;
        int manCountInSave = 0;

        try {
            connection.setAutoCommit(false);

            if (saveType == SaveType.ALL_PERSONS) {
                clearPersonsFromSlot(slotName, null);
            } else if (saveType == SaveType.DEVELOPERS_ONLY) {
                clearPersonsFromSlot(slotName, Developer.class);
            } else if (saveType == SaveType.MANAGERS_ONLY) {
                clearPersonsFromSlot(slotName, Manager.class);
            }

            String personSql = "INSERT OR REPLACE INTO persons_data (id, slot_name, person_type, birth_time, current_x, current_y, " +
                    "dev_dx, dev_dy, dev_direction_change_counter, " +
                    "man_circle_center_x, man_circle_center_y, man_angle) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(personSql)) {
                for (Person p : personsToSave) {
                    if (saveType == SaveType.DEVELOPERS_ONLY && !(p instanceof Developer)) continue;
                    if (saveType == SaveType.MANAGERS_ONLY && !(p instanceof Manager)) continue;

                    pstmt.setInt(1, p.getId());
                    pstmt.setString(2, slotName);

                    if (p instanceof Developer) {
                        pstmt.setString(3, "Developer");
                        pstmt.setInt(4, bornTimes.getOrDefault(p.getId(), 0));
                        pstmt.setDouble(5, p.getCurrentX());
                        pstmt.setDouble(6, p.getCurrentY());
                        Developer dev = (Developer) p;
                        pstmt.setDouble(7, dev.dx);
                        pstmt.setDouble(8, dev.dy);
                        pstmt.setInt(9, dev.directionChangeCounter);
                        pstmt.setNull(10, Types.REAL);
                        pstmt.setNull(11, Types.REAL);
                        pstmt.setNull(12, Types.REAL);
                        devCountInSave++;
                    } else if (p instanceof Manager) {
                        pstmt.setString(3, "Manager");
                        pstmt.setInt(4, bornTimes.getOrDefault(p.getId(), 0));
                        pstmt.setDouble(5, p.getCurrentX());
                        pstmt.setDouble(6, p.getCurrentY());
                        Manager man = (Manager) p;
                        pstmt.setNull(7, Types.REAL);
                        pstmt.setNull(8, Types.REAL);
                        pstmt.setNull(9, Types.INTEGER);
                        pstmt.setDouble(10, man.circleCenterX);
                        pstmt.setDouble(11, man.circleCenterY);
                        pstmt.setDouble(12, man.angle);
                        manCountInSave++;
                    } else {
                        continue;
                    }

                    pstmt.addBatch();
                }

                if (!personsToSave.isEmpty()) {
                    pstmt.executeBatch();
                }
            }

            if (saveType == SaveType.ALL_PERSONS) {
                saveSlotMetadata(slotName, saveType.name(), devCountInSave, manCountInSave);
            } else {
                Map<String, Integer> counts = getCountsForSlot(slotName);
                if (saveType == SaveType.DEVELOPERS_ONLY) {
                    saveSlotMetadata(slotName, saveType.name(), devCountInSave, counts.getOrDefault("manager", 0));
                } else if (saveType == SaveType.MANAGERS_ONLY) {
                    saveSlotMetadata(slotName, saveType.name(), counts.getOrDefault("developer", 0), manCountInSave);
                }
            }

            connection.commit();
            System.out.println(saveType.name() + " сохранены в слот '" + slotName + "'. Devs: " + devCountInSave + ", Mans: " + manCountInSave);

        } catch (SQLException e) {
            connection.rollback();
            System.err.println("Ошибка при сохранении в слот '" + slotName + "': " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Возвращает количество разработчиков и менеджеров в указанном слоте.
     *
     * @param slotName имя слота
     * @return карта с количеством разработчиков и менеджеров
     * @throws SQLException если произошла ошибка базы данных
     */
    private Map<String, Integer> getCountsForSlot(String slotName) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        String sql = "SELECT developer_count, manager_count FROM slot_metadata WHERE slot_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, slotName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    counts.put("developer", rs.getInt("developer_count"));
                    counts.put("manager", rs.getInt("manager_count"));
                } else {
                    counts.put("developer", 0);
                    counts.put("manager", 0);
                }
            }
        }
        return counts;
    }

    /**
     * Обновляет или создаёт запись метаданных для слота.
     *
     * @param slotName   имя слота
     * @param saveTypeStr тип сохранения как строка
     * @param devCount   количество разработчиков
     * @param manCount   количество менеджеров
     * @throws SQLException если произошла ошибка базы данных
     */
    private void saveSlotMetadata(String slotName, String saveTypeStr, int devCount, int manCount) throws SQLException {
        String metaSql = "INSERT OR REPLACE INTO slot_metadata (slot_name, save_type, developer_count, manager_count, created_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = connection.prepareStatement(metaSql)) {
            pstmt.setString(1, slotName);
            pstmt.setString(2, saveTypeStr);
            pstmt.setInt(3, devCount);
            pstmt.setInt(4, manCount);
            pstmt.executeUpdate();
        }
    }

    /**
     * Загружает объекты Person из указанного слота.
     *
     * @param slotName имя слота
     * @param loadType тип загрузки (ALL_PERSONS, DEVELOPERS_ONLY, MANAGERS_ONLY)
     * @return карта с ключами "persons" и "bornTimes"
     * @throws SQLException если произошла ошибка базы данных
     */
    public Map<String, Object> loadPersonsFromSlot(String slotName, SaveType loadType) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        List<Person> persons = new ArrayList<>();
        Map<Integer, Integer> bornTimes = new HashMap<>();

        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT id, person_type, birth_time, current_x, current_y, " +
                        "dev_dx, dev_dy, dev_direction_change_counter, " +
                        "man_circle_center_x, man_circle_center_y, man_angle " +
                        "FROM persons_data WHERE slot_name = ?");

        if (loadType == SaveType.DEVELOPERS_ONLY) {
            sqlBuilder.append(" AND person_type = 'Developer'");
        } else if (loadType == SaveType.MANAGERS_ONLY) {
            sqlBuilder.append(" AND person_type = 'Manager'");
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sqlBuilder.toString())) {
            pstmt.setString(1, slotName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String type = rs.getString("person_type");
                    int birthTimeFromDB = rs.getInt("birth_time");
                    double x = rs.getDouble("current_x");
                    double y = rs.getDouble("current_y");

                    Person p = null;
                    try {
                        if ("Developer".equals(type) && (loadType == SaveType.ALL_PERSONS || loadType == SaveType.DEVELOPERS_ONLY)) {
                            Developer dev = new Developer(0, 0);
                            dev.dx = rs.getDouble("dev_dx");
                            dev.dy = rs.getDouble("dev_dy");
                            dev.directionChangeCounter = rs.getInt("dev_direction_change_counter");
                            p = dev;
                        } else if ("Manager".equals(type) && (loadType == SaveType.ALL_PERSONS || loadType == SaveType.MANAGERS_ONLY)) {
                            Manager man = new Manager(0, 0);
                            man.circleCenterX = rs.getDouble("man_circle_center_x");
                            man.circleCenterY = rs.getDouble("man_circle_center_y");
                            man.angle = rs.getDouble("man_angle");
                            p = man;
                        }
                    } catch (java.io.FileNotFoundException e) {
                        System.err.println("Ошибка ресурса (изображение): Не удалось создать объект ID " + id + ", тип " + type + ". " + e.getMessage());
                        continue;
                    }

                    if (p != null) {
                        p.setIdForLoad(id);
                        p.setCurrentXForLoad(x);
                        p.setCurrentYForLoad(y);
                        persons.add(p);
                        bornTimes.put(id, birthTimeFromDB);
                    }
                }
            }
        }

        result.put("persons", persons);
        result.put("bornTimes", bornTimes);

        Map<String, Integer> counts = getCountsForSlot(slotName);
        result.put("devSpawnedCount", counts.getOrDefault("developer", 0));
        result.put("manSpawnedCount", counts.getOrDefault("manager", 0));

        System.out.println(loadType.name() + " загружены из слота '" + slotName + "'. Найдено: " + persons.size());

        return result;
    }

    /**
     * Возвращает список всех доступных слотов.
     *
     * @return список имён слотов
     * @throws SQLException если произошла ошибка базы данных
     */
    public List<String> getSavedSlotNames() throws SQLException {
        List<String> slots = new ArrayList<>();
        String sql = "SELECT slot_name FROM slot_metadata ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                slots.add(rs.getString("slot_name"));
            }
        }
        return slots;
    }

    /**
     * Получает метаданные слота.
     *
     * @param slotName имя слота
     * @return карта с метаданными
     * @throws SQLException если произошла ошибка базы данных
     */
    public Map<String, String> getSlotMetadata(String slotName) throws SQLException {
        Map<String, String> metadata = new HashMap<>();
        String sql = "SELECT save_type, developer_count, manager_count, strftime('%Y-%m-%d %H:%M:%S', created_at) as created_at_str FROM slot_metadata WHERE slot_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, slotName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    metadata.put("save_type", rs.getString("save_type"));
                    metadata.put("developer_count", String.valueOf(rs.getInt("developer_count")));
                    metadata.put("manager_count", String.valueOf(rs.getInt("manager_count")));
                    metadata.put("created_at", rs.getString("created_at_str"));
                }
            }
        }
        return metadata;
    }

    /**
     * Удаляет указанный слот.
     *
     * @param slotName имя слота
     * @throws SQLException если произошла ошибка базы данных
     */
    public void deleteSlot(String slotName) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            clearPersonsFromSlot(slotName, null);
            String deleteMetaSql = "DELETE FROM slot_metadata WHERE slot_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteMetaSql)) {
                pstmt.setString(1, slotName);
                pstmt.executeUpdate();
            }
            connection.commit();
            System.out.println("Слот '" + slotName + "' удален из БД.");
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("Ошибка при удалении слота '" + slotName + "': " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Переименовывает указанный слот.
     *
     * @param oldName старое имя
     * @param newName новое имя
     * @throws SQLException если произошла ошибка базы данных
     */
    public void renameSlot(String oldName, String newName) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM slot_metadata WHERE slot_name = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, newName);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Слот с именем '" + newName + "' уже существует.");
                }
            }
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            String updatePersons = "UPDATE persons_data SET slot_name = ? WHERE slot_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updatePersons)) {
                pstmt.setString(1, newName);
                pstmt.setString(2, oldName);
                pstmt.executeUpdate();
            }

            String updateMeta = "UPDATE slot_metadata SET slot_name = ? WHERE slot_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateMeta)) {
                pstmt.setString(1, newName);
                pstmt.setString(2, oldName);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    connection.rollback();
                    throw new SQLException("Слот '" + oldName + "' не найден в metadata для переименования.");
                }
            }

            connection.commit();
            System.out.println("Слот '" + oldName + "' переименован в '" + newName + "'.");
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("Ошибка при переименовании слота '" + oldName + "' в '" + newName + "': " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Очищает данные Person из указанного слота.
     *
     * @param slotName      имя слота
     * @param typeToClear   тип объекта (Developer.class, Manager.class), null - все
     * @throws SQLException если произошла ошибка базы данных
     */
    private void clearPersonsFromSlot(String slotName, Class<? extends Person> typeToClear) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("DELETE FROM persons_data WHERE slot_name = ?");
        if (typeToClear == Developer.class) {
            sqlBuilder.append(" AND person_type = 'Developer'");
        } else if (typeToClear == Manager.class) {
            sqlBuilder.append(" AND person_type = 'Manager'");
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sqlBuilder.toString())) {
            pstmt.setString(1, slotName);
            pstmt.executeUpdate();
        }
    }

    /**
     * Закрывает соединение с базой данных.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Соединение с SQLite базой данных (v2) закрыто.");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии соединения с БД (v2): " + e.getMessage());
        }
    }
}
