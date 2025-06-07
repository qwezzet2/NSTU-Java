package ru.nstu.laba1timp.console;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import ru.nstu.laba1timp.model.Manager; // Импорт Manager
import ru.nstu.laba1timp.controllers.Habitat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConsoleController {

    @FXML
    private TextArea consoleTextArea;

    private StringBuilder currentInput = new StringBuilder(); // Буфер для текущего ввода
    private int lastOutputLength = 0; // Длина текста до начала ввода пользователя

    @FXML
    public void initialize() {
        // Начальное сообщение
        String welcomeText = "Консоль управления симуляцией.\nВведите 'help' для списка команд.\n\n>";
        consoleTextArea.setText(welcomeText);
        lastOutputLength = welcomeText.length();
        consoleTextArea.positionCaret(lastOutputLength); // Ставим каретку в конец

        // Фильтр событий клавиатуры
        consoleTextArea.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            // Запрещаем редактировать прошлый вывод
            if (consoleTextArea.getCaretPosition() < lastOutputLength || consoleTextArea.getSelection().getStart() < lastOutputLength) {
                consoleTextArea.positionCaret(consoleTextArea.getText().length()); // Перемещаем каретку в конец
            }

            // Обработка клавиш
            if (keyEvent.getCode() == KeyCode.ENTER) {
                processEnter();
                keyEvent.consume(); // Поглощаем Enter
            } else if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
                // Разрешаем Backspace только если есть что стирать в текущем вводе
                if (consoleTextArea.getCaretPosition() <= lastOutputLength) {
                    keyEvent.consume();
                }
            } else if (keyEvent.getCode() == KeyCode.DELETE) {
                // Разрешаем Delete только если есть что стирать в текущем вводе
                if (consoleTextArea.getCaretPosition() < lastOutputLength) {
                    keyEvent.consume();
                }
            } else if (keyEvent.isControlDown() || keyEvent.isMetaDown() || keyEvent.isAltDown()) {
                // Блокируем некоторые комбинации
                if (keyEvent.getCode() == KeyCode.C || keyEvent.getCode() == KeyCode.V || keyEvent.getCode() == KeyCode.X || keyEvent.getCode() == KeyCode.A) {
                    // Можно разрешить/запретить копирование/вставку и т.д.
                    // keyEvent.consume(); // Раскомментировать для блокировки
                }
            } else if (keyEvent.getCode().isArrowKey() || keyEvent.getCode() == KeyCode.HOME || keyEvent.getCode() == KeyCode.END || keyEvent.getCode() == KeyCode.PAGE_UP || keyEvent.getCode() == KeyCode.PAGE_DOWN) {
                // Блокируем клавиши навигации, если они пытаются уйти из зоны ввода
                if (consoleTextArea.getCaretPosition() <= lastOutputLength && (keyEvent.getCode() == KeyCode.LEFT || keyEvent.getCode() == KeyCode.UP || keyEvent.getCode() == KeyCode.HOME)) {
                    keyEvent.consume();
                }
                // Для остальных клавиш навигации позволяем двигаться в пределах ввода
                else if (consoleTextArea.getSelection().getEnd() < lastOutputLength) {
                    // Если выделение выходит за пределы ввода - блокируем
                    keyEvent.consume();
                }
            }
        });

        // Следим за изменением текста, чтобы корректно обрабатывать Backspace/Delete
        // и не давать редактировать прошлый вывод
        consoleTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!Platform.isFxApplicationThread()) return; // Только в UI потоке

            // Если текст стал короче, чем был до ввода -> не даем стереть
            if (newValue.length() < lastOutputLength) {
                consoleTextArea.setText(oldValue); // Возвращаем старый текст
                consoleTextArea.positionCaret(consoleTextArea.getText().length()); // Каретку в конец
            }
            // Если изменили что-то до зоны ввода -> отменяем
            else if (!newValue.startsWith(oldValue.substring(0, lastOutputLength))) {
                consoleTextArea.setText(oldValue);
                consoleTextArea.positionCaret(consoleTextArea.getText().length());
            }
        });

        // Запрещаем использование мыши для изменения позиции или выделения вне зоны ввода
        consoleTextArea.addEventFilter(MouseEvent.ANY, event -> {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED ||
                    event.getEventType() == MouseEvent.MOUSE_DRAGGED ||
                    event.getEventType() == MouseEvent.MOUSE_CLICKED)
            {
                // Даем время UI обновиться после клика/перетаскивания
                Platform.runLater(() -> {
                    if (consoleTextArea.getCaretPosition() < lastOutputLength || consoleTextArea.getSelection().getStart() < lastOutputLength) {
                        consoleTextArea.positionCaret(consoleTextArea.getText().length());
                        // Сбрасываем выделение, если оно было вне зоны ввода
                        if (consoleTextArea.getSelection().getLength() > 0 && consoleTextArea.getSelection().getStart() < lastOutputLength) {
                            consoleTextArea.deselect();
                        }
                    }
                });
            }
        });
    }

    // Обработка нажатия Enter
    private void processEnter() {
        String currentText = consoleTextArea.getText();
        String commandLine = currentText.substring(lastOutputLength).trim(); // Получаем введенную команду

        appendToConsole("\n"); // Перевод строки после команды

        if (!commandLine.isEmpty()) {
            executeCommand(commandLine); // Выполняем команду
        }

        appendToConsole(">"); // Новая строка приглашения
        lastOutputLength = consoleTextArea.getText().length(); // Обновляем длину вывода
        consoleTextArea.positionCaret(lastOutputLength); // Ставим каретку
    }

    // Выполнение команды
    private void executeCommand(String commandLine) {
        String[] parts = commandLine.split("\\s+", 2); // Разделяем команду и параметры
        String command = parts[0].toLowerCase();
        String param = (parts.length > 1) ? parts[1] : null;

        switch (command) {
            case "help":
                printHelp();
                break;
            case "fire_managers":
                fireAllManagers();
                break;
            case "hire_managers":
                hireManagers(param);
                break;
            default:
                appendToConsole("Неизвестная команда: " + command + "\n");
                break;
        }
    }

    // Вывод справки
    private void printHelp() {
        appendToConsole("Доступные команды:\n");
        appendToConsole("  help            - Показать эту справку\n");
        appendToConsole("  fire_managers   - Уволить всех текущих менеджеров\n");
        appendToConsole("  hire_managers N - Нанять N новых менеджеров (N > 0)\n");
    }

    // Увольнение всех менеджеров
    private void fireAllManagers() {
        Habitat hab = Habitat.getInstance();
        if (hab == null) {
            appendToConsole("Ошибка: Не удалось получить доступ к среде Habitat.\n");
            return;
        }
        int removedCount = hab.removeAllManagers(); // Вызываем метод в Habitat
        appendToConsole("Уволено менеджеров: " + removedCount + "\n");
    }

    // Найм N менеджеров
    private void hireManagers(String param) {
        if (param == null) {
            appendToConsole("Ошибка: Укажите количество нанимаемых менеджеров (N).\n");
            appendToConsole("Пример: hire_managers 5\n");
            return;
        }
        try {
            int n = Integer.parseInt(param);
            if (n <= 0) {
                appendToConsole("Ошибка: Количество менеджеров (N) должно быть положительным числом.\n");
                return;
            }
            Habitat hab = Habitat.getInstance();
            if (hab == null) {
                appendToConsole("Ошибка: Не удалось получить доступ к среде Habitat.\n");
                return;
            }
            int hiredCount = hab.spawnManagers(n); // Вызываем метод в Habitat
            appendToConsole("Нанято новых менеджеров: " + hiredCount + "\n");

        } catch (NumberFormatException e) {
            appendToConsole("Ошибка: Неверный формат количества менеджеров (N). Введите целое число.\n");
        }
    }

    // Безопасное добавление текста в консоль
    private void appendToConsole(String text) {
        Platform.runLater(() -> {
            consoleTextArea.appendText(text);
            // Обновляем lastOutputLength, чтобы нельзя было стереть новый вывод
            // Делаем это здесь, а не только в processEnter, на случай асинхронного вывода
            lastOutputLength = consoleTextArea.getText().length();
            consoleTextArea.positionCaret(lastOutputLength);
            consoleTextArea.setScrollTop(Double.MAX_VALUE); // Прокрутка вниз
        });
    }
}