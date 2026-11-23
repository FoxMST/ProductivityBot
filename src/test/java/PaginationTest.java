import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Тесты постраничного вывода списков задач (по 10 задач на странице).
 * Время зафиксировано: 10.12.2026.
 */
class PaginationTest {

    /**
     * Идентификатор чата, используемый во всех тестах.
     */
    private static final long CHAT = 1L;
    /**
     * Заголовок списка /show_tasklist и /mark_as_done на 10.12.2026.
     */
    private static final String SHOW_HEADER =
            "📋 Задачи на 10.12.2026. Формат: (Номер) - (Задача) - (ID) - "
                    + "(Дата, время) - (Дедлайн) - (Статус):";
    /**
     * Сообщение об отмене текущего сценария.
     */
    private static final String CANCELLED =
            "Черновик задачи удален. Введите любую команду.";
    /**
     * Сообщение о том, что кнопка больше недоступна.
     */
    private static final String STALE =
            "Это действие уже недоступно. Введите любую команду.";

    /**
     * Хранилище, в которое тесты добавляют задачи.
     */
    private TaskStorage storage;
    /**
     * Тестируемый сервис.
     */
    private ResponseService bot;
    /**
     * День, на который добавляются задачи (10.12.2026).
     */
    private LocalDate today;

    /**
     * Создает сервис с фиксированными часами и пустым хранилищем.
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-10T10:00:00Z"), ZoneOffset.UTC);
        storage = new TaskStorage();
        bot = new ResponseService(clock, storage);
        today = LocalDate.of(2026, 12, 10);
    }

    /**
     * Проверяет, что до 10 задач включительно список не делится на страницы.
     */
    @Test
    void tenTasksShouldFitOnOnePage() {
        addTasks(10);
        BotResponse page = openList();
        Assertions.assertEquals(10, taskLines(page, 1).size());
        Assertions.assertEquals(Arrays.asList("MARK_DONE"), buttonIds(page));
        Assertions.assertEquals(1, page.getKeyboard().size());
    }

    /**
     * Проверяет, что 11-я задача создает вторую страницу.
     */
    @Test
    void elevenTasksShouldMakeTwoPages() {
        addTasks(11);
        BotResponse page = openList();
        Assertions.assertEquals(10, taskLines(page, 1).size());
        Assertions.assertEquals("1/2", pageLabel(page));
        BotResponse second = press(ButtonAction.NEXT_PAGE);
        Assertions.assertEquals(Arrays.asList(line(11)), taskLines(second, 1));
        Assertions.assertEquals("2/2", pageLabel(second));
    }

    /**
     * Проверяет первую страницу: 10 задач, ряд пагинации и кнопка пометки.
     */
    @Test
    void firstPageShouldHaveTenTasksAndPagerRow() {
        addTasks(25);
        BotResponse page = openList();
        List<String> lines = taskLines(page, 1);
        Assertions.assertEquals(SHOW_HEADER, page.getText().split("\n")[0]);
        Assertions.assertEquals(10, lines.size());
        Assertions.assertEquals(line(1), lines.get(0));
        Assertions.assertEquals(line(10), lines.get(9));
        Assertions.assertEquals(2, page.getKeyboard().size());
        Assertions.assertEquals(
                Arrays.asList("PREV_PAGE", "PAGE_INFO", "NEXT_PAGE", "MARK_DONE"),
                buttonIds(page));
        Assertions.assertEquals("1/3", pageLabel(page));
    }

    /**
     * Проверяет, что вторая страница показывает задачи 11-20 со сквозной нумерацией.
     */
    @Test
    void nextPageShouldShowTasksElevenToTwenty() {
        addTasks(25);
        openList();
        BotResponse page = press(ButtonAction.NEXT_PAGE);
        List<String> lines = taskLines(page, 1);
        Assertions.assertEquals(10, lines.size());
        Assertions.assertEquals(line(11), lines.get(0));
        Assertions.assertEquals(line(20), lines.get(9));
        Assertions.assertEquals("2/3", pageLabel(page));
    }

    /**
     * Проверяет, что последняя страница содержит остаток списка.
     */
    @Test
    void lastPageShouldHaveRemainder() {
        addTasks(25);
        openList();
        press(ButtonAction.NEXT_PAGE);
        BotResponse page = press(ButtonAction.NEXT_PAGE);
        List<String> lines = taskLines(page, 1);
        Assertions.assertEquals(5, lines.size());
        Assertions.assertEquals(line(21), lines.get(0));
        Assertions.assertEquals(line(25), lines.get(4));
        Assertions.assertEquals("3/3", pageLabel(page));
    }

    /**
     * Проверяет, что «вперед» на последней странице остается на ней.
     */
    @Test
    void nextOnLastPageShouldStay() {
        addTasks(25);
        openList();
        press(ButtonAction.NEXT_PAGE);
        press(ButtonAction.NEXT_PAGE);
        BotResponse page = press(ButtonAction.NEXT_PAGE);
        Assertions.assertEquals("3/3", pageLabel(page));
        Assertions.assertEquals(line(21), taskLines(page, 1).get(0));
    }


    /**
     * Проверяет, что «назад» возвращает на предыдущую страницу.
     */
    @Test
    void prevShouldReturnToPreviousPage() {
        addTasks(25);
        openList();
        press(ButtonAction.NEXT_PAGE);
        BotResponse page = press(ButtonAction.PREV_PAGE);
        Assertions.assertEquals("1/3", pageLabel(page));
        Assertions.assertEquals(line(1), taskLines(page, 1).get(0));
    }


    /**
     * Проверяет, что заголовок списка повторяется на каждой странице.
     */
    @Test
    void headerShouldBeOnEveryPage() {
        addTasks(25);
        BotResponse page = openList();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(SHOW_HEADER, page.getText().split("\n")[0]);
            page = press(ButtonAction.NEXT_PAGE);
        }
    }


    /**
     * Проверяет, что отмена на любой странице завершает сценарий,
     * а кнопки страниц после этого неактуальны.
     */
    @Test
    void cancelShouldWorkOnAnyPageAndDisablePageButtons() {
        addTasks(25);
        openList();
        press(ButtonAction.NEXT_PAGE);
        Assertions.assertEquals(CANCELLED, press(ButtonAction.CANCEL).getText());
        Assertions.assertEquals(STALE, press(ButtonAction.NEXT_PAGE).getText());
    }


    /**
     * Проверяет, что страницы работают и в списке задач с дедлайном.
     */
    @Test
    void deadlineListShouldBePaginatedToo() {
        for (int i = 1; i <= 12; i++) {
            storage.addTask("Задача " + i, today, null,
                    LocalDate.of(2027, 1, 1).plusDays(i), null);
        }
        bot.handleMessage(CHAT, "/show_tasklist");
        bot.handleAction(CHAT, ButtonAction.ONLY_DEADLINE.id());
        BotResponse first = bot.handleMessage(CHAT, "10.12.2026-10.12.2026").get(0);

        Assertions.assertTrue(first.getText().startsWith(
                "📋 Задачи с дедлайном на интервале 10.12.2026-10.12.2026"));
        Assertions.assertEquals("1/2", pageLabel(first));
        Assertions.assertEquals(10, taskLines(first, 2).size());

        BotResponse second = press(ButtonAction.NEXT_PAGE);
        List<String> lines = taskLines(second, 2);
        Assertions.assertEquals("2/2", pageLabel(second));
        Assertions.assertEquals(2, lines.size());
        Assertions.assertTrue(lines.get(0).startsWith(
                "11. Задача 11 - 11 - 10.12.2026 → дедлайн 12.01.2027"));
        Assertions.assertTrue(second.getText().startsWith(
                "📋 Задачи с дедлайном на интервале 10.12.2026-10.12.2026"));
    }


    /**
     * Добавляет на сегодня задачи «Задача 1» … «Задача N» (ID совпадает с номером).
     *
     * @param count сколько задач добавить
     */
    private void addTasks(int count) {
        for (int i = 1; i <= count; i++) {
            storage.addTask("Задача " + i, today);
        }
    }

    /**
     * Нажимает inline-кнопку.
     *
     * @param action действие кнопки
     * @return первый ответ бота
     */
    private BotResponse press(ButtonAction action) {
        return bot.handleAction(CHAT, action.id()).get(0);
    }


    /**
     * Собирает идентификаторы всех кнопок ответа в порядке их расположения.
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

    /**
     * Формирует ожидаемую строку списка для невыполненной задачи без времени.
     *
     * @param number номер задачи в списке (он же ID и номер в тексте)
     * @return строка вида «1. Задача 1 - 1 - 10.12.2026 - ⭕»
     */
    private String line(int number) {
        return number + ". Задача " + number + " - " + number + " - 10.12.2026 - ⭕";
    }
}