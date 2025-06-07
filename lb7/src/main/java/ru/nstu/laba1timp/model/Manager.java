package ru.nstu.laba1timp.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable; // Добавлено
import javafx.application.Platform;

public class Manager extends Person implements Serializable { // Реализован Serializable
    public static int count = 0;
    public static int spawnedCount = 0;
    static transient Image image; // transient
    private static int lifeTime;

    // Поля для кругового движения (оставляем public, как было)
    public double circleCenterX = 0;
    public double circleCenterY = 0;
    public double angle = 0;

    private static final long serialVersionUID = 3L; // Версия для сериализации

    // Статический блок для загрузки изображения при инициализации класса
    static {
        loadImage();
    }

    // Метод для загрузки изображения
    private static void loadImage() {
        if (image == null) {
            try {
                image = new Image(new FileInputStream("src/main/resources/Stlb.png"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Не удалось загрузить изображение Manager (Stlb.png)", e);
            } catch (Exception e) {
                throw new RuntimeException("Непредвиденная ошибка при загрузке изображения Manager", e);
            }
        }
    }

    // Конструктор
    public Manager(int x, int y) throws FileNotFoundException {
        super(); // Вызов конструктора Person
        loadImage(); // Убеждаемся, что изображение загружено
        if (image == null) {
            throw new FileNotFoundException("Изображение Manager не может быть загружено.");
        }
        // Создаем ImageView и устанавливаем начальные координаты
        recreateImageView(x, y);
        this.currentX = x; // Сохраняем координаты в Person
        this.currentY = y;
        count++; // Инкремент общего счетчика
        spawnedCount++; // Инкремент счетчика сессии
        // Начальные значения circleCenterX, circleCenterY, angle остаются 0 по умолчанию,
        // они будут инициализированы в ManagerAI при первом обновлении.
    }

    // Геттер для ImageView
    @Override
    public ImageView getImageView() {
        return imageView;
    }

    // Метод воссоздания ImageView (для десериализации и конструктора)
    @Override
    public void recreateImageView(double x, double y) {
        loadImage(); // Убеждаемся, что изображение загружено
        if (image == null) {
            System.err.println("Невозможно воссоздать ImageView: Manager image is null.");
            return;
        }
        // Создаем и настраиваем ImageView
        imageView = new ImageView(image);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setFitWidth(180);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);
        // currentX/Y не устанавливаем здесь
    }

    // Сеттер для статического времени жизни
    public static void setLifeTime(int lifeTime) {
        Manager.lifeTime = lifeTime;
    }

    // Геттер для статического времени жизни
    public static int getLifeTime() {
        return lifeTime;
    }

    // Метод moveTo наследуется от Person
}