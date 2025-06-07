// ru.nstu.laba1timp.model.Developer.java
package ru.nstu.laba1timp.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.application.Platform;

// Класс Developer, наследующийся от Person
public class Developer extends Person {
    public static int count = 0;  // Счетчик созданных объектов
    public static int spawnedCount = 0;  // Счетчик созданных за сеанс объектов
    static Image image;  // Изображение объекта
    private static int lifeTime;  // Время жизни объекта

    // Поля для хаотичного движения
    public double dx, dy;  // Текущее направление движения
    public int directionChangeCounter = 0;  // Счетчик до смены направления

    static {
        try {
            // Загрузка изображения при инициализации класса
            image = new Image(new FileInputStream("src/main/resources/Car.png"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Developer(int x, int y) throws FileNotFoundException {
        super();
        imageView = new ImageView(image);  // Создание ImageView с изображением
        imageView.setX(x);  // Установка начальной позиции X
        imageView.setY(y);  // Установка начальной позиции Y
        imageView.setFitWidth(80);  // Ширина изображения
        imageView.setFitHeight(80);  // Высота изображения
        imageView.setPreserveRatio(true);  // Сохранение пропорций
        count++;  // Инкремент счетчиков
        spawnedCount++;
    }

    @Override
    public ImageView getImageView() {
        return imageView;  // Возвращает ImageView объекта
    }

    public static void setLifeTime(int lifeTime) {
        Developer.lifeTime = lifeTime;  // Установка времени жизни
    }

    public static int getLifeTime() {
        return lifeTime;  // Получение времени жизни
    }

    @Override
    public void moveTo(double x, double y) {
        // Обновление позиции в UI потоке
        Platform.runLater(() -> {
            imageView.setX(x);
            imageView.setY(y);
        });
    }
}