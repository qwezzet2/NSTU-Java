package ru.nstu.laba1timp.model;

// import ru.nstu.laba1timp.controllers.Habitat;
import ru.nstu.laba1timp.controllers.Habitat;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import java.util.Random;
import java.io.Serializable;

// Абстрактный базовый класс для всех персонажей
public abstract class Person implements IBehaviour, Serializable { // Реализован Serializable

    // Графическое представление (transient, т.к. не сериализуется)
    protected transient ImageView imageView;

    // Уникальный идентификатор
    protected int id;

    // Координаты для сохранения состояния и использования в AI
    protected double currentX;
    protected double currentY;

    // Версия для контроля сериализации
    private static final long serialVersionUID = 1L;

    // Конструктор
    public Person() {
        Habitat hab = Habitat.getInstance(); // Получаем экземпляр среды
        Random rand = new Random(); // Генератор случайных чисел
        // Генерация уникального ID с проверкой на существование
        do {
            id = rand.nextInt(100000, 1000000);
            // Проверяем, что Habitat и его коллекции инициализированы перед проверкой ID
        } while (hab != null && hab.getIdCollection() != null && hab.getIdCollection().contains(id));


    }

    // Абстрактный метод для получения ImageView
    public abstract ImageView getImageView();

    // Геттер для ID
    public int getId() {
        return id;
    }

    // Геттер для сохраненной координаты X
    public double getCurrentX() {
        return currentX;
    }

    // Геттер для сохраненной координаты Y
    public double getCurrentY() {
        return currentY;
    }

    // Абстрактный метод для воссоздания ImageView после десериализации
    // Принимает координаты, по которым нужно разместить ImageView
    public abstract void recreateImageView(double x, double y);

    // Метод для перемещения объекта
    // Обновляет сохраненные координаты и асинхронно перемещает ImageView в UI потоке
    public void moveTo(double x, double y) {
        // Обновляем внутренние координаты объекта
        this.currentX = x;
        this.currentY = y;

        // Запускаем обновление UI в потоке JavaFX Application Thread
        Platform.runLater(() -> {
            // Проверяем, что ImageView существует (важно после десериализации)
            if (imageView != null) {
                imageView.setX(x);
                imageView.setY(y);
            } else {

            }
        });
    }
}