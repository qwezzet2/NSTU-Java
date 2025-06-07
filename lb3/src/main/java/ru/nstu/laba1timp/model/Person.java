// ru.nstu.laba1timp.model.Person.java
package ru.nstu.laba1timp.model;

import ru.nstu.laba1timp.Habitat;
import javafx.scene.image.ImageView;
import java.util.Random;

public abstract class Person implements IBehaviour {
    protected ImageView imageIV;
    protected int id;

    public Person() {
        Habitat hab = Habitat.getInstance();
        Random rand = new Random();
        do {
            id = rand.nextInt(100000, 1000000);
        } while (hab.getIdCollection().contains(id));
    }

    public abstract ImageView getImageView();
    public int getId() {
        return id;
    }
}