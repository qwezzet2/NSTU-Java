package common;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private Operation operation;
    private Artwork artwork;
    private List<Artwork> artworks;
    private Set<String> userTypes;

    public enum Operation {
        GET_ALL,
        ADD,
        DELETE,
        UPDATE,
        GET_ACTIVE_USERS
    }

    public Request(Operation operation) {
        this.operation = operation;
    }

    public Request(Operation operation, Artwork artwork) {
        this.operation = operation;
        this.artwork = artwork;
    }

    public void setUserTypes(Set<String> userTypes) {
        this.userTypes = userTypes;
    }

    public Set<String> getUserTypes() {
        return userTypes;
    }

    public Operation getOperation() {
        return operation;
    }

    public Artwork getArtwork() {
        return artwork;
    }

    public List<Artwork> getArtworks() {
        return artworks;
    }

    public void setArtworks(List<Artwork> artworks) {
        this.artworks = artworks;
    }
}