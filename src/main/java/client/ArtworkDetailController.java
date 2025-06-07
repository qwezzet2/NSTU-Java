package client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import common.Artwork;
import java.util.Base64;
import java.io.ByteArrayInputStream;

public class ArtworkDetailController {
    @FXML private ImageView detailImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private Label typeLabel;
    @FXML private Label yearLabel;
    @FXML private Label userTypeLabel;

    private Stage stage;

    public void setArtwork(Artwork artwork) {
        if (artwork.getImageBase64() != null) {
            byte[] imageData = Base64.getDecoder().decode(artwork.getImageBase64());
            detailImageView.setImage(new Image(new ByteArrayInputStream(imageData)));
        } else {
            detailImageView.setImage(null); // Гарантируем, что изображение очищено
        }

        titleLabel.setText("Название: " + artwork.getTitle());
        authorLabel.setText("Автор: " + artwork.getAuthor());
        typeLabel.setText("Тип: " + artwork.getType());
        yearLabel.setText("Год: " + artwork.getYear());
        userTypeLabel.setText("Добавлено пользователем: " + artwork.getUserType());

        // Автоматически подстраиваем размер окна под содержимое
        if (stage != null) {
            stage.sizeToScene();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleClose() {
        stage.close();
    }
}