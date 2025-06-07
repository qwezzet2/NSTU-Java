// ru.nstu.laba1timp.model.BaseAI.java
package ru.nstu.laba1timp.model;

import ru.nstu.laba1timp.Habitat;

import java.util.LinkedList;
import java.util.Random;

// Базовый абстрактный класс для ИИ объектов
public abstract class BaseAI extends Thread {
    protected String objectType;  // Тип объекта, которым управляет этот ИИ
    public boolean isActive = true;  // Флаг активности потока
    public final Object monitor = new Object();  // Объект для синхронизации

    public BaseAI(String name) {
        super(name);  // Устанавливаем имя потока
    }

    public void run() {
        LinkedList<Person> objects = Habitat.getInstance().getObjCollection();  // Получаем коллекцию объектов
        Random rand = new Random();  // Генератор случайных чисел

        while (true) {
            // Блок синхронизации для управления активностью потока
            synchronized (monitor) {
                while (!isActive) {
                    try {
                        monitor.wait();  // Приостанавливаем поток, если не активен
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();  // Восстанавливаем флаг прерывания
                        return;  // Завершаем поток при прерывании
                    }
                }
            }

            // Синхронизированный доступ к коллекции объектов
            synchronized (objects) {
                for (int i = 0; i < objects.size(); i++) {
                    Person obj = objects.get(i);
                    // Обновляем позицию только объектов соответствующего типа
                    if ((obj.getClass().getSimpleName()).equals(objectType)) {
                        updateObjectPosition(obj, rand);  // Абстрактный метод для обновления позиции
                    }
                }
            }

            try {
                Thread.sleep(20);  // Задержка для контроля скорости обновления
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Абстрактный метод для обновления позиции объекта
    protected abstract void updateObjectPosition(Person obj, Random rand);
}