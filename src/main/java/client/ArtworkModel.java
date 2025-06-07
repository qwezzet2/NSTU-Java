package client;

import common.Artwork;
import common.Request;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;

public class ArtworkModel {
    private ObservableList<Artwork> artworks = FXCollections.observableArrayList();
    private Client client;
    private boolean connectionError = false;

    public ArtworkModel() throws IOException {
        client = new Client();
        refresh();
    }

    public void refresh() throws IOException {
        if (connectionError) {
            throw new IOException("Нет соединения с сервером");
        }

        try {
            List<Artwork> newArtworks = client.getAllArtworks();
            artworks.setAll(newArtworks);
        } catch (Exception e) {
            connectionError = true;
            client.close();
            throw new IOException("Ошибка при обновлении данных: " + e.getMessage(), e);
        }
    }

    public void add(Artwork artwork) throws Exception {
        client.addArtwork(artwork);
        refresh();
    }

    public void update(Artwork artwork) throws Exception {
        client.updateArtwork(artwork);
        refresh();
    }

    public void delete(Artwork artwork) throws Exception {
        client.deleteArtwork(artwork);
        refresh();
    }

    public List<Artwork> getAllArtworksFromServer() throws Exception {
        return client.getAllArtworks();
    }

    public ObservableList<Artwork> getArtworks() {
        return artworks;
    }
    public Client getClient() {
        return client;
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }
}