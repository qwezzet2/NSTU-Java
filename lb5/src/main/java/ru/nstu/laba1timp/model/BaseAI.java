package ru.nstu.laba1timp.model;

// <<< ПАКЕТ ОСТАВЛЕН КАК ЕСТЬ >>>
// import ru.nstu.laba1timp.controllers.Habitat; // <<< ЗАКОММЕНТИРОВАНО, т.к. Habitat в другом пакете
import ru.nstu.laba1timp.controllers.Habitat; // <<< ИСПОЛЬЗУЕТСЯ ЭТОТ ИМПОРТ

import java.util.LinkedList;
import java.util.Random;
import java.util.Vector; // Оставлен, хотя не используется

// Базовый абстрактный класс для ИИ объектов
public abstract class BaseAI extends Thread {

    protected String objectType;

    // По умолчанию AI неактивен
    public boolean isActive = false; // <<< ИЗМЕНЕНО НА false ПО УМОЛЧАНИЮ >>>

    public final Object monitor = new Object();

    public BaseAI(String name) {
        super(name);
    }

    @Override
    public void run() {
        // Получаем экземпляр Habitat один раз
        Habitat habitatInstance = Habitat.getInstance();
        if (habitatInstance == null) {
            System.err.println("Ошибка: Habitat не инициализирован в потоке " + getName());
            return;
        }

        // Получаем коллекцию объектов
        LinkedList<Person> objects = habitatInstance.getObjCollection();
        if (objects == null) {
            System.err.println("Ошибка: Коллекция объектов null в потоке " + getName());
            return;
        }

        Random rand = new Random();

        while (true) {
            try {
                // Блок синхронизации для управления активностью потока
                synchronized (monitor) {
                    while (!isActive) { // Поток будет ждать здесь при старте
                        try {
                            monitor.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
                            System.out.println("Поток " + getName() + " прерван во время ожидания.");
                            return; // Завершаем поток
                        }
                    }
                } // конец synchronized (monitor)

                // Синхронизированный доступ к коллекции объектов
                synchronized (objects) {
                    // Создаем копию для безопасной итерации
                    LinkedList<Person> objectsCopy = new LinkedList<>(objects);
                    for (Person obj : objectsCopy) {
                        // Проверяем тип и null
                        if (obj != null && obj.getClass().getSimpleName().equals(objectType)) {
                            updateObjectPosition(obj, rand); // Вызов абстрактного метода
                        }
                    }
                } // конец synchronized (objects)

                // Задержка для контроля скорости
                Thread.sleep(20);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Поток " + getName() + " прерван во время сна.");
                return; // Завершаем поток
            } catch (NullPointerException npe) {
                // Отлов NPE для отладки
                System.err.println("NullPointerException в потоке " + getName() + ": " + npe.getMessage());
                npe.printStackTrace();
                // Небольшая пауза перед следующей попыткой
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (Exception e) {
                // Отлов других непредвиденных ошибок
                System.err.println("Неожиданная ошибка в потоке " + getName() + ": " + e.getMessage());
                e.printStackTrace();
                return; // Завершаем поток при серьезной ошибке
            }
        } // конец while(true)
    } // конец run()

    // Абстрактный метод для обновления позиции объекта
    protected abstract void updateObjectPosition(Person obj, Random rand);

} // конец класса BaseAI