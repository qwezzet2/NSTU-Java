// ru.nstu.laba1timp.model.DeveloperAI.java
package ru.nstu.laba1timp.model;

import java.util.Random;
import ru.nstu.laba1timp.Habitat;

// Класс ИИ для управления объектами Developer
public class DeveloperAI extends BaseAI {
    private static DeveloperAI instance;  // Единственный экземпляр (singleton)
    private static final double SPEED = 5;  // Скорость движения
    private static final int DIRECTION_CHANGE_INTERVAL = 5 * 50;  // Интервал смены направления

    public static DeveloperAI getInstance() {
        if (instance == null) {
            instance = new DeveloperAI("Developer AI");  // Создание экземпляра при первом вызове
        }
        return instance;
    }

    private DeveloperAI(String name) {
        super(name);
        this.objectType = "Developer";  // Установка типа объекта
    }

    @Override
    protected void updateObjectPosition(Person obj, Random rand) {
        Developer dev = (Developer) obj;

        // Смена направления при достижении счетчика
        if (dev.directionChangeCounter <= 0) {
            double angle = rand.nextDouble() * 2 * Math.PI;  // Случайный угол
            dev.dx = SPEED * Math.cos(angle);  // Новое направление по X
            dev.dy = SPEED * Math.sin(angle);  // Новое направление по Y
            dev.directionChangeCounter = DIRECTION_CHANGE_INTERVAL;  // Сброс счетчика
        } else {
            dev.directionChangeCounter--;  // Декремент счетчика
        }

        // Расчет новой позиции
        double currentX = dev.getImageView().getX();
        double currentY = dev.getImageView().getY();
        double newX = currentX + dev.dx;
        double newY = currentY + dev.dy;

        // Отражение от границ
        if (newX < 0 || newX > Habitat.getInstance().getWidth() - dev.getImageView().getFitWidth()) {
            dev.dx *= -1;
            newX = currentX + dev.dx;
        }
        if (newY < 0 || newY > Habitat.getInstance().getHeight() - dev.getImageView().getFitHeight()) {
            dev.dy *= -1;
            newY = currentY + dev.dy;
        }

        dev.moveTo(newX, newY);  // Перемещение объекта
    }
}