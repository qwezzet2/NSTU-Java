package ru.nstu.laba1timp.model;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import java.util.Random;
import java.io.Serializable;

import ru.nstu.laba1timp.controllers.Habitat; // Импортируем Habitat для получения ID

/**
 * Абстрактный класс Person представляет базовый элемент симуляции.
 * Содержит общие свойства и методы для всех персонажей (Developer, Manager).
 */
public abstract class Person implements IBehaviour, Serializable {

    /**
     * ImageView объекта. transient — не сериализуется.
     */
    protected transient ImageView imageView;

    /**
     * Уникальный идентификатор объекта.
     */
    protected int id;

    /**
     * Текущая координата X для логики и сохранения состояния.
     */
    protected double currentX;

    /**
     * Текущая координата Y для логики и сохранения состояния.
     */
    protected double currentY;

    /**
     * Версия сериализации для контроля совместимости.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Конструктор Person.
     * Генерирует уникальный ID на основе существующих в среде.
     */
    public Person() {
        Habitat hab = Habitat.getInstance(); // Получаем экземпляр среды
        Random rand = new Random();          // Для генерации случайного ID

        // Генерация уникального ID с проверкой на существование
        if (hab != null && hab.getIdCollection() != null) {
            synchronized (hab.getIdCollection()) { // Потокобезопасность
                do {
                    this.id = rand.nextInt(100000, 1000000);
                } while (hab.getIdCollection().contains(this.id));

                // Не добавляем ID здесь — это делает Habitat или FileMaster при загрузке
            }
        } else {
            // Если Habitat или коллекция не готовы — генерируем без проверки
            this.id = rand.nextInt(100000, 1000000);
            System.err.println("Предупреждение (Person конструктор): Habitat или idCollection не инициализированы. ID сгенерирован без проверки уникальности.");
        }
    }

    /**
     * Возвращает ImageView объекта.
     * Должен быть реализован в подклассах.
     *
     * @return ImageView
     */
    public abstract ImageView getImageView();

    /**
     * Возвращает уникальный идентификатор объекта.
     *
     * @return ID объекта
     */
    public int getId() {
        return id;
    }

    /**
     * Устанавливает ID объекта.
     * Предназначен только для использования при загрузке из сохранённого состояния.
     *
     * @param loadedId загруженный ID
     */
    public void setIdForLoad(int loadedId) {
        this.id = loadedId;
    }

    /**
     * Возвращает текущую координату X.
     *
     * @return координата X
     */
    public double getCurrentX() {
        return currentX;
    }

    /**
     * Устанавливает координату X при восстановлении из сохранённого состояния.
     *
     * @param x новая координата X
     */
    public void setCurrentXForLoad(double x) {
        this.currentX = x;
    }

    /**
     * Возвращает текущую координату Y.
     *
     * @return координата Y
     */
    public double getCurrentY() {
        return currentY;
    }

    /**
     * Устанавливает координату Y при восстановлении из сохранённого состояния.
     *
     * @param y новая координата Y
     */
    public void setCurrentYForLoad(double y) {
        this.currentY = y;
    }

    /**
     * Воссоздаёт ImageView после десериализации.
     * Должен быть реализован в подклассах.
     *
     * @param x координата X для размещения
     * @param y координата Y для размещения
     */
    public abstract void recreateImageView(double x, double y);

    /**
     * Перемещает объект по указанным координатам.
     * Обновляет внутренние значения и перемещает ImageView в UI потоке.
     *
     * @param x новая координата X
     * @param y новая координата Y
     */
    public void moveTo(double x, double y) {
        this.currentX = x;
        this.currentY = y;

        Platform.runLater(() -> {
            if (imageView != null) {
                imageView.setX(x);
                imageView.setY(y);
            } else {

            }
        });
    }
}
