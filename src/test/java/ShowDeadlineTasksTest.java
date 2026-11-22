import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Тесты показа задач с дедлайном в /show_tasklist.
 */
public class ShowDeadlineTasksTest {
    /**
     * Идентификатор тестового чата.
     */
    private static final long CHAT = 1L;
    /**
     * Заголовок списка для интервала 17.11.2026-19.11.2026.
     */
    private static final String HEADER =
            "📋 Задачи с дедлайном на интервале 17.11.2026-19.11.2026 "
                    + "в порядке срочности.\n"
                    + "Формат: (Номер) - (Задача) - (ID) - (Дата, время) - "
                    + "(Дедлайн) - (Статус):";
    /**
     * Хранилище с тестовыми задачами.
     */
    private TaskStorage storage;
    /**
     * Тестируемый сервис (сегодня — 16.11.2026).
     */
    private ResponseService bot;

    /**
     * Создает сервис и добавляет задачи: три с дедлайном в интервале
     * 17-19.11, одну без дедлайна и одну с дедлайном вне интервала.
     */
    @BeforeEach
    public void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"),
                ZoneId.of("UTC"));
        storage = new TaskStorage();
        bot = new ResponseService(clock, storage);
        storage.addTask("Купить хлеб", LocalDate.of(2026, 11, 17), null,
                LocalDate.of(2026, 12, 11), LocalTime.of(18, 0));
        storage.addTask("Позвонить маме", LocalDate.of(2026, 11, 19), null,
                LocalDate.of(2026, 12, 17), LocalTime.of(11, 0));
        storage.addTask("Написать отчёт", LocalDate.of(2026, 11, 18), null,
                LocalDate.of(2026, 12, 20), LocalTime.of(8, 0));
        storage.addTask("Без дедлайна", LocalDate.of(2026, 11, 17));
        storage.addTask("Далекая задача", LocalDate.of(2026, 11, 25), null,
                LocalDate.of(2026, 12, 1), null);
    }


    /**
     * Проверяет список за введенный интервал: порядок по срочности дедлайна.
     */
    @Test
    public void manualIntervalShouldShowTasksSortedByDeadline() {
        openIntervalQuestion();
        String expected = HEADER
                + "\n1. Купить хлеб - 1 - 17.11.2026 → дедлайн 11.12.2026 18:00 - ⭕"
                + "\n2. Позвонить маме - 2 - 19.11.2026 → дедлайн 17.12.2026 11:00 - ⭕"
                + "\n3. Написать отчёт - 3 - 18.11.2026 → дедлайн 20.12.2026 08:00 - ⭕";
        BotResponse response = bot.handleMessage(CHAT, "17.11.2026-19.11.2026").get(0);
        Assertions.assertEquals(expected, response.getText());
        Assertions.assertTrue(buttonIds(response).contains("MARK_DONE"));
    }


    /**
     * Проверяет, что «На эти 2 недели» захватывает задачу на 25.11.
     */
    @Test
    public void twoWeeksButtonShouldIncludeLaterTask() {
        openIntervalQuestion();
        String text = bot.handleAction(CHAT, "TWO_WEEKS").get(0).getText();
        Assertions.assertTrue(text.contains("16.11.2026-29.11.2026"));
        Assertions.assertTrue(text.contains("Далекая задача"));
    }

    /**
     * Проверяет, что «За месяц» показывает период до 16.12.2026.
     */
    @Test
    public void monthButtonShouldShowOneMonth() {
        openIntervalQuestion();
        String text = bot.handleAction(CHAT, "MONTH").get(0).getText();
        Assertions.assertTrue(text.contains("16.11.2026-16.12.2026"));
    }


    /**
     * Проверяет, что неверный интервал не ломает диалог.
     */
    @Test
    public void badIntervalShouldAskAgain() {
        openIntervalQuestion();
        String[] badInputs = {"abc", "17.11.2026", "19.11.2026-17.11.2026",
                "31.02.2026-01.03.2026"};
        for (String input : badInputs) {
            BotResponse response = bot.handleMessage(CHAT, input).get(0);
            Assertions.assertTrue(response.getText().startsWith("❌ Неправильный формат"));
            Assertions.assertTrue(buttonIds(response).contains("WEEK"));
        }
        String text = bot.handleMessage(CHAT, "17.11.2026-19.11.2026").get(0).getText();
        Assertions.assertTrue(text.startsWith("📋 Задачи с дедлайном"));
    }


    /**
     * Доходит до вопроса про интервал: /show_tasklist и кнопка «Только с дедлайном».
     */
    private void openIntervalQuestion() {
        bot.handleMessage(CHAT, "/show_tasklist");
        bot.handleAction(CHAT, "ONLY_DEADLINE");
    }

    /**
     * Собирает идентификаторы всех кнопок ответа.
     *
     * @param response ответ бота
     * @return идентификаторы действий кнопок
     */
    private List<String> buttonIds(BotResponse response) {
        List<String> ids = new ArrayList<>();
        for (List<ResponseButton> row : response.getKeyboard()) {
            for (ResponseButton button : row) {
                ids.add(button.getActionId());
            }
        }
        return ids;
    }
}