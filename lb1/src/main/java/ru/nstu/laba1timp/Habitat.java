package ru.nstu.laba1timp;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;

public class Habitat {
    private int width = 1000; // Ширина области визуализации
    private int height = 600; // Высота области визуализации
    private int n1 = 1; // Интервал генерации разработчиков (в секундах)
    private int n2 = 2; // Интервал генерации менеджеров (в секундах)
    private float p1 = 0.8f; // Вероятность генерации разработчиков
    private float p2 = 0.7f; // Вероятность генерации менеджеров
    private int k = 40; // Максимальный процент менеджеров от разработчиков

    private ArrayList<Person> array = new ArrayList<>(); // Коллекция для хранения объектов
    private static Habitat instance;

    // Приватный конструктор для Singleton
    private Habitat() {}

    // Установка экземпляра Habitat
    public static void setInstance(Habitat instance) {
        Habitat.instance = instance;
    }

    // Получение экземпляра Habitat (Singleton)
    public static Habitat getInstance() {
        if (instance == null) {
            instance = new Habitat();
        }
        return instance;
    }

    // Получение ширины области визуализации
    public int getWidth() {
        return width;
    }

    // Получение высоты области визуализации
    public int getHeight() {
        return height;
    }

    // Получение списка объектов
    public ArrayList<Person> getArray() {
        return array;
    }

    // Обновление состояния симуляции
    public void update(long time) {
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        float p = rand.nextFloat();

        try {
            // Генерация разработчиков
            if ((time % n1 == 0) && (p <= p1)) {
                Developer dev = new Developer(rand.nextInt(0, width - 40), rand.nextInt(0, height - 40));
                st.getMainController().getPane().getChildren().add(dev.getImageView());
                array.add(dev);
            }

            // Генерация менеджеров (только если их количество меньше K% от разработчиков)
            if ((time % n2 == 0) && (p <= p2)) {
                int managerCount = Manager.count;
                int developerCount = Developer.count;
                if (managerCount < (developerCount * k / 100)) {
                    Manager manager = new Manager(rand.nextInt(0, width - 40), rand.nextInt(0, height - 40));
                    st.getMainController().getPane().getChildren().add(manager.getImageView());
                    array.add(manager);
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    // Очистка списка объектов
    public void clearObjects() {
        array.clear();
        Developer.count = 0; // Сброс счетчика разработчиков
        Manager.count = 0; // Сброс счетчика менеджеров
    }
}