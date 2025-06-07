package ru.nstu.laba1timp.controllers;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;

import java.util.*;

/**
 * Класс Habitat представляет собой "среду обитания" объектов Person (Developer и Manager).
 * Он отвечает за создание, удаление и обновление объектов на основе времени и параметров.
 */
public class Habitat {
    private int width = 1090;  // Ширина области визуализации
    private int height = 680;  // Высота области визуализации

    public int n1 = 1;          // Период появления Developer (в секундах)
    public int n2 = 2;          // Период появления Manager (в секундах)
    public float p1 = 0.8f;     // Вероятность появления Developer при каждом вызове
    public float p2 = 0.4f;     // Вероятность появления Manager при каждом вызове
    public int maxManagerPercent = 40;  // Максимальный процент менеджеров относительно разработчиков

    // Коллекции для хранения объектов Person, их времени рождения и ID
    private final LinkedList<Person> objCollection;
    private final HashMap<Integer, Integer> bornCollection;
    private final TreeSet<Integer> idCollection;

    private static Habitat instance;

    /**
     * Приватный конструктор для реализации шаблона Singleton.
     */
    private Habitat() {
        objCollection = new LinkedList<>();
        bornCollection = new HashMap<>();
        idCollection = new TreeSet<>();
    }

    /**
     * Возвращает единственный экземпляр класса Habitat (Singleton).
     */
    public static synchronized Habitat getInstance() {
        if (instance == null) {
            instance = new Habitat();
        }
        return instance;
    }

    // Геттеры для коллекций
    public LinkedList<Person> getObjCollection() { return objCollection; }
    public HashMap<Integer, Integer> getBornCollection() { return bornCollection; }
    public TreeSet<Integer> getIdCollection() { return idCollection; }

    // Геттеры для размеров области визуализации
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /**
     * Обновляет состояние среды: удаляет "умерших" объектов,
     * создаёт новых Developer и Manager согласно заданным условиям.
     */
    public void update() {
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        Controller controller = (st != null) ? st.getMainController() : null;

        if (controller == null || controller.getPane() == null) {
            System.err.println("Habitat.update(): Ошибка - Контроллер или его панель не инициализированы.");
            return;
        }

        int currentTime = st.getTime();

        // 1. Удаление "умерших" объектов
        synchronized (objCollection) {
            synchronized (bornCollection) {
                synchronized (idCollection) {
                    List<ImageView> viewsToRemoveFromPane = new ArrayList<>();
                    Iterator<Person> iterator = objCollection.iterator();

                    while (iterator.hasNext()) {
                        Person obj = iterator.next();
                        int personId = obj.getId();
                        int personLifeTime = (obj instanceof Developer) ? Developer.getLifeTime() : Manager.getLifeTime();

                        if (bornCollection.containsKey(personId) && (bornCollection.get(personId) + personLifeTime <= currentTime)) {
                            if (obj.getImageView() != null) {
                                viewsToRemoveFromPane.add(obj.getImageView());
                            }

                            iterator.remove();
                            bornCollection.remove(personId);
                            idCollection.remove(personId);
                        }
                    }

                    if (!viewsToRemoveFromPane.isEmpty()) {
                        Platform.runLater(() -> controller.getPane().getChildren().removeAll(viewsToRemoveFromPane));
                    }
                }
            }
        }

        // 2. Генерация новых Developer
        if ((currentTime > 0 && currentTime % n1 == 0) && (rand.nextFloat() <= p1)) {
            try {
                int spawnX = rand.nextInt(0, Math.max(1, width - 80));
                int spawnY = rand.nextInt(0, Math.max(1, height - 80));

                Developer dev = new Developer(spawnX, spawnY);

                synchronized (objCollection) {
                    synchronized (bornCollection) {
                        synchronized (idCollection) {
                            if (!idCollection.contains(dev.getId())) {
                                objCollection.add(dev);
                                bornCollection.put(dev.getId(), currentTime);
                                idCollection.add(dev.getId());

                                if (dev.getImageView() != null) {
                                    final ImageView devView = dev.getImageView();
                                    Platform.runLater(() -> controller.getPane().getChildren().add(devView));
                                }
                            } else {
                                System.err.println("Habitat.update(): Сгенерирован Developer с уже существующим ID: " + dev.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Habitat.update(): Ошибка создания Developer: " + e.getMessage());
            }
        }

        // 3. Генерация новых Manager
        if ((currentTime > 0 && currentTime % n2 == 0) && (rand.nextFloat() <= p2)) {
            long currentDevelopersCount;
            long currentManagersCount;

            synchronized (objCollection) {
                currentDevelopersCount = objCollection.stream().filter(Developer.class::isInstance).count();
                currentManagersCount = objCollection.stream().filter(Manager.class::isInstance).count();
            }

            if (currentDevelopersCount == 0 || (currentManagersCount * 100.0 / currentDevelopersCount) < maxManagerPercent) {
                try {
                    Manager manager = new Manager(0, 0);

                    if (manager.getImageView() != null) {
                        double paneWidth = getWidth();
                        double paneHeight = getHeight();
                        double objectWidth = manager.getImageView().getFitWidth();
                        double objectHeight = manager.getImageView().getFitHeight();
                        double radius = 100;

                        double maxCenterX = paneWidth - radius - objectWidth / 2;
                        double maxCenterY = paneHeight - radius - objectHeight / 2;
                        double minCenterX = radius + objectWidth / 2;
                        double minCenterY = radius + objectHeight / 2;

                        if (minCenterX > maxCenterX) minCenterX = maxCenterX = paneWidth / 2;
                        if (minCenterY > maxCenterY) minCenterY = maxCenterY = paneHeight / 2;

                        if (maxCenterX >= minCenterX && maxCenterY >= minCenterY) {
                            manager.circleCenterX = minCenterX + rand.nextDouble() * (maxCenterX - minCenterX);
                            manager.circleCenterY = minCenterY + rand.nextDouble() * (maxCenterY - minCenterY);
                        } else {
                            manager.circleCenterX = paneWidth / 2;
                            manager.circleCenterY = paneHeight / 2;
                        }

                        manager.angle = rand.nextDouble() * 2 * Math.PI;

                        double initialX = manager.circleCenterX + radius * Math.cos(manager.angle) - objectWidth / 2;
                        double initialY = manager.circleCenterY + radius * Math.sin(manager.angle) - objectHeight / 2;

                        manager.moveTo(initialX, initialY);
                    }

                    synchronized (objCollection) {
                        synchronized (bornCollection) {
                            synchronized (idCollection) {
                                if (!idCollection.contains(manager.getId())) {
                                    objCollection.add(manager);
                                    bornCollection.put(manager.getId(), currentTime);
                                    idCollection.add(manager.getId());

                                    if (manager.getImageView() != null) {
                                        final ImageView manView = manager.getImageView();
                                        Platform.runLater(() -> controller.getPane().getChildren().add(manView));
                                    }
                                } else {
                                    System.err.println("Habitat.update(): Сгенерирован Manager с уже существующим ID: " + manager.getId());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Habitat.update(): Ошибка создания Manager: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Полностью очищает среду от всех объектов.
     */
    public void clearObjects() {
        List<ImageView> viewsToRemoveFromPane = new ArrayList<>();

        synchronized (objCollection) {
            for (Person p : objCollection) {
                if (p.getImageView() != null) {
                    viewsToRemoveFromPane.add(p.getImageView());
                }
            }
            objCollection.clear();
        }

        synchronized (bornCollection) {
            bornCollection.clear();
        }

        synchronized (idCollection) {
            idCollection.clear();
        }

        if (!viewsToRemoveFromPane.isEmpty()) {
            Platform.runLater(() -> {
                Statistics st = Statistics.getInstance();
                Controller ctrl = (st != null) ? st.getMainController() : null;

                if (ctrl != null && ctrl.getPane() != null) {
                    ctrl.getPane().getChildren().removeAll(viewsToRemoveFromPane);
                } else {
                    System.err.println("Habitat.clearObjects(): Не удалось очистить панель визуализации (контроллер или панель null).");
                }
            });
        }

        Developer.count = 0;
        Developer.spawnedCount = 0;
        Manager.count = 0;
        Manager.spawnedCount = 0;

        System.out.println("Habitat полностью очищен.");
    }

    /**
     * Удаляет все объекты типа Manager.
     *
     * @return количество удалённых менеджеров
     */
    public int removeAllManagers() {
        int removedCount = 0;
        List<Person> managersToRemove = new ArrayList<>();
        List<ImageView> viewsToRemoveFromPane = new ArrayList<>();

        synchronized (objCollection) {
            objCollection.removeIf(p -> {
                if (p instanceof Manager) {
                    managersToRemove.add(p);
                    if (p.getImageView() != null) {
                        viewsToRemoveFromPane.add(p.getImageView());
                    }
                    return true;
                }
                return false;
            });
        }

        if (!managersToRemove.isEmpty()) {
            synchronized (bornCollection) {
                synchronized (idCollection) {
                    for (Person manager : managersToRemove) {
                        bornCollection.remove(manager.getId());
                        idCollection.remove(manager.getId());
                        removedCount++;
                    }
                }
            }

            if (!viewsToRemoveFromPane.isEmpty()) {
                Platform.runLater(() -> {
                    Statistics st = Statistics.getInstance();
                    Controller ctrl = (st != null) ? st.getMainController() : null;

                    if (ctrl != null && ctrl.getPane() != null) {
                        ctrl.getPane().getChildren().removeAll(viewsToRemoveFromPane);
                    }
                });
            }
        }

        if (removedCount > 0) {
            System.out.println("Удалено менеджеров: " + removedCount);
        }

        return removedCount;
    }

    /**
     * Удаляет все объекты типа Developer.
     *
     * @return количество удалённых разработчиков
     */
    public int removeAllDevelopers() {
        int removedCount = 0;
        List<Person> developersToRemove = new ArrayList<>();
        List<ImageView> viewsToRemoveFromPane = new ArrayList<>();

        synchronized (objCollection) {
            objCollection.removeIf(p -> {
                if (p instanceof Developer) {
                    developersToRemove.add(p);
                    if (p.getImageView() != null) {
                        viewsToRemoveFromPane.add(p.getImageView());
                    }
                    return true;
                }
                return false;
            });
        }

        if (!developersToRemove.isEmpty()) {
            synchronized (bornCollection) {
                synchronized (idCollection) {
                    for (Person dev : developersToRemove) {
                        bornCollection.remove(dev.getId());
                        idCollection.remove(dev.getId());
                        removedCount++;
                    }
                }
            }

            if (!viewsToRemoveFromPane.isEmpty()) {
                Platform.runLater(() -> {
                    Statistics st = Statistics.getInstance();
                    Controller ctrl = (st != null) ? st.getMainController() : null;

                    if (ctrl != null && ctrl.getPane() != null) {
                        ctrl.getPane().getChildren().removeAll(viewsToRemoveFromPane);
                    }
                });
            }
        }

        if (removedCount > 0) {
            System.out.println("Удалено разработчиков: " + removedCount);
        }

        return removedCount;
    }

    /**
     * Создаёт указанное количество новых Manager.
     *
     * @param n количество менеджеров для создания
     * @return фактически созданное количество
     */
    public int spawnManagers(int n) {
        int spawned = 0;
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        Controller controller = (st != null) ? st.getMainController() : null;

        if (controller == null || controller.getPane() == null) {
            System.err.println("Habitat.spawnManagers(): Ошибка - Контроллер или его панель не инициализированы.");
            return 0;
        }

        int currentTime = st.getTime();

        for (int i = 0; i < n; i++) {
            try {
                Manager manager = new Manager(0, 0);

                if (manager.getImageView() != null) {
                    double paneWidth = getWidth();
                    double paneHeight = getHeight();
                    double objectWidth = manager.getImageView().getFitWidth();
                    double objectHeight = manager.getImageView().getFitHeight();
                    double radius = 100;

                    double maxCenterX = paneWidth - radius - objectWidth / 2;
                    double maxCenterY = paneHeight - radius - objectHeight / 2;
                    double minCenterX = radius + objectWidth / 2;
                    double minCenterY = radius + objectHeight / 2;

                    if (minCenterX > maxCenterX) minCenterX = maxCenterX = paneWidth / 2;
                    if (minCenterY > maxCenterY) minCenterY = maxCenterY = paneHeight / 2;

                    if (maxCenterX >= minCenterX && maxCenterY >= minCenterY) {
                        manager.circleCenterX = minCenterX + rand.nextDouble() * (maxCenterX - minCenterX);
                        manager.circleCenterY = minCenterY + rand.nextDouble() * (maxCenterY - minCenterY);
                    } else {
                        manager.circleCenterX = paneWidth / 2;
                        manager.circleCenterY = paneHeight / 2;
                    }

                    manager.angle = rand.nextDouble() * 2 * Math.PI;

                    double initialX = manager.circleCenterX + radius * Math.cos(manager.angle) - objectWidth / 2;
                    double initialY = manager.circleCenterY + radius * Math.sin(manager.angle) - objectHeight / 2;

                    manager.moveTo(initialX, initialY);
                }

                synchronized (objCollection) {
                    synchronized (bornCollection) {
                        synchronized (idCollection) {
                            if (!idCollection.contains(manager.getId())) {
                                objCollection.add(manager);
                                bornCollection.put(manager.getId(), currentTime);
                                idCollection.add(manager.getId());

                                if (manager.getImageView() != null) {
                                    final ImageView manView = manager.getImageView();
                                    Platform.runLater(() -> controller.getPane().getChildren().add(manView));
                                }

                                spawned++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Habitat.spawnManagers(): Ошибка при создании Manager #" + (i+1) + ": " + e.getMessage());
            }
        }

        if (spawned > 0) {
            System.out.println("Создано менеджеров командой: " + spawned);
        }

        return spawned;
    }

    /**
     * Создаёт указанное количество новых Developer.
     *
     * @param n количество разработчиков для создания
     * @return фактически созданное количество
     */
    public int spawnDevelopers(int n) {
        int spawned = 0;
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        Controller controller = (st != null) ? st.getMainController() : null;

        if (controller == null || controller.getPane() == null) {
            System.err.println("Habitat.spawnDevelopers(): Ошибка - Контроллер или его панель не инициализированы.");
            return 0;
        }

        int currentTime = st.getTime();

        for (int i = 0; i < n; i++) {
            try {
                int spawnX = rand.nextInt(0, Math.max(1, width - 80));
                int spawnY = rand.nextInt(0, Math.max(1, height - 80));

                Developer dev = new Developer(spawnX, spawnY);

                synchronized (objCollection) {
                    synchronized (bornCollection) {
                        synchronized (idCollection) {
                            if (!idCollection.contains(dev.getId())) {
                                objCollection.add(dev);
                                bornCollection.put(dev.getId(), currentTime);
                                idCollection.add(dev.getId());

                                if (dev.getImageView() != null) {
                                    final ImageView devView = dev.getImageView();
                                    Platform.runLater(() -> controller.getPane().getChildren().add(devView));
                                }

                                spawned++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Habitat.spawnDevelopers(): Ошибка при создании Developer #" + (i+1) + ": " + e.getMessage());
            }
        }

        if (spawned > 0) {
            System.out.println("Создано разработчиков командой: " + spawned);
        }

        return spawned;
    }
}
