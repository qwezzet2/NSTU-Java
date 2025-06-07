package ru.nstu.laba1timp.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable; // Добавлено
import javafx.application.Platform;

public class Developer extends Person implements Serializable { // Реализован Serializable
    public static int count = 0;
    public static int spawnedCount = 0;
    static transient Image image; // transient
    private static int lifeTime;
    public double dx, dy; // Оставляем public как было
    public int directionChangeCounter = 0; // Оставляем public как было
    private static final long serialVersionUID = 2L;

    // Статический блок для загрузки изображения при инициализации класса
    static {
        loadImage();
    }

    // Метод для загрузки изображения (вызывается из static блока и recreateImageView)
    private static void loadImage() {
        if (image == null) {
            try {
                image = new Image(new FileInputStream("src/main/resources/Car.png"));
            } catch (FileNotFoundException e) {
                // Оборачиваем в RuntimeException, т.к. статическая инициализация не может бросать проверяемые исключения
                throw new RuntimeException("Не удалось загрузить изображение Developer (Car.png)", e);
            } catch (Exception e) {
                // Ловим другие возможные ошибки при загрузке изображения
                throw new RuntimeException("Непредвиденная ошибка при загрузке изображения Developer", e);
            }
        }
    }

    // Конструктор
    public Developer(int x, int y) throws FileNotFoundException {
        super(); // Вызов конструктора базового класса Person
        loadImage(); // Убедимся, что изображение загружено
        if (image == null) {
            // Эта ошибка теоретически не должна возникать из-за RuntimeException в loadImage, но оставим для ясности
            throw new FileNotFoundException("Изображение Developer не может быть загружено.");
        }
        // Используем recreateImageView для создания и настройки ImageView
        recreateImageView(x, y);
        // Сохраняем начальные координаты в полях Person
        this.currentX = x;
        this.currentY = y;
        count++; // Инкремент общего счетчика
        spawnedCount++; // Инкремент счетчика за сессию
    }

    // Геттер для ImageView
    @Override
    public ImageView getImageView() {
        return imageView;
    }

    // Метод воссоздания ImageView (используется при десериализации и в конструкторе)
    @Override
    public void recreateImageView(double x, double y) {
        loadImage(); // Убедимся, что статическое изображение загружено
        if (image == null) {
            System.err.println("Невозможно воссоздать ImageView: Developer image is null.");
            return; // Не можем создать ImageView без изображения
        }
        // Создаем и настраиваем ImageView
        imageView = new ImageView(image);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        // Не устанавливаем currentX/Y здесь, так как они должны быть уже установлены
        // либо в конструкторе, либо после десериализации перед вызовом этого метода.
    }

    // Сеттер для статического поля времени жизни
    public static void setLifeTime(int lifeTime) {
        Developer.lifeTime = lifeTime;
    }

    // Геттер для статического поля времени жизни
    public static int getLifeTime() {
        return lifeTime;
    }

    // Метод moveTo наследуется от Person и не требует переопределения здесь,
    // так как базовая реализация обновляет currentX/Y и перемещает imageView.
}