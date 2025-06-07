package ru.nstu.laba1timp.controllers; // <<< ИСПРАВЛЕН ПАКЕТ

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;
import javafx.application.Platform;
import javafx.scene.image.ImageView;

import java.io.FileNotFoundException;
import java.util.*;

public class Habitat {
    private int width = 1090;
    private int height = 680;
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
        if (st == null || st.getMainController() == null) {
            System.err.println("Статистика/контроллер не инициализированы в Habitat.update()");
            return;
        }
        int time = st.getTime();
        float p = rand.nextFloat();

        try {
            // Удаление объектов
            synchronized (objCollection) {
                Iterator<Person> iterator = objCollection.iterator();
                while (iterator.hasNext()) {
                    Person obj = iterator.next();
                    int id = obj.getId();
                    int lifeTime = (obj instanceof Developer) ? Developer.getLifeTime() : Manager.getLifeTime();
                    synchronized (bornCollection) {
                        if (bornCollection.containsKey(id) && bornCollection.get(id) + lifeTime <= time) {
                            final ImageView imageViewToRemove = obj.getImageView();
                            if (imageViewToRemove != null && st.getMainController() != null && st.getMainController().getPane() != null) {
                                Platform.runLater(() -> st.getMainController().getPane().getChildren().remove(imageViewToRemove));
                            }
                            iterator.remove();
                            bornCollection.remove(id);
                            synchronized (idCollection) {
                                idCollection.remove(id);
                            }
                        }
                    }
                }
            }

            // Генерация Developer
            if ((time % n1 == 0) && (p <= p1)) {
                try {
                    int spawnX = rand.nextInt(0, Math.max(1, width - 80));
                    int spawnY = rand.nextInt(0, Math.max(1, height - 80));
                    Developer dev = new Developer(spawnX, spawnY);
                    synchronized (objCollection) {
                        final ImageView devImageView = dev.getImageView();
                        if (devImageView != null && st.getMainController() != null && st.getMainController().getPane() != null) {
                            Platform.runLater(() -> st.getMainController().getPane().getChildren().add(devImageView));
                        } else {
                            System.err.println("ImageView для нового Developer == null");
                        }
                        objCollection.add(dev);
                        synchronized (bornCollection) {
                            bornCollection.put(dev.getId(), time);
                        }
                        synchronized (idCollection) {
                            idCollection.add(dev.getId());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка создания Developer: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Генерация Manager
            if ((time % n2 == 0) && (p <= p2)) {
                int managers;
                int developers;
                synchronized (objCollection) {
                    managers = (int) objCollection.stream().filter(obj -> obj instanceof Manager).count();
                    developers = (int) objCollection.stream().filter(obj -> obj instanceof Developer).count();
                }
                if (developers == 0 || ((double) managers * 100.0 / developers) < maxManagerPercent) {
                    try {
                        Manager manager = new Manager(0, 0);
                        final ImageView managerImageView = manager.getImageView();
                        if (managerImageView == null) {
                            System.err.println("Не удалось создать ImageView для нового Manager.");
                            // Используем continue, чтобы перейти к следующей итерации, если бы это было в цикле
                            // В данном случае можно просто return или пропустить блок добавления
                            return;
                        }
                        double paneWidth = getWidth();
                        double paneHeight = getHeight();
                        double objectWidth = managerImageView.getFitWidth();
                        double objectHeight = managerImageView.getFitHeight();
                        double radius = 100;
                        double maxCenterX = paneWidth - radius - objectWidth / 2;
                        double maxCenterY = paneHeight - radius - objectHeight / 2;
                        double minCenterX = radius + objectWidth / 2;
                        // <<< ИСПРАВЛЕНО: Объявление minCenterY ПЕРЕМЕЩЕНО СЮДА >>>
                        double minCenterY = radius + objectHeight / 2;

                        if (minCenterX > maxCenterX) minCenterX = maxCenterX = paneWidth / 2;
                        if (minCenterY > maxCenterY) minCenterY = maxCenterY = paneHeight / 2;

                        // Установка центра круга
                        if (maxCenterX >= minCenterX && maxCenterY >= minCenterY) { // <<< Теперь minCenterY объявлена
                            manager.circleCenterX = minCenterX + rand.nextDouble() * (maxCenterX - minCenterX);
                            manager.circleCenterY = minCenterY + rand.nextDouble() * (maxCenterY - minCenterY);
                        } else {
                            manager.circleCenterX = paneWidth / 2;
                            manager.circleCenterY = paneHeight / 2;
                            System.err.println("Warning: Habitat не может определить корректный центр круга для Manager.");
                        }
                        manager.angle = rand.nextDouble() * 2 * Math.PI;
                        double initialX = manager.circleCenterX + radius * Math.cos(manager.angle) - objectWidth / 2;
                        double initialY = manager.circleCenterY + radius * Math.sin(manager.angle) - objectHeight / 2;
                        manager.moveTo(initialX, initialY);
                        managerImageView.setX(initialX);
                        managerImageView.setY(initialY);

                        synchronized (objCollection) {
                            if (st.getMainController() != null && st.getMainController().getPane() != null) {
                                Platform.runLater(() -> st.getMainController().getPane().getChildren().add(managerImageView));
                            }
                            objCollection.add(manager);
                            synchronized (bornCollection) {
                                bornCollection.put(manager.getId(), time);
                            }
                            synchronized (idCollection) {
                                idCollection.add(manager.getId());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка создания Manager: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Неожиданная ошибка в Habitat.update(): " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Метод для полной очистки среды
    public void clearObjects() {
        synchronized (objCollection) {
            final List<ImageView> viewsToRemove = new LinkedList<>();
            for (Person p : objCollection) {
                if (p.getImageView() != null) {
                    viewsToRemove.add(p.getImageView());
                }
            }
            if (!viewsToRemove.isEmpty() && Statistics.getInstance() != null && Statistics.getInstance().getMainController() != null) {
                Platform.runLater(() -> {
                    Controller controller = Statistics.getInstance().getMainController();
                    if (controller != null && controller.getPane() != null) {
                        controller.getPane().getChildren().removeAll(viewsToRemove);
                    }
                });
            } else if (!viewsToRemove.isEmpty()) {
                System.err.println("Не удалось очистить панель в Habitat.clearObjects().");
            }
            objCollection.clear();
        }
        synchronized (bornCollection) {
            bornCollection.clear();
        }
        synchronized (idCollection) {
            idCollection.clear();
        }
        Developer.count = 0;
        Manager.count = 0;
        Developer.spawnedCount = 0;
        Manager.spawnedCount = 0;
        System.out.println("Habitat очищен.");
    }

    // Метод: Удаление всех менеджеров
    public int removeAllManagers() {
        int removedCount = 0;
        List<Person> managersToRemove = new ArrayList<>();
        Statistics st = Statistics.getInstance();

        synchronized (objCollection) {
            objCollection.removeIf(p -> {
                if (p instanceof Manager) {
                    managersToRemove.add(p);
                    return true;
                }
                return false;
            });
        }

        if (!managersToRemove.isEmpty()) {
            synchronized (bornCollection) {
                synchronized (idCollection) {
                    for (Person manager : managersToRemove) {
                        int id = manager.getId();
                        bornCollection.remove(id);
                        idCollection.remove(id);
                        final ImageView iv = manager.getImageView();
                        if (iv != null && st != null && st.getMainController() != null && st.getMainController().getPane() != null) {
                            Platform.runLater(() -> st.getMainController().getPane().getChildren().remove(iv));
                        }
                        removedCount++;
                    }
                }
            }
        }
        System.out.println("Удалено менеджеров командой: " + removedCount);
        return removedCount;
    }

    // Метод: Создание N менеджеров
    public int spawnManagers(int n) {
        int spawned = 0;
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        if (st == null) { System.err.println("Ошибка: Statistics не инициализирован в spawnManagers."); return 0; }
        int currentTime = st.getTime();

        for (int i = 0; i < n; i++) {
            try {
                Manager manager = new Manager(0, 0);
                final ImageView managerImageView = manager.getImageView();
                if (managerImageView == null) { System.err.println("Не создан ImageView для Manager #" + (i+1)); continue; }

                double paneWidth=getWidth(); double paneHeight=getHeight(); double objectWidth=managerImageView.getFitWidth(); double objectHeight=managerImageView.getFitHeight(); double radius=100;
                double maxCenterX=paneWidth-radius-objectWidth/2; double maxCenterY=paneHeight-radius-objectHeight/2; double minCenterX=radius+objectWidth/2; double minCenterY=radius+objectHeight/2;
                if(minCenterX > maxCenterX) minCenterX = maxCenterX = paneWidth / 2; if(minCenterY > maxCenterY) minCenterY = maxCenterY = paneHeight / 2;
                if(maxCenterX>=minCenterX&&maxCenterY>=minCenterY){ manager.circleCenterX=minCenterX+rand.nextDouble()*(maxCenterX-minCenterX); manager.circleCenterY=minCenterY+rand.nextDouble()*(maxCenterY-minCenterY); } else { manager.circleCenterX = paneWidth / 2; manager.circleCenterY = paneHeight / 2; }
                manager.angle=rand.nextDouble()*2*Math.PI; double initialX=manager.circleCenterX+radius*Math.cos(manager.angle)-objectWidth/2; double initialY=manager.circleCenterY+radius*Math.sin(manager.angle)-objectHeight/2;
                manager.moveTo(initialX, initialY); managerImageView.setX(initialX); managerImageView.setY(initialY);

                synchronized(objCollection) { synchronized(bornCollection) { synchronized(idCollection) {
                    if (st.getMainController() != null && st.getMainController().getPane() != null) {
                        Platform.runLater(() -> st.getMainController().getPane().getChildren().add(managerImageView));
                    }
                    objCollection.add(manager); bornCollection.put(manager.getId(), currentTime); idCollection.add(manager.getId()); spawned++;
                }}}
            } catch (Exception e) { System.err.println("Ошибка при создании Manager #" + (i+1) + " командой: " + e.getMessage()); e.printStackTrace(); }
        }
        System.out.println("Создано менеджеров командой: " + spawned);
        return spawned;
    }
}