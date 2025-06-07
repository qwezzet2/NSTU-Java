// ru.nstu.laba1timp.Habitat.java
package ru.nstu.laba1timp;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

// Основной класс, управляющий средой обитания объектов Developer и Manager
public class Habitat {
    // Ширина области моделирования
    private int width = 1090;
    // Высота области моделирования (скорректирована согласно FXML)
    private int height = 680; // Corrected height based on FXML

    // Параметры генерации объектов:
    public int n1 = 1; // Интервал генерации Developer (в тактах)
    public int n2 = 2; // Интервал генерации Manager (в тактах)
    public float p1 = 0.8f; // Вероятность появления Developer
    public float p2 = 0.4f; // Вероятность появления Manager
    public int maxManagerPercent = 40; // Максимальный процент Manager относительно Developer

    // Коллекции для хранения данных:
    private LinkedList<Person> objCollection; // Основная коллекция объектов
    private HashMap<Integer, Integer> bornCollection; // Время рождения объектов (ID -> время создания)
    private TreeSet<Integer> idCollection; // Множество уникальных идентификаторов

    // Реализация паттерна Singleton:
    private static Habitat instance;

    // Приватный конструктор
    private Habitat() {
        objCollection = new LinkedList<>();
        bornCollection = new HashMap<>();
        idCollection = new TreeSet<>();
    }

    // Метод для получения единственного экземпляра класса
    public static Habitat getInstance() {
        if (instance == null) {
            instance = new Habitat();
        }
        return instance;
    }

    // Геттеры для коллекций:
    public LinkedList<Person> getObjCollection() {
        return objCollection;
    }

    public HashMap<Integer, Integer> getBornCollection() {
        return bornCollection;
    }

    public TreeSet<Integer> getIdCollection() {
        return idCollection;
    }

    // Геттеры размеров области:
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // Основной метод обновления состояния среды
    public void update() {
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        int time = st.getTime(); // Текущее время моделирования
        float p = rand.nextFloat(); // Случайное число для проверки вероятности

        try {
            // Удаление объектов с истекшим сроком жизни:
            synchronized (objCollection) { // Синхронизация доступа к коллекции объектов
                Iterator<Person> iterator = objCollection.iterator();
                while (iterator.hasNext()) {
                    Person obj = iterator.next();
                    int id = obj.getId();
                    // Определение времени жизни в зависимости от типа объекта
                    int lifeTime = (obj instanceof Developer) ? Developer.getLifeTime() : Manager.getLifeTime();

                    synchronized (bornCollection) { // Синхронизация доступа к коллекции времени рождения
                        if (bornCollection.containsKey(id) && bornCollection.get(id) + lifeTime <= time) {
                            // Удаление графического представления из интерфейса
                            st.getMainController().getPane().getChildren().remove(obj.getImageView());
                            iterator.remove(); // Удаление объекта из коллекции
                            bornCollection.remove(id); // Удаление записи о времени рождения

                            synchronized (idCollection) { // Синхронизация доступа к коллекции ID
                                idCollection.remove(id); // Освобождение ID
                            }
                        }
                    }
                }
            }

            // Генерация новых Developer:
            if ((time % n1 == 0) && (p <= p1)) {
                try {
                    // Создание нового Developer в случайной позиции (с учетом границ)
                    Developer dev = new Developer(rand.nextInt(0, width - 80), rand.nextInt(0, height - 80));

                    synchronized (objCollection) {
                        // Добавление на графическую панель
                        st.getMainController().getPane().getChildren().add(dev.getImageView());
                        objCollection.add(dev); // Добавление в основную коллекцию

                        synchronized (bornCollection) {
                            bornCollection.put(dev.getId(), time); // Запись времени рождения
                        }

                        synchronized (idCollection) {
                            idCollection.add(dev.getId()); // Регистрация ID
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace(); // Обработка ошибки загрузки изображения
                }
            }

            // Генерация новых Manager с учетом ограничения по проценту:
            if ((time % n2 == 0) && (p <= p2)) {
                int managers;
                int developers;
                // Подсчет текущего количества Manager и Developer
                synchronized (objCollection) {
                    managers = (int) objCollection.stream().filter(obj -> obj instanceof Manager).count();
                    developers = (int) objCollection.stream().filter(obj -> obj instanceof Developer).count();
                }

                // Проверка процентного соотношения
                if (developers == 0 || (managers * 100 / developers) < maxManagerPercent) {
                    try {
                        // Создание Manager с временными координатами (0,0)
                        Manager manager = new Manager(0, 0);

                        // Инициализация параметров кругового движения:
                        double paneWidth = Habitat.getInstance().getWidth();
                        double paneHeight = Habitat.getInstance().getHeight();
                        double objectWidth = manager.getImageView().getFitWidth();
                        double objectHeight = manager.getImageView().getFitHeight();
                        double radius = 100; // Радиус окружности (как в ManagerAI)

                        // Расчет допустимых границ для центра окружности:
                        double maxCenterX = paneWidth - radius - objectWidth / 2;
                        double maxCenterY = paneHeight - radius - objectHeight / 2;
                        double minCenterX = radius + objectWidth / 2;
                        double minCenterY = radius + objectHeight / 2;

                        // Случайное размещение центра окружности
                        manager.circleCenterX = minCenterX + rand.nextDouble() * (maxCenterX - minCenterX);
                        manager.circleCenterY = minCenterY + rand.nextDouble() * (maxCenterY - minCenterY);
                        manager.angle = rand.nextDouble() * 2 * Math.PI; // Начальный угол

                        // Расчет начальной позиции на окружности
                        double initialX = manager.circleCenterX + radius * Math.cos(manager.angle) - objectWidth / 2;
                        double initialY = manager.circleCenterY + radius * Math.sin(manager.angle) - objectHeight / 2;

                        // Установка начальной позиции
                        manager.getImageView().setX(initialX);
                        manager.getImageView().setY(initialY);

                        // Добавление Manager в коллекции:
                        synchronized (objCollection) {
                            st.getMainController().getPane().getChildren().add(manager.getImageView());
                            objCollection.add(manager);

                            synchronized (bornCollection) {
                                bornCollection.put(manager.getId(), time);
                            }

                            synchronized (idCollection) {
                                idCollection.add(manager.getId());
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace(); // Обработка ошибки загрузки изображения
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace(); // Общая обработка исключений
        }
    }

    // Метод для полной очистки среды
    public void clearObjects() {
        // Очистка коллекций с синхронизацией:
        synchronized (objCollection) {
            objCollection.clear();
        }
        synchronized (bornCollection) {
            bornCollection.clear();
        }
        synchronized (idCollection) {
            idCollection.clear();
        }

        // Сброс счетчиков объектов:
        Developer.count = 0;
        Manager.count = 0;
        Developer.spawnedCount = 0;
        Manager.spawnedCount = 0;
    }
}