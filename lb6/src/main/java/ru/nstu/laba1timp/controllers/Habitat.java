package ru.nstu.laba1timp.controllers;

import ru.nstu.laba1timp.model.Developer;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.model.Person;
import javafx.application.Platform;
import javafx.scene.image.ImageView;

import java.io.FileNotFoundException; // Убедись, что этот импорт нужен (для конструкторов Developer/Manager)
import java.util.*; // Импортируем нужные коллекции (List, Map, Set, LinkedList, HashMap, TreeSet, Iterator, ArrayList, Objects)
import java.util.stream.Collectors;

public class Habitat {
    // --- Поля ---
    private final int width = 1090; // Ширина области визуализации
    private final int height = 680; // Высота области визуализации
    public int n1 = 1; // Период появления Разработчиков по умолчанию
    public int n2 = 2; // Период появления Менеджеров по умолчанию
    public float p1 = 0.8f; // Вероятность появления Разработчиков по умолчанию
    public float p2 = 0.4f; // Вероятность появления Менеджеров по умолчанию
    public int maxManagerPercent = 40; // Макс. процент менеджеров по умолчанию

    // --- Коллекции (используем типы из твоего первоначального кода) ---
    private final LinkedList<Person> objCollection; // <<<=== Используем LinkedList
    private final HashMap<Integer, Integer> bornCollection;
    private final TreeSet<Integer> idCollection;

    private static volatile Habitat instance; // volatile для потокобезопасного синглтона
    private final Object lock = new Object(); // Объект для синхронизации доступа к коллекциям

    private Habitat() {
        objCollection = new LinkedList<>(); // Инициализируем LinkedList
        bornCollection = new HashMap<>();
        idCollection = new TreeSet<>();
    }

    // Синглтон
    public static Habitat getInstance() {
        Habitat localInstance = instance;
        if (localInstance == null) {
            synchronized (Habitat.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Habitat();
                }
            }
        }
        return localInstance;
    }

    // --- Геттеры (возвращаем оригинальные типы) ---
    public LinkedList<Person> getObjCollection() { return objCollection; } // <<<=== Возвращаем LinkedList
    public HashMap<Integer, Integer> getBornCollection() { return bornCollection; }
    public TreeSet<Integer> getIdCollection() { return idCollection; } // Используем TreeSet
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // --- Логика обновления симуляции ---
    public void update() {
        Random rand = new Random();
        Statistics st = Statistics.getInstance();
        // Проверяем инициализацию статистики и контроллера
        if (st == null || st.getMainController() == null) {
            System.err.println("Статистика или контроллер не инициализированы в Habitat.update()");
            return;
        }
        int time = st.getTime(); // Текущее время симуляции
        float p = rand.nextFloat(); // Один случайный бросок на весь цикл обновления

        // Используем один блок synchronized для консистентности во время обновления
        synchronized (lock) {
            // 1. Удаляем "мертвые" объекты
            removeDeadObjects(time, st.getMainController());

            // 2. Создаем новые объекты
            spawnDeveloper(time, p, rand, st.getMainController());
            spawnManager(time, p, rand, st.getMainController());
        }
    }

    // Приватный метод для удаления объектов, у которых истек срок жизни
    private void removeDeadObjects(int currentTime, Controller controller) {
        // Итератор нужен для безопасного удаления во время итерации внутри synchronized блока
        // Синхронизация уже есть во внешнем методе update()
        Iterator<Person> iterator = objCollection.iterator();
        List<ImageView> viewsToRemove = new ArrayList<>(); // Собираем View для удаления из UI

        while (iterator.hasNext()) {
            Person obj = iterator.next();
            if (obj == null) { iterator.remove(); continue; } // Пропускаем и удаляем null ссылки

            int id = obj.getId();
            // Определяем время жизни в зависимости от типа объекта
            int lifeTime = (obj instanceof Developer) ? Developer.getLifeTime() : Manager.getLifeTime();

            // Проверяем, истекло ли время жизни
            if (bornCollection.getOrDefault(id, Integer.MAX_VALUE) + lifeTime <= currentTime) {
                ImageView imageView = obj.getImageView();
                if (imageView != null) {
                    viewsToRemove.add(imageView); // Добавляем View в список на удаление
                } else {
                    System.err.println("Предупреждение: Удаляется объект ID " + id + " с null ImageView.");
                }

                iterator.remove(); // Удаляем из objCollection (LinkedList)
                bornCollection.remove(id); // Удаляем из bornCollection
                idCollection.remove(id); // Удаляем из idCollection
            }
        }

        // Удаляем ImageView из UI в потоке JavaFX после завершения итерации
        if (!viewsToRemove.isEmpty() && controller != null && controller.getPane() != null) {
            Platform.runLater(() -> {
                if (controller.getPane() != null) { // Доп. проверка панели
                    controller.getPane().getChildren().removeAll(viewsToRemove);
                }
            });
        }
    }

    // Приватный метод для создания Разработчика
    private void spawnDeveloper(int currentTime, float probabilityRoll, Random rand, Controller controller) {
        // Создаем, если время > 0, делится на период И случайное число меньше или равно вероятности
        if ((currentTime > 0 && currentTime % n1 == 0) && (probabilityRoll <= p1)) {
            try {
                // Генерируем координаты
                double spawnX = rand.nextDouble(0, Math.max(1, width - 80));
                double spawnY = rand.nextDouble(0, Math.max(1, height - 80));

                Developer dev = new Developer((int)spawnX, (int)spawnY);
                ImageView devImageView = dev.getImageView();

                if (devImageView != null && controller != null && controller.getPane() != null) {
                    // Добавляем объект в коллекции (уже внутри synchronized(lock) блока из update)
                    objCollection.add(dev);
                    bornCollection.put(dev.getId(), currentTime);
                    idCollection.add(dev.getId());
                    // Добавляем ImageView на панель в потоке UI
                    Platform.runLater(() -> {
                        if (controller.getPane() != null) {
                            controller.getPane().getChildren().add(devImageView);
                        }
                    });
                } else { System.err.println("Не удалось создать/добавить Разработчика или его ImageView."); }
            } catch (Exception e) { System.err.println("Ошибка создания Разработчика: " + e.getMessage()); e.printStackTrace(); }
        }
    }

    // Приватный метод для создания Менеджера
    private void spawnManager(int currentTime, float probabilityRoll, Random rand, Controller controller) {
        // Создаем, если время > 0, делится на период И случайное число меньше или равно вероятности
        if ((currentTime > 0 && currentTime % n2 == 0) && (probabilityRoll <= p2)) {
            // Проверяем ограничение по максимальному проценту менеджеров
            long managersCount = objCollection.stream().filter(obj -> obj instanceof Manager).count();
            long totalCount = objCollection.size(); // Текущее общее число объектов

            // Разрешаем создание, если объектов нет ИЛИ процентное ограничение выполняется
            if (totalCount == 0 || ((double) managersCount * 100.0 / totalCount) < maxManagerPercent) {
                try {
                    Manager manager = new Manager(0, 0); // Начальные координаты не так важны
                    ImageView managerImageView = manager.getImageView();

                    if (managerImageView != null && controller != null && controller.getPane() != null) {
                        // Добавляем объект в коллекции (уже внутри synchronized(lock) блока из update)
                        objCollection.add(manager);
                        bornCollection.put(manager.getId(), currentTime);
                        idCollection.add(manager.getId());

                        // --- Инициализация позиции Менеджера для кругового движения ---
                        double paneWidth=getWidth(); double paneHeight=getHeight(); double objectWidth=managerImageView.getFitWidth(); double objectHeight=managerImageView.getFitHeight(); double radius=100;
                        double maxCenterX=paneWidth-radius-objectWidth/2; double maxCenterY=paneHeight-radius-objectHeight/2; double minCenterX=radius+objectWidth/2; double minCenterY=radius+objectHeight/2;
                        if(minCenterX > maxCenterX) minCenterX = maxCenterX = paneWidth / 2; if(minCenterY > maxCenterY) minCenterY = maxCenterY = paneHeight / 2;
                        if(maxCenterX>=minCenterX&&maxCenterY>=minCenterY){ manager.circleCenterX=minCenterX+rand.nextDouble()*(maxCenterX-minCenterX); manager.circleCenterY=minCenterY+rand.nextDouble()*(maxCenterY-minCenterY); } else { manager.circleCenterX = paneWidth / 2; manager.circleCenterY = paneHeight / 2; }
                        manager.angle=rand.nextDouble()*2*Math.PI; double initialX=manager.circleCenterX+radius*Math.cos(manager.angle)-objectWidth/2; double initialY=manager.circleCenterY+radius*Math.sin(manager.angle)-objectHeight/2;
                        manager.moveTo(initialX, initialY); // Обновляем позицию (включая currentX/Y и UI)
                        // -------------------------------------------------------------

                        // Добавляем ImageView на панель в потоке UI
                        Platform.runLater(() -> {
                            if (controller.getPane() != null) {
                                controller.getPane().getChildren().add(managerImageView);
                            }
                        });

                    } else { System.err.println("Не удалось создать/добавить Менеджера или его ImageView."); }
                } catch (Exception e) { System.err.println("Ошибка создания Менеджера: " + e.getMessage()); e.printStackTrace(); }
            }
        }
    }

    // === МЕТОДЫ ДЛЯ СЕТЕВОГО ОБМЕНА ===
    /**
     * Возвращает копию списка объектов Person указанного типа в виде LinkedList.
     * Метод потокобезопасен.
     * @param type Класс подтипа Person (например, Developer.class).
     * @return Новый LinkedList, содержащий объекты Person указанного типа.
     */
    public List<Person> getPersonsByType(Class<?> type) {
        synchronized (lock) {
            LinkedList<Person> result = new LinkedList<>();
            for (Person p : objCollection) { // Итерация по LinkedList
                if (p != null && type.isInstance(p)) {
                    result.add(p);
                }
            }
            return result; // Возвращаем LinkedList (который реализует List)
        }
    }
    /**
     * Удаляет переданный список объектов Person из симуляции. Потокобезопасен.
     * @param personsToRemove Список объектов Person для удаления.
     */
    public void removePersons(List<Person> personsToRemove) {
        if (personsToRemove == null || personsToRemove.isEmpty()) return;
        List<ImageView> viewsToRemove = new ArrayList<>();
        int removedCount = 0;
        synchronized (lock) {
            Set<Integer> idsToRemoveSet = personsToRemove.stream()
                    .filter(Objects::nonNull)
                    .map(Person::getId)
                    .collect(Collectors.toSet());

            Iterator<Person> iterator = objCollection.iterator(); // Итератор для LinkedList
            while(iterator.hasNext()){
                Person current = iterator.next();
                if(current != null && idsToRemoveSet.contains(current.getId())){
                    iterator.remove(); // Удаляем из objCollection
                    bornCollection.remove(current.getId());
                    idCollection.remove(current.getId());
                    if(current.getImageView() != null){
                        viewsToRemove.add(current.getImageView());
                    }
                    removedCount++;
                }
            }
        } // Конец synchronized

        if (!viewsToRemove.isEmpty()) {
            Statistics st = Statistics.getInstance();
            Controller controller = (st != null) ? st.getMainController() : null;
            if (controller != null && controller.getPane() != null) {
                Platform.runLater(() -> {
                    if (controller.getPane() != null)
                        controller.getPane().getChildren().removeAll(viewsToRemove);
                });
                System.out.println("Удалено " + removedCount + " объектов через сетевой обмен.");
            } else { System.err.println("Ошибка: Не удалось удалить ImageView из UI во время сетевого обмена."); }
        } else if (removedCount > 0) {
            System.out.println("Удалено " + removedCount + " объектов из коллекций (ImageView не найдены).");
        }
    }
    /**
     * Добавляет список объектов Person (полученных по сети) в симуляцию. Потокобезопасен.
     * @param personsToAdd Список объектов Person для добавления.
     * @param birthTimesToAdd Карта <ID, время_рождения> для добавляемых объектов.
     */
    public void addPersons(List<Person> personsToAdd, Map<Integer, Integer> birthTimesToAdd) {
        if (personsToAdd == null || personsToAdd.isEmpty() || birthTimesToAdd == null) return;
        List<ImageView> viewsToAdd = new ArrayList<>(); int addedCount = 0; int skippedCount = 0;
        synchronized (lock) {
            int currentTime = Statistics.getInstance() != null ? Statistics.getInstance().getTime() : 0;
            for (Person p : personsToAdd) {
                if (p == null) continue; int id = p.getId();
                if (idCollection.contains(id)) { System.err.println("Предупреждение: Пропуск добавления Person ID " + id + " из-за конфликта ID."); skippedCount++; continue; }
                // Воссоздаем transient ImageView
                p.recreateImageView(p.getCurrentX(), p.getCurrentY());
                ImageView view = p.getImageView();
                if (view != null) {
                    objCollection.add(p); // Добавляем в LinkedList
                    idCollection.add(id); bornCollection.put(id, birthTimesToAdd.getOrDefault(id, currentTime));
                    viewsToAdd.add(view); addedCount++;
                } else { System.err.println("Ошибка: Не удалось воссоздать ImageView для полученного Person ID " + id); }
            }
        } // Конец synchronized

        if (!viewsToAdd.isEmpty()) {
            Statistics st = Statistics.getInstance();
            Controller controller = (st != null) ? st.getMainController() : null;
            if (controller != null && controller.getPane() != null) {
                Platform.runLater(() -> {
                    if (controller.getPane() != null) // Доп. проверка
                        controller.getPane().getChildren().addAll(viewsToAdd);
                });
                System.out.println("Добавлено " + addedCount + " объектов через сетевой обмен" + (skippedCount > 0 ? " (пропущено " + skippedCount + ")." : "."));
            } else { System.err.println("Ошибка: Не удалось добавить ImageView в UI во время сетевого обмена."); }
        } else if (addedCount == 0 && skippedCount > 0) {
            System.out.println("Пропущено " + skippedCount + " объектов из-за конфликта ID при добавлении.");
        }
    }
    // ========================================

    // --- Существующие методы очистки и утилиты ---
    public void clearObjects() {
        synchronized (lock) {
            final List<ImageView> viewsToRemove = new LinkedList<>();
            for (Person p : objCollection) { if (p != null && p.getImageView() != null) { viewsToRemove.add(p.getImageView()); } }
            objCollection.clear(); bornCollection.clear(); idCollection.clear();
            Developer.count = 0; Manager.count = 0; Developer.spawnedCount = 0; Manager.spawnedCount = 0;
            if (Statistics.getInstance() != null && Statistics.getInstance().getMainController() != null) {
                Platform.runLater(() -> { Controller controller = Statistics.getInstance().getMainController(); if (controller != null && controller.getPane() != null) { controller.getPane().getChildren().clear(); } });
            }
            System.out.println("Habitat очищен.");
        }
    }
    public int removeAllManagers() {
        int removedCount = 0;
        synchronized (lock) { List<Person> managersToRemove = getPersonsByType(Manager.class); removePersons(managersToRemove); removedCount = managersToRemove.size(); }
        System.out.println("Удалено менеджеров командой: " + removedCount); return removedCount;
    }
    public int spawnManagers(int n) {
        int spawned = 0; Random rand = new Random(); Statistics st = Statistics.getInstance(); if (st == null || st.getMainController() == null) { System.err.println("Ошибка: Statistics/Controller не инициализирован в spawnManagers."); return 0; } int currentTime = st.getTime(); Controller controller = st.getMainController();
        List<Person> managersToAddCollection = new ArrayList<>(); // Собираем для UI
        synchronized (lock) { for (int i = 0; i < n; i++) { try { Manager manager = new Manager(0, 0); ImageView managerImageView = manager.getImageView(); if (managerImageView == null) { System.err.println("Не создан ImageView для Manager #" + (i+1)); continue; }
            if (!idCollection.contains(manager.getId())) {
                objCollection.add(manager); // Добавляем в LinkedList
                bornCollection.put(manager.getId(), currentTime); idCollection.add(manager.getId()); spawned++; managersToAddCollection.add(manager);
                /* ... инициализация позиции менеджера ... */
                double paneWidth=getWidth(); double paneHeight=getHeight(); double objectWidth=managerImageView.getFitWidth(); double objectHeight=managerImageView.getFitHeight(); double radius=100; double maxCenterX=paneWidth-radius-objectWidth/2; double maxCenterY=paneHeight-radius-objectHeight/2; double minCenterX=radius+objectWidth/2; double minCenterY=radius+objectHeight/2; if(minCenterX > maxCenterX) minCenterX = maxCenterX = paneWidth / 2; if(minCenterY > maxCenterY) minCenterY = maxCenterY = paneHeight / 2; if(maxCenterX>=minCenterX&&maxCenterY>=minCenterY){ manager.circleCenterX=minCenterX+rand.nextDouble()*(maxCenterX-minCenterX); manager.circleCenterY=minCenterY+rand.nextDouble()*(maxCenterY-minCenterY); } else { manager.circleCenterX = paneWidth / 2; manager.circleCenterY = paneHeight / 2; } manager.angle=rand.nextDouble()*2*Math.PI; double initialX=manager.circleCenterX+radius*Math.cos(manager.angle)-objectWidth/2; double initialY=manager.circleCenterY+radius*Math.sin(manager.angle)-objectHeight/2; manager.moveTo(initialX, initialY);
            } else { System.err.println("Конфликт ID для нового Manager #" + (i+1)); }
        } catch (Exception e) { System.err.println("Ошибка при создании Manager #" + (i+1) + " командой: " + e.getMessage()); e.printStackTrace(); } } }
        // Добавляем все View в UI после цикла
        if (!managersToAddCollection.isEmpty()) { List<ImageView> viewsToAddUI = managersToAddCollection.stream().map(Person::getImageView).filter(Objects::nonNull).collect(Collectors.toList()); if (!viewsToAddUI.isEmpty()) { Platform.runLater(() -> { if (controller.getPane() != null) { controller.getPane().getChildren().addAll(viewsToAddUI); } }); } }
        System.out.println("Создано менеджеров командой: " + spawned); return spawned;
    }

} // Конец класса Habitat