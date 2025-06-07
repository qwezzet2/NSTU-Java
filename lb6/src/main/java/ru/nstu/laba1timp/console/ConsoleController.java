package ru.nstu.laba1timp.console;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import ru.nstu.laba1timp.model.Manager;
import ru.nstu.laba1timp.controllers.Habitat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Контроллер для консоли управления симуляцией.
 * Обрабатывает ввод команд пользователя и управляет взаимодействием с Habitat.
 */
public class ConsoleController {

    @FXML
    private TextArea consoleTextArea;

    // Буфер для текущего ввода пользователя
    private StringBuilder currentInput = new StringBuilder();

    // Длина текста до начала ввода пользователя (чтобы запретить редактирование вывода системы)
    private int lastOutputLength = 0;

    @FXML
    public void initialize() {
        // Инициализация консоли с приветственным сообщением
        String welcomeText = "Консоль управления симуляцией.\nВведите 'help' для списка команд.\n\n>";
        consoleTextArea.setText(welcomeText);
        lastOutputLength = welcomeText.length();
        consoleTextArea.positionCaret(lastOutputLength);

        // Обработка нажатий клавиш
        consoleTextArea.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            // Запрещаем редактирование системного вывода
            if (consoleTextArea.getCaretPosition() < lastOutputLength ||
                    consoleTextArea.getSelection().getStart() < lastOutputLength) {
                consoleTextArea.positionCaret(consoleTextArea.getText().length());
            }

            // Обработка специальных клавиш
            if (keyEvent.getCode() == KeyCode.ENTER) {
                processEnter();
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
                // Запрещаем удаление системного вывода
                if (consoleTextArea.getCaretPosition() <= lastOutputLength) {
                    keyEvent.consume();
                }
            } else if (keyEvent.getCode() == KeyCode.DELETE) {
                // Запрещаем удаление системного вывода
                if (consoleTextArea.getCaretPosition() < lastOutputLength) {
                    keyEvent.consume();
                }
            } else if (keyEvent.isControlDown() || keyEvent.isMetaDown() || keyEvent.isAltDown()) {
                // Обработка комбинаций клавиш
                if (keyEvent.getCode() == KeyCode.C || keyEvent.getCode() == KeyCode.V ||
                        keyEvent.getCode() == KeyCode.X || keyEvent.getCode() == KeyCode.A) {

                }
            } else if (keyEvent.getCode().isArrowKey() || keyEvent.getCode() == KeyCode.HOME ||
                    keyEvent.getCode() == KeyCode.END || keyEvent.getCode() == KeyCode.PAGE_UP ||
                    keyEvent.getCode() == KeyCode.PAGE_DOWN) {
                // Ограничиваем навигацию только областью ввода пользователя
                if (consoleTextArea.getCaretPosition() <= lastOutputLength &&
                        (keyEvent.getCode() == KeyCode.LEFT || keyEvent.getCode() == KeyCode.UP ||
                                keyEvent.getCode() == KeyCode.HOME)) {
                    keyEvent.consume();
                } else if (consoleTextArea.getSelection().getEnd() < lastOutputLength) {
                    keyEvent.consume();
                }
            }
        });

        // Слушатель изменений текста для защиты системного вывода от модификации
        consoleTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!Platform.isFxApplicationThread()) return;

            // Восстанавливаем текст если попытались удалить системный вывод
            if (newValue.length() < lastOutputLength) {
                consoleTextArea.setText(oldValue);
                consoleTextArea.positionCaret(consoleTextArea.getText().length());
            }
            // Восстанавливаем текст если изменили системный вывод
            else if (!newValue.startsWith(oldValue.substring(0, lastOutputLength))) {
                consoleTextArea.setText(oldValue);
                consoleTextArea.positionCaret(consoleTextArea.getText().length());
            }
        });

        // Обработка событий мыши для защиты системного вывода
        consoleTextArea.addEventFilter(MouseEvent.ANY, event -> {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED ||
                    event.getEventType() == MouseEvent.MOUSE_DRAGGED ||
                    event.getEventType() == MouseEvent.MOUSE_CLICKED) {

                Platform.runLater(() -> {
                    // Корректируем позицию каретки если пользователь кликнул на системный вывод
                    if (consoleTextArea.getCaretPosition() < lastOutputLength ||
                            consoleTextArea.getSelection().getStart() < lastOutputLength) {
                        consoleTextArea.positionCaret(consoleTextArea.getText().length());
                        if (consoleTextArea.getSelection().getLength() > 0 &&
                                consoleTextArea.getSelection().getStart() < lastOutputLength) {
                            consoleTextArea.deselect();
                        }
                    }
                });
            }
        });
    }

    /**
     * Обрабатывает нажатие Enter - извлекает команду и выполняет ее.
     */
    private void processEnter() {
        String currentText = consoleTextArea.getText();
        String commandLine = currentText.substring(lastOutputLength).trim();

        appendToConsole("\n");

        if (!commandLine.isEmpty()) {
            executeCommand(commandLine);
        }

        appendToConsole(">");
        lastOutputLength = consoleTextArea.getText().length();
        consoleTextArea.positionCaret(lastOutputLength);
    }

    //Выполняет команду, введенную пользователем.

    private void executeCommand(String commandLine) {
        String[] parts = commandLine.split("\\s+", 2);
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

    //Выводит список доступных команд.
    private void printHelp() {
        appendToConsole("Доступные команды:\n");
        appendToConsole("  help            - Показать эту справку\n");
        appendToConsole("  fire_managers   - Уволить всех текущих менеджеров\n");
        appendToConsole("  hire_managers N - Нанять N новых менеджеров (N > 0)\n");
    }

    // Увольняет всех менеджеров через Habitat.

    private void fireAllManagers() {
        Habitat hab = Habitat.getInstance();
        if (hab == null) {
            appendToConsole("Ошибка: Не удалось получить доступ к среде Habitat.\n");
            return;
        }
        int removedCount = hab.removeAllManagers();
        appendToConsole("Уволено менеджеров: " + removedCount + "\n");
    }

    //Нанимает указанное количество менеджеров через Habitat.

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
            int hiredCount = hab.spawnManagers(n);
            appendToConsole("Нанято новых менеджеров: " + hiredCount + "\n");

        } catch (NumberFormatException e) {
            appendToConsole("Ошибка: Неверный формат количества менеджеров (N). Введите целое число.\n");
        }
    }

    //Добавляет текст в консоль, гарантируя выполнение в UI потоке.

    private void appendToConsole(String text) {
        Platform.runLater(() -> {
            consoleTextArea.appendText(text);
            lastOutputLength = consoleTextArea.getText().length();
            consoleTextArea.positionCaret(lastOutputLength);
            consoleTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}