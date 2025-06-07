package common;

import java.io.Serializable;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javafx.scene.image.Image; // Добавляем импорт
import javafx.scene.image.ImageView; // Добавляем импорт

public class Artwork implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient ImageView imageView;
    private int id;
    private String title;
    private String author;
    private String type;
    private int year;
    private String imageBase64;
    private String userType; // Убрали userId, так как используем userType

    public Artwork(int id, String title, String author, String type, int year,
                   String imageBase64, String userType) {
        this.id = id;
        this.title = title == null ? "" : title.trim();
        this.author = author == null ? "" : author.trim();
        this.type = type == null ? "" : type.trim();
        this.year = year;
        this.imageBase64 = imageBase64;
        this.userType = userType;
    }

    public ImageView getImageView() {
        if (imageView == null && imageBase64 != null) {
            try {
                byte[] imageData = Base64.getDecoder().decode(imageBase64);
                Image image = new Image(new ByteArrayInputStream(imageData));
                imageView = new ImageView(image);
                imageView.setFitWidth(50);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
            } catch (Exception e) {
                System.err.println("Ошибка создания ImageView: " + e.getMessage());
                return null;
            }
        }
        return imageView;
    }


    // Геттеры (убрали getUserId(), так как используем getUserType())
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getType() { return type; }
    public int getYear() { return year; }
    public String getImageBase64() { return imageBase64; }
    public String getUserType() { return userType; }

    @Override
    public String toString() {
        return "ID: " + id + ", Название: " + title + ", Автор: " + author +
                ", Тип: " + type + ", Год: " + year + ", Пользователь: " + userType;
    }
}