// ru.nstu.laba1timp.model.ManagerAI.java
package ru.nstu.laba1timp.model;

import java.util.Random;
import ru.nstu.laba1timp.Habitat;

// Класс ИИ для управления объектами Manager
public class ManagerAI extends BaseAI {
    private static ManagerAI instance;  // Единственный экземпляр (singleton)
    private static final double SPEED = 3;  // Скорость движения
    private static final double RADIUS = 100;  // Радиус окружности
    private static final double ANGLE_INCREMENT = Math.toRadians(2);  // Приращение угла

    public static ManagerAI getInstance() {
        if (instance == null) {
            instance = new ManagerAI("Manager AI");  // Создание экземпляра при первом вызове
        }
        return instance;
    }

    private ManagerAI(String name) {
        super(name);
        this.objectType = "Manager";  // Установка типа объекта
    }

    @Override
    protected void updateObjectPosition(Person obj, Random rand) {
        Manager man = (Manager) obj;

        // Инициализация центра окружности при первом вызове
        if (man.circleCenterX == 0 && man.circleCenterY == 0) {
            double paneWidth = Habitat.getInstance().getWidth();
            double paneHeight = Habitat.getInstance().getHeight();
            double objectWidth = man.getImageView().getFitWidth();
            double objectHeight = man.getImageView().getFitHeight();

            // Расчет допустимых границ для центра
            double maxCenterX = paneWidth - RADIUS - objectWidth / 2;
            double maxCenterY = paneHeight - RADIUS - objectHeight / 2;
            double minCenterX = RADIUS + objectWidth / 2;
            double minCenterY = RADIUS + objectHeight / 2;

            // Случайное размещение центра в допустимых границах
            man.circleCenterX = minCenterX + rand.nextDouble() * (maxCenterX - minCenterX);
            man.circleCenterY = minCenterY + rand.nextDouble() * (maxCenterY - minCenterY);
            man.angle = rand.nextDouble() * 2 * Math.PI;  // Начальный угол
        }

        // Расчет новой позиции на окружности
        man.angle += ANGLE_INCREMENT;
        double newX = man.circleCenterX + RADIUS * Math.cos(man.angle) - man.getImageView().getFitWidth() / 2;
        double newY = man.circleCenterY + RADIUS * Math.sin(man.angle) - man.getImageView().getFitHeight() / 2;

        man.moveTo(newX, newY);  // Перемещение объекта
    }
}