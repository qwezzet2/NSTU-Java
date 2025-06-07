// ru.nstu.laba1timp.model.Person.java
package ru.nstu.laba1timp.model;

import ru.nstu.laba1timp.Habitat;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import java.util.Random;

// Абстрактный базовый класс для всех персонажей
public abstract class Person implements IBehaviour {
    protected ImageView imageView;  // Графическое представление
    protected int id;  // Уникальный идентификатор

    public Person() {
        Habitat hab = Habitat.getInstance();
        Random rand = new Random();
        // Генерация уникального ID
        do {
            id = rand.nextInt(100000, 1000000);
        } while (hab.getIdCollection().contains(id));
    }

    public abstract ImageView getImageView();

    public int getId() {
        return id;
    }

    // Метод для перемещения объекта (выполняется в UI потоке)
    public void moveTo(double x, double y) {
        Platform.runLater(() -> {
            imageView.setX(x);
            imageView.setY(y);
        });
    }
}