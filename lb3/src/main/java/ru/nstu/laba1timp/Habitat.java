package ru.nstu.laba1timp;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;
import java.io.FileNotFoundException;
import java.util.*;

public class Habitat {
    private int width = 1300;
    private int height = 600;
    public int n1 = 1;
    public int n2 = 2;
    public float p1 = 0.8f;
    public float p2 = 0.4f;
    public int maxManagerPercent = 40;

    private LinkedList<Person> objCollection;
    private HashMap<Integer, Integer> bornCollection;
    private TreeSet<Integer> idCollection;

    private static Habitat instance;

    private Habitat() {
        objCollection = new LinkedList<>();
        bornCollection = new HashMap<>();
        idCollection = new TreeSet<>();
    }

    public static Habitat getInstance() {
        if (instance == null) {
            instance = new Habitat();
        }
        return instance;
    }

    public LinkedList<Person> getObjCollection() {
        return objCollection;
    }

    public HashMap<Integer, Integer> getBornCollection() {
        return bornCollection;
    }

    public TreeSet<Integer> getIdCollection() {
        return idCollection;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void update() {
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        int time = st.getTime();
        float p = rand.nextFloat();

        try {
            // Удаление объектов с истекшим временем жизни
            Iterator<Person> iterator = objCollection.iterator();
            while (iterator.hasNext()) {
                Person obj = iterator.next();
                int id = obj.getId();
                int lifeTime = (obj instanceof Developer) ? Developer.getLifeTime() : Manager.getLifeTime();

                if (bornCollection.get(id) + lifeTime <= time) {
                    st.getMainController().getPane().getChildren().remove(obj.getImageView());
                    iterator.remove();
                    bornCollection.remove(id);
                    idCollection.remove(id);
                }
            }

            // Генерация разработчиков
            if ((time % n1 == 0) && (p <= p1)) {
                Developer dev = new Developer(rand.nextInt(0, width - 80), rand.nextInt(0, height - 80));
                st.getMainController().getPane().getChildren().add(dev.getImageView());
                objCollection.add(dev);
                bornCollection.put(dev.getId(), time);
                idCollection.add(dev.getId());
            }

            // Генерация менеджеров с учетом процента
            if ((time % n2 == 0) && (p <= p2)) {
                int managers = Manager.count;
                int developers = Developer.count;
                if (developers == 0 || (managers * 100 / developers) < maxManagerPercent) {
                    Manager manager = new Manager(rand.nextInt(0, width - 180), rand.nextInt(0, height - 140));
                    st.getMainController().getPane().getChildren().add(manager.getImageView());
                    objCollection.add(manager);
                    bornCollection.put(manager.getId(), time);
                    idCollection.add(manager.getId());
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void clearObjects() {
        objCollection.clear();
        bornCollection.clear();
        idCollection.clear();
        Developer.count = 0;
        Manager.count = 0;
        Developer.spawnedCount = 0;
        Manager.spawnedCount = 0;
    }
}