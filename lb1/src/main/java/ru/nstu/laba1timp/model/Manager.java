package ru.nstu.laba1timp.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Manager extends Person {
    public static int count = 0; // Счетчик созданных объектов
    static Image image; // Изображение для менеджера

    static {
        try {
            // Загрузка изображения из ресурсов
            image = new Image(new FileInputStream("src/main/resources/Stlb.png"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Manager(int x, int y) throws FileNotFoundException {
        super();
        imageIV = new ImageView(image);
        imageIV.setX(x);
        imageIV.setY(y);
        imageIV.setFitWidth(180);
        imageIV.setFitHeight(140);
        imageIV.setPreserveRatio(true);
        count++; // Увеличиваем счетчик при создании объекта
    }

    @Override
    public ImageView getImageView() {
        return imageIV;
    }
}