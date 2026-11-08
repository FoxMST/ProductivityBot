import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Тесты для проверки логики формирования ответов ботом.
 * Все тесты используют фиксированные часы ({@link Clock#fixed}),
 * поэтому результат не зависит от реальной даты запуска.
 */
class ResponseServiceTest {

    private static final long CHAT = 1L;
    private static final String CANCELLED = "Черновик задачи удален. Введите любую команду.";
    private static final String STALE = "Это действие уже недоступно. Введите любую команду.";
    private static final String BAD_DATE = "❌ Неправильный формат. Введите ДД.ММ.ГГГГ";

    private static final String LIST_HEADER_TODAY =
            "📋 Задачи на 10.12.2026. Формат: (Номер) - (Задача) - (ID) - (Время) - (Дедлайн) - (Статус):";

    private ResponseService bot;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-10T10:00:00Z"), ZoneOffset.UTC);
        bot = new ResponseService(clock, new TaskStorage());
    }

    private String say(String text) {
        return join(bot.handleMessage(CHAT, text));
    }

    private String press(ButtonAction action) {
        return join(bot.handleAction(CHAT, action.id()));
    }

    private String join(List<BotResponse> responses) {
        StringBuilder sb = new StringBuilder();
        for (BotResponse r : responses) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(r.getText());
        }
        return sb.toString();
    }

    private void addDefaultTask() {
        say("/add_task");
        say("Купить хлеб");
        say("10.12.2026");
        press(ButtonAction.NO); // без времени
        press(ButtonAction.NO); // без дедлайна
    }

    @Test
    void shouldReturnGreetingForStart() {
        Assertions.assertEquals("👋 Привет! Я бот-продуктивность.\n"
                + "Напиши /help, чтобы увидеть команды.", say("/start"));
    }

    @Test
    void shouldReturnHelp() {
        String help = say("/help");
        Assertions.assertAll(
                () -> Assertions.assertTrue(help.contains("📖 Доступные команды"), "Нет заголовка"),
                () -> Assertions.assertTrue(help.contains("/add_task — добавить задачу"), "Нет /add_task"),
                () -> Assertions.assertTrue(help.contains("/mark_as_done — отметить выполненные"), "Нет /mark_as_done")
        );
    }

    @Test
    void shouldEchoText() {
        Assertions.assertEquals("Вы ввели: «привет»", say("привет"));
    }

    @Test
    void shouldAddTaskAndShowIt() {
        addDefaultTask();

        Assertions.assertEquals("За какой день показать задачи?", say("/show_tasklist"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕",
                press(ButtonAction.TODAY));
    }

    @Test
    void commandShouldEscapeAddTaskFlow() {
        say("/add_task");
        say("/help");
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", say("/show_tasklist").contains("За какой день")
                ? press(ButtonAction.TODAY) : "ошибка");
    }

    @Test
    void shouldCancelAddTask() {
        say("/add_task");
        Assertions.assertEquals(CANCELLED, press(ButtonAction.CANCEL));
        Assertions.assertEquals("Вы ввели: «Купить хлеб»", say("Купить хлеб"));
    }

    @Test
    void shouldReportEmptyDay() {
        say("/show_tasklist");
        Assertions.assertEquals("📋 На 11.12.2026 задач нет.", press(ButtonAction.TOMORROW));
    }

    @Test
    void shouldAskDateAgainOnWrongFormat() {
        say("/show_tasklist");
        Assertions.assertEquals("Введите дату (ДД.ММ.ГГГГ)", press(ButtonAction.CUSTOM_DATE));
        Assertions.assertEquals(BAD_DATE, say("10.12.26"));
        Assertions.assertEquals(BAD_DATE, say("31.02.2026"));
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", say("10.12.2026"));
    }

    @Test
    void shouldDeleteByNumberFromList() {
        addDefaultTask();
        say("/add_task");
        say("Позвонить маме");
        say("10.12.2026");
        press(ButtonAction.NO);
        press(ButtonAction.NO);

        Assertions.assertEquals("Отправьте ID задачи или выберите день для удаления:", say("/delete_task"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕\n2. Позвонить маме - 2 - 10.12.2026 - ⭕\n"
                        + "Напишите номер задачи, которую хотите удалить.",
                press(ButtonAction.TODAY));
        Assertions.assertEquals("🗑 Задача удалена.", say("1"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Позвонить маме - 2 - 10.12.2026 - ⭕",
                press(ButtonAction.TODAY));
    }

    @Test
    void shouldDeleteByIdWithConfirmation() {
        addDefaultTask();

        say("/delete_task");
        Assertions.assertEquals("Удалить задачу «Купить хлеб» ?", say("1"));
        Assertions.assertEquals("🗑 Задача удалена.", press(ButtonAction.DELETE_CONFIRM));
    }

    @Test
    void shouldNotDeleteWhenCancelled() {
        addDefaultTask();

        say("/delete_task");
        say("1");
        Assertions.assertEquals(CANCELLED, press(ButtonAction.CANCEL));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕",
                press(ButtonAction.TODAY));
    }

    @Test
    void shouldReportUnknownId() {
        say("/delete_task");
        Assertions.assertEquals(
                "❌ Задача с ID 999 не найдена. Введите другой ID или выберите день:",
                say("999"));
    }

    @Test
    void shouldAskForIdOrDayOnNonNumber() {
        say("/delete_task");
        Assertions.assertEquals("❌ Введите ID задачи (число) или выберите день кнопкой.", say("abc"));
    }

    @Test
    void shouldRejectWrongNumberInList() {
        addDefaultTask();
        say("/delete_task");
        press(ButtonAction.TODAY);
        Assertions.assertEquals("❌ Введите номер задачи из списка (от 1 до 1).", say("5"));
    }

    @Test
    void shouldEditTextByNumberFromList() {
        addDefaultTask();

        Assertions.assertEquals("Отправьте ID задачи или выберите день для редактирования:", say("/edit_task"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕\n"
                        + "Напишите номер задачи, которую хотите изменить.",
                press(ButtonAction.TODAY));
        Assertions.assertEquals("Что изменить?", say("1"));
        Assertions.assertEquals("Введите новый текст", press(ButtonAction.EDIT_TEXT));
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить масло - 1 - 10.12.2026 - ⭕",
                press(ButtonAction.TODAY));
    }

    @Test
    void shouldEditTextById() {
        addDefaultTask();

        say("/edit_task");
        Assertions.assertEquals("Что изменить?", say("1"));
        press(ButtonAction.EDIT_TEXT);
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));
    }

    @Test
    void shouldCancelEditOnNewTextStep() {
        addDefaultTask();

        say("/edit_task");
        say("1");
        press(ButtonAction.EDIT_TEXT);
        Assertions.assertEquals(CANCELLED, press(ButtonAction.CANCEL));
    }

    @Test
    void staleButtonShouldBeHandled() {
        Assertions.assertEquals(STALE, press(ButtonAction.TODAY));
        Assertions.assertEquals(STALE, join(bot.handleAction(CHAT, "UNKNOWN")));
    }

    @Test
    void shouldAddTaskWithTimeAndDeadline() {
        say("/add_task");
        say("Сделать дз");
        say("10.12.2026");

        press(ButtonAction.YES); // время
        say("18:30");

        press(ButtonAction.YES); // дедлайн
        Assertions.assertEquals("✅ Задача добавлена.", say("12.12.2026 20:00"));

        say("/show_tasklist");
        String list = press(ButtonAction.TODAY);
        Assertions.assertTrue(list.contains("18:30"));
        Assertions.assertTrue(list.contains("DL: 12.12.2026 20:00"));
    }

    @Test
    void shouldMarkTaskAsDoneFromList() {
        addDefaultTask();

        say("/show_tasklist");
        press(ButtonAction.TODAY);

        Assertions.assertEquals("Введите номера задач через пробел (например: 1 2)",
                press(ButtonAction.MARK_DONE));

        Assertions.assertEquals("✅ Задачи помечены как выполненные (1 шт.)", say("1"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1 - 10.12.2026 - ✅",
                press(ButtonAction.TODAY));
    }

    @Test
    void shouldEditDateTimeAndDeadline() {
        addDefaultTask();

        say("/edit_task");
        say("1");

        press(ButtonAction.EDIT_DATETIME);
        Assertions.assertEquals("✅ Задача обновлена.", say("11.12.2026 15:00"));
    }
}