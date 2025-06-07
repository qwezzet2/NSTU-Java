package ru.nstu.laba1timp.model;

import java.util.Random;
// import ru.nstu.laba1timp.controllers.Habitat;
import ru.nstu.laba1timp.controllers.Habitat;

// Класс ИИ для управления объектами Manager
public class ManagerAI extends BaseAI {

    private static ManagerAI instance;  // Единственный экземпляр (singleton)

    // Константы для кругового движения
    // private static final double SPEED = 3;
    private static final double RADIUS = 100;  // Радиус окружности
    private static final double ANGLE_INCREMENT = Math.toRadians(2);  // Приращение угла в радианах за шаг

    // Метод получения единственного экземпляра (Singleton)
    public static ManagerAI getInstance() {
        if (instance == null) {
            instance = new ManagerAI("Manager AI"); // Создание при первом вызове
        }
        return instance;
    }

    // Приватный конструктор для Singleton
    private ManagerAI(String name) {
        super(name);
        this.objectType = "Manager"; // Указываем, что этот AI управляет объектами Manager
    }

    // Основной метод обновления позиции объекта Manager
    @Override
    protected void updateObjectPosition(Person obj, Random rand) {
        // Проверка типа объекта (дополнительная безопасность)
        if (!(obj instanceof Manager)) {
            return;
        }
        Manager man = (Manager) obj;

        // Проверка наличия ImageView (важно после десериализации)
        if (man.getImageView() == null) {
            // System.err.println("Предупреждение: ManagerAI обнаружил Manager без ImageView (ID: " + man.getId() + ")");
            return;
        }

        // --- Логика кругового движения ---

        // 1. Инициализация центра окружности и начального угла, если они не установлены
        // Это происходит при первом обновлении объекта ИЛИ после десериализации,
        // так как circleCenterX/Y/angle не сохраняются (они не transient, но имеют начальные 0).
        if (man.circleCenterX == 0 && man.circleCenterY == 0) {
            Habitat habitat = Habitat.getInstance(); // Получаем доступ к среде
            if (habitat == null) {
                System.err.println("Ошибка: Habitat не доступен в ManagerAI.");
                return;
            }
            double paneWidth = habitat.getWidth();
            double paneHeight = habitat.getHeight();
            double objectWidth = man.getImageView().getFitWidth();
            double objectHeight = man.getImageView().getFitHeight();

            // Расчет допустимых границ для центра окружности, чтобы объект не выходил за рамки
            double maxCenterX = paneWidth - RADIUS - objectWidth / 2;
            double maxCenterY = paneHeight - RADIUS - objectHeight / 2;
            double minCenterX = RADIUS + objectWidth / 2;
            double minCenterY = RADIUS + objectHeight / 2;

            // Корректировка границ, если панель слишком мала
            if (minCenterX > maxCenterX) minCenterX = maxCenterX = paneWidth / 2;
            if (minCenterY > maxCenterY) minCenterY = maxCenterY = paneHeight / 2;

            // Установка случайного центра в допустимых границах
            if (maxCenterX >= minCenterX && maxCenterY >= minCenterY) {
                man.circleCenterX = minCenterX + rand.nextDouble() * (maxCenterX - minCenterX);
                man.circleCenterY = minCenterY + rand.nextDouble() * (maxCenterY - minCenterY);
            } else {
                // Fallback, если границы некорректны
                man.circleCenterX = paneWidth / 2;
                man.circleCenterY = paneHeight / 2;
                System.err.println("Предупреждение: ManagerAI не может определить корректный центр круга, используется центр панели.");
            }

            // Установка случайного начального угла, если он еще не установлен (равен 0)
            if (man.angle == 0) {
                man.angle = rand.nextDouble() * 2 * Math.PI; // Угол в радианах
            }

            // Рассчитываем начальную позицию на окружности
            double initialX = man.circleCenterX + RADIUS * Math.cos(man.angle) - objectWidth / 2;
            double initialY = man.circleCenterY + RADIUS * Math.sin(man.angle) - objectHeight / 2;

            // Устанавливаем начальную позицию
            // Используем moveTo, чтобы обновить currentX/Y в Person
            man.moveTo(initialX, initialY);
            // Также устанавливаем напрямую в ImageView, т.к. moveTo асинхронный
            man.getImageView().setX(initialX);
            man.getImageView().setY(initialY);

            // System.out.println("Manager ID " + man.getId() + " инициализирован: Центр(" + String.format("%.1f", man.circleCenterX) + ", " + String.format("%.1f", man.circleCenterY) + "), Угол(" + String.format("%.2f", man.angle) + ")");
        }

        // 2. Обновление угла для движения по окружности
        man.angle += ANGLE_INCREMENT;

        // Нормализация угла (чтобы он оставался в пределах 0 до 2*PI)
        if (man.angle > 2 * Math.PI) {
            man.angle -= 2 * Math.PI;
        }

        // 3. Расчет новой позиции на окружности
        double newX = man.circleCenterX + RADIUS * Math.cos(man.angle) - man.getImageView().getFitWidth() / 2;
        double newY = man.circleCenterY + RADIUS * Math.sin(man.angle) - man.getImageView().getFitHeight() / 2;

        // 4. Перемещение объекта
        man.moveTo(newX, newY); // Обновляет currentX/Y и позицию ImageView (асинхронно)
    }
}