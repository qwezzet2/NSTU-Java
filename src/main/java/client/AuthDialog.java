package client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import common.Artwork;
import javafx.application.Platform;

public class AuthDialog extends Application {
    private static String userType;
    private static Set<String> existingUsers = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Авторизация в системе");
        primaryStage.setResizable(false);

        // Создаем основной контейнер с градиентным фоном
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #4776E6, #8E54E9);");

        // Заголовок
        Label titleLabel = new Label("Добро пожаловать!");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);

        // Подзаголовок
        Label subtitleLabel = new Label("Выберите тип пользователя:");
        subtitleLabel.setFont(Font.font("Arial", 16));
        subtitleLabel.setTextFill(Color.WHITE);

        // Группа переключателей
        ToggleGroup group = new ToggleGroup();
        VBox radioBox = new VBox(15);
        radioBox.setAlignment(Pos.CENTER_LEFT);

        RadioButton adminRadio = createStyledRadioButton("Администратор", group);
        RadioButton userRadio = createStyledRadioButton("Обычный пользователь", group);
        radioBox.getChildren().addAll(adminRadio, userRadio);

        // Поле для пароля
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Введите пароль администратора");
        passwordField.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 10; -fx-font-size: 14; -fx-background-radius: 5;");
        passwordField.setDisable(true);
        passwordField.setMaxWidth(250);

        adminRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            passwordField.setDisable(!newVal);
        });

        // Кнопка входа
        Button submitBtn = new Button("Войти");
        submitBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 10 25; -fx-background-radius: 5; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> handleLogin(primaryStage, adminRadio, userRadio, passwordField));

        // Добавляем все элементы в основной контейнер
        mainLayout.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                radioBox,
                passwordField,
                submitBtn
        );

        // Создаем сцену и настраиваем Stage
        Scene scene = new Scene(mainLayout, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private RadioButton createStyledRadioButton(String text, ToggleGroup group) {
        RadioButton radio = new RadioButton(text);
        radio.setToggleGroup(group);
        radio.setFont(Font.font("Arial", 14));
        radio.setTextFill(Color.WHITE);
        radio.setStyle("-fx-padding: 5 0;");
        return radio;
    }

    private void handleLogin(Stage primaryStage, RadioButton adminRadio, RadioButton userRadio, PasswordField passwordField) {
        try (Client tempClient = new Client()) {
            List<Artwork> artworks = tempClient.getAllArtworks();

            existingUsers.clear();
            for (Artwork artwork : artworks) {
                existingUsers.add(artwork.getUserType());
            }

            if (adminRadio.isSelected()) {
                if (!"123".equals(passwordField.getText())) {
                    showAlert("Ошибка", "Неверный пароль администратора!");
                    return;
                }
                userType = generateUniqueUserType("ADM", existingUsers);
            } else if (userRadio.isSelected()) {
                userType = generateUniqueUserType("PERS", existingUsers);
            } else {
                showAlert("Ошибка", "Выберите тип пользователя!");
                return;
            }

            primaryStage.close();
            Platform.runLater(() -> {
                try {
                    new MainApp(userType, adminRadio.isSelected()).start(new Stage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                showAlert("Ошибка подключения",
                        "Не удалось подключиться к серверу:\n\n" +
                                e.getMessage() + "\n\n" +
                                "Пожалуйста, убедитесь, что сервер запущен, и попробуйте снова.");
            });
            e.printStackTrace();
        } catch (Exception ex) {
            Platform.runLater(() -> {
                showAlert("Ошибка", "Ошибка при запуске приложения: " + ex.getMessage());
            });
            ex.printStackTrace();
        }
    }
    private String generateUniqueUserType(String prefix, Set<String> existingUsers) {
        int counter = 0;
        String newUserType;
        do {
            newUserType = prefix + counter;
            counter++;
        } while (existingUsers.contains(newUserType));
        return newUserType;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Стилизация алерта
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #f8f9fa;");
        dialogPane.setHeader(null);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}