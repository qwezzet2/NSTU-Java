// ru.nstu.laba1timp.model.Developer.java
package ru.nstu.laba1timp.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Developer extends Person {
    public static int count = 0;
    public static int spawnedCount = 0;
    static Image image;
    private static int lifeTime;

    static {
        try {
            image = new Image(new FileInputStream("src/main/resources/Car.png"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Developer(int x, int y) throws FileNotFoundException {
        super();
        imageIV = new ImageView(image);
        imageIV.setX(x);
        imageIV.setY(y);
        imageIV.setFitWidth(80);
        imageIV.setFitHeight(80);
        imageIV.setPreserveRatio(true);
        count++;
        spawnedCount++;
    }

    @Override
    public ImageView getImageView() {
        return imageIV;
    }

    public static void setLifeTime(int lifeTime) {
        Developer.lifeTime = lifeTime;
    }

    public static int getLifeTime() {
        return lifeTime;
    }
}