package client;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import common.Artwork;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;  // Добавлен этот импорт
import java.util.Set;
import javafx.beans.property.SimpleObjectProperty;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class ArtworkController {
    @FXML private Label rowCountLabel;
    @FXML private TableView<Artwork> table;
    @FXML private TableColumn<Artwork, Integer> colId;
    @FXML private TableColumn<Artwork, String> colTitle;
    @FXML private TableColumn<Artwork, String> colAuthor;
    @FXML private TableColumn<Artwork, String> colType;
    @FXML private TableColumn<Artwork, Integer> colYear;
    @FXML private TableColumn<Artwork, ImageView> colImage;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField typeField;
    @FXML private TextField yearField;
    @FXML private ImageView imagePreview;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label refreshStatusLabel;
    @FXML private Label onlineUsersLabel;

    private Timeline refreshTimeline;
    private String userType = "";
    private boolean isAdmin = false;
    private ArtworkModel model;
    private String currentImageBase64;

    public ArtworkController() {
    }

    public void initData(String userType, boolean isAdmin) {
        this.userType = userType;
        this.isAdmin = isAdmin;
    }

    @FXML
    public void initialize() {
        try {
            model = new ArtworkModel();
            refreshStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

            // Настройка столбцов
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
            colType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colYear.setCellValueFactory(new PropertyValueFactory<>("year"));

            colImage.setCellValueFactory(cellData -> {
                Artwork artwork = cellData.getValue();
                return new SimpleObjectProperty<>(artwork.getImageView());
            });

            table.setFixedCellSize(35); // Фиксированная высота строки

            model.getArtworks().addListener((ListChangeListener<Artwork>) c -> {
                rowCountLabel.setText("Записей: " + table.getItems().size());
            });
            table.setRowFactory(tv -> new TableRow<Artwork>() {
                @Override
                protected void updateItem(Artwork item, boolean empty) {
                    super.updateItem(item, empty);
                    setPrefHeight(40);  // Высота каждой строки
                }
            });
            colImage.setCellFactory(column -> new TableCell<>() {
                private final ImageView imageView = new ImageView();
                {
                    imageView.setFitWidth(50);
                    imageView.setFitHeight(50);
                    imageView.setPreserveRatio(true);
                }

                @Override
                protected void updateItem(ImageView item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        imageView.setImage(item.getImage());
                        setGraphic(imageView);
                    }
                }
            });

            editButton.setDisable(!isAdmin);
            deleteButton.setDisable(!isAdmin);

            table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    if (isAdmin) {
                        editButton.setDisable(false);
                        deleteButton.setDisable(false);
                        titleField.setText(newSelection.getTitle());
                        authorField.setText(newSelection.getAuthor());
                        typeField.setText(newSelection.getType());
                        yearField.setText(String.valueOf(newSelection.getYear()));
                        if (newSelection.getImageBase64() != null) {
                            currentImageBase64 = newSelection.getImageBase64();
                            imagePreview.setImage(newSelection.getImageView().getImage());
                        }
                    }
                } else {
                    editButton.setDisable(!isAdmin);
                    deleteButton.setDisable(!isAdmin);
                    clearFields();
                }
            });


            setupAutoRefresh();
            table.setItems(model.getArtworks());

            table.setRowFactory(tv -> {
                TableRow<Artwork> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        Artwork artwork = row.getItem();
                        showArtworkDetail(artwork);
                    }
                });
                return row;
            });

// Новый метод для показа деталей

        } catch (IOException e) {
            showError("Ошибка подключения", "Не удалось подключиться к серверу");
        }
        Platform.runLater(() -> {
            colId.setPrefWidth(50);
            colTitle.setPrefWidth(200);
            colAuthor.setPrefWidth(150);
            colType.setPrefWidth(150);
            colYear.setPrefWidth(80);
            colImage.setPrefWidth(120);
        });
    }
    private void showArtworkDetail(Artwork artwork) {
        try {
            InputStream fxmlStream = getClass().getResourceAsStream("/fxml/artwork_detail.fxml");
            if (fxmlStream == null) {
                throw new IOException("Не удалось найти файл artwork_detail.fxml");
            }
            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(fxmlStream);

            ArtworkDetailController controller = loader.getController();
            controller.setArtwork(artwork);

            Stage stage = new Stage();
            controller.setStage(stage);

            stage.setTitle("Детали произведения: " + artwork.getTitle());
            stage.setScene(new Scene(root));
            stage.setMinWidth(450); // Минимальная ширина
            stage.setMinHeight(650); // Минимальная высота
            stage.show();
        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть детали произведения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupAutoRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> {
                    try {
                        // Сохраняем текущие значения полей и состояние выбора
                        Artwork selected = table.getSelectionModel().getSelectedItem();
                        int selectedId = selected != null ? selected.getId() : -1;

                        String title = titleField.getText();
                        String author = authorField.getText();
                        String type = typeField.getText();
                        String year = yearField.getText();
                        String image = currentImageBase64;

                        model.refresh();
                        refreshStatusLabel.setText("Обновлено: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        updateOnlineUsers();

                        // Восстанавливаем выбор, если он был
                        if (selectedId != -1) {
                            for (Artwork artwork : table.getItems()) {
                                if (artwork.getId() == selectedId) {
                                    table.getSelectionModel().select(artwork);
                                    break;
                                }
                            }
                        }

                        // Восстанавливаем значения полей только если ничего не изменялось
                        if (selected != null && selected.getId() == selectedId) {
                            titleField.setText(title);
                            authorField.setText(author);
                            typeField.setText(type);
                            yearField.setText(year);
                            currentImageBase64 = image;

                            // Обновляем превью изображения, если оно было
                            if (image != null) {
                                try {
                                    byte[] imageData = Base64.getDecoder().decode(image);
                                    imagePreview.setImage(new Image(new ByteArrayInputStream(imageData)));
                                } catch (Exception e) {
                                    System.err.println("Ошибка восстановления изображения: " + e.getMessage());
                                }
                            }
                        }
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            refreshStatusLabel.setText("Ошибка соединения с сервером");
                            refreshStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

                            // Показываем уведомление только при первом обнаружении ошибки
                            if (!refreshStatusLabel.getText().contains("Ошибка")) {
                                showError("Ошибка соединения",
                                        "Не удалось подключиться к серверу. Проверьте, что сервер запущен и попробуйте снова.\n\n" +
                                                "Детали: " + e.getMessage());
                            }
                        });
                    }
                })
        );
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
        updateOnlineUsers();
    }

    private void updateOnlineUsers() {
        try {
            Set<String> activeUsers = model.getClient().getActiveUsers();

            // Добавляем текущего пользователя, если он еще не в списке
            if (!activeUsers.contains(userType)) {
                activeUsers.add(userType);
            }

            StringBuilder sb = new StringBuilder("Сейчас на сервере: (");
            sb.append(activeUsers.size()).append(") ");
            sb.append(String.join(" ", activeUsers));

            Platform.runLater(() -> onlineUsersLabel.setText(sb.toString()));
        } catch (Exception e) {
            Platform.runLater(() -> onlineUsersLabel.setText("Ошибка получения данных о пользователях"));
        }
    }

    @FXML
    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(imagePreview.getScene().getWindow());
        if (file != null) {
            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                currentImageBase64 = Base64.getEncoder().encodeToString(fileContent);
                imagePreview.setImage(new Image(file.toURI().toString()));
            } catch (Exception e) {
                showError("Ошибка", "Не удалось загрузить изображение");
            }
        }
    }

    private void clearFields() {
        titleField.clear();
        authorField.clear();
        typeField.clear();
        yearField.clear();
        imagePreview.setImage(null);
        currentImageBase64 = null;
    }

    @FXML
    public void addArtwork() {
        try {
            Artwork artwork = new Artwork(
                    0,
                    titleField.getText(),
                    authorField.getText(),
                    typeField.getText(),
                    Integer.parseInt(yearField.getText()),
                    currentImageBase64,
                    userType
            );
            model.add(artwork);
            clearFields();
        } catch (Exception e) {
            showError("Ошибка", "Не удалось добавить произведение");
        }
    }

    @FXML
    public void editArtwork() {
        Artwork selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            Artwork edited = new Artwork(
                    selected.getId(),
                    titleField.getText(),
                    authorField.getText(),
                    typeField.getText(),
                    Integer.parseInt(yearField.getText()),
                    currentImageBase64 != null ? currentImageBase64 : selected.getImageBase64(),
                    userType
            );
            model.update(edited);
        } catch (Exception e) {
            showError("Ошибка", "Не удалось обновить произведение");
        }
    }

    @FXML
    public void deleteArtwork() {
        Artwork selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                model.delete(selected);
            } catch (Exception e) {
                showError("Ошибка", "Не удалось удалить произведение");
            }
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void stopRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}