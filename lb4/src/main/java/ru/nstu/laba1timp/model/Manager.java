// ru.nstu.laba1timp.model.Manager.java
package ru.nstu.laba1timp.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.application.Platform;

// Класс Manager, наследующийся от Person
public class Manager extends Person {
    public static int count = 0;  // Счетчик созданных объектов
    public static int spawnedCount = 0;  // Счетчик созданных за сеанс объектов
    static Image image;  // Изображение объекта
    private static int lifeTime;  // Время жизни объекта

    // Поля для кругового движения
    public double circleCenterX = 0, circleCenterY = 0;  // Центр окружности
    public double angle = 0;  // Текущий угол

    static {
        try {
            // Загрузка изображения при инициализации класса
            image = new Image(new FileInputStream("src/main/resources/Stlb.png"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Manager(int x, int y) throws FileNotFoundException {
        super();
        imageView = new ImageView(image);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setFitWidth(180);  // Ширина изображения
        imageView.setFitHeight(140);  // Высота изображения
        imageView.setPreserveRatio(true);
        count++;
        spawnedCount++;
    }

    @Override
    public ImageView getImageView() {
        return imageView;
    }

    public static void setLifeTime(int lifeTime) {
        Manager.lifeTime = lifeTime;
    }

    public static int getLifeTime() {
        return lifeTime;
    }

    @Override
    public void moveTo(double x, double y) {
        Platform.runLater(() -> {
            imageView.setX(x);
            imageView.setY(y);
        });
    }
}