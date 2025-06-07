package ru.nstu.laba1timp.model;

import java.util.Random;
// import ru.nstu.laba1timp.controllers.Habitat; // <<< ЗАКОММЕНТИРОВАНО
import ru.nstu.laba1timp.controllers.Habitat; // <<< ИСПОЛЬЗУЕТСЯ ЭТОТ ИМПОРТ

// Класс ИИ для управления объектами Developer
public class DeveloperAI extends BaseAI {

    private static DeveloperAI instance;  // Единственный экземпляр (singleton)

    // Константы для движения
    private static final double SPEED = 5;  // Скорость движения
    private static final int DIRECTION_CHANGE_INTERVAL = 250;  // Интервал смены направления (5 сек при sleep(20))

    // Метод для получения единственного экземпляра (Singleton)
    public static DeveloperAI getInstance() {
        if (instance == null) {
            instance = new DeveloperAI("Developer AI");  // Создание экземпляра при первом вызове
        }
        return instance;
    }

    // Приватный конструктор для Singleton
    private DeveloperAI(String name) {
        super(name);
        this.objectType = "Developer";  // Установка типа объекта, которым управляет этот AI
    }

    // Основной метод обновления позиции объекта Developer
    @Override
    protected void updateObjectPosition(Person obj, Random rand) {
        // Проверка, действительно ли это Developer (дополнительная защита)
        if (!(obj instanceof Developer)) {
            return;
        }
        Developer dev = (Developer) obj;

        // Проверка наличия ImageView (важно после десериализации, когда ImageView transient)
        if (dev.getImageView() == null) {
            // Если ImageView нет, AI не может рассчитать размеры или переместить объект.
            // Можно добавить логику ожидания или просто пропустить итерацию.
            // System.err.println("Предупреждение: DeveloperAI обнаружил Developer без ImageView (ID: " + dev.getId() + ")");
            return;
        }

        // --- Логика хаотичного движения ---

        // 1. Смена направления, если счетчик истек
        if (dev.directionChangeCounter <= 0) {
            double angle = rand.nextDouble() * 2 * Math.PI; // Случайный угол в радианах
            dev.dx = SPEED * Math.cos(angle); // Новая компонента скорости по X
            dev.dy = SPEED * Math.sin(angle); // Новая компонента скорости по Y
            dev.directionChangeCounter = DIRECTION_CHANGE_INTERVAL; // Сброс счетчика интервала
        } else {
            dev.directionChangeCounter--; // Уменьшаем счетчик
        }

        // 2. Расчет новой предполагаемой позиции
        // Используем currentX/currentY из Person для чтения текущей позиции
        double currentX = dev.getCurrentX();
        double currentY = dev.getCurrentY();
        double newX = currentX + dev.dx;
        double newY = currentY + dev.dy;

        // 3. Получение размеров объекта и среды
        double imgWidth = dev.getImageView().getFitWidth();
        double imgHeight = dev.getImageView().getFitHeight();
        Habitat habitat = Habitat.getInstance(); // Получаем экземпляр среды
        if (habitat == null) {
            System.err.println("Ошибка: Habitat не доступен в DeveloperAI.");
            return; // Не можем проверить границы
        }
        double habitatWidth = habitat.getWidth();
        double habitatHeight = habitat.getHeight();

        // 4. Проверка и обработка столкновений с границами (отражение)
        boolean reflected = false; // Флаг, было ли отражение

        // Проверка по оси X
        if (newX < 0) { // Столкновение с левой границей
            dev.dx *= -1; // Инвертируем скорость по X
            newX = 0;     // Корректируем позицию, чтобы не "залипал" за границей
            reflected = true;
        } else if (newX > habitatWidth - imgWidth) { // Столкновение с правой границей
            dev.dx *= -1; // Инвертируем скорость по X
            newX = habitatWidth - imgWidth; // Корректируем позицию
            reflected = true;
        }

        // Проверка по оси Y
        if (newY < 0) { // Столкновение с верхней границей
            dev.dy *= -1; // Инвертируем скорость по Y
            newY = 0;     // Корректируем позицию
            reflected = true;
        } else if (newY > habitatHeight - imgHeight) { // Столкновение с нижней границей
            dev.dy *= -1; // Инвертируем скорость по Y
            newY = habitatHeight - imgHeight; // Корректируем позицию
            reflected = true;
        }

        // Если было отражение, можно сбросить счетчик смены направления для большей хаотичности
        if (reflected) {
            dev.directionChangeCounter = 0; // На следующей итерации будет выбрано новое случайное направление
        }

        // 5. Перемещение объекта в новую позицию
        // Метод moveTo обновит и currentX/currentY в Person, и позицию ImageView в UI потоке
        dev.moveTo(newX, newY);
    }
}