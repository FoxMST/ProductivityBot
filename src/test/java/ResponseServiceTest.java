import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Тесты для проверки логики формирования ответов.
 */
class ResponseServiceTest {

    private static final long CHAT = 1L;

    private static final String CANCELLED =
            "Черновик задачи удален. Введите любую команду.";
    private static final String STALE =
            "Это действие уже недоступно. Введите любую команду.";
    private static final String BAD_DATE =
            "❌Неправильный ввод формата для даты, напишите повторно дату в формате ДД.ММ.ГГГГ";
    private static final String LIST_HEADER_TODAY =
            "📋 Задачи на 10.12.2026. Формат вывода (Номер задачи в списке) - (задача) - (ID):";

    /**
     * Тестируемый объект (время зафиксировано: 10.12.2026).
     */
    private ResponseService bot;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-10T10:00:00Z"), ZoneOffset.UTC);
        bot = new ResponseService(clock, new TaskStorage());
    }

    private String say(String text) {
        return join(bot.handleMessage(CHAT, text));
    }

    private String press(Button.ButtonAction action) {
        return join(bot.handleAction(CHAT, action.id()));
    }

    private static String join(List<BotResponse> responses) {
        StringBuilder sb = new StringBuilder();
        for (BotResponse r : responses) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(r.getText());
        }
        return sb.toString();
    }

    // проверка эхо

    @Test
    void shouldReturnGreetingForStart() {
        Assertions.assertEquals("👋 Привет! Я бот-продуктивность.\n"
                + "Напиши /help, чтобы увидеть команды.", say("/start"));
    }

    @Test
    void shouldReturnHelp() {
        Assertions.assertEquals("📖 Доступные команды:\n"
                + "/start — начать\n"
                + "/help — справка\n"
                + "/add_task — добавить задачу\n"
                + "/show_tasklist — показать задачи за день\n"
                + "/edit_task — изменить задачу\n"
                + "/delete_task — удалить задачу\n"
                + "Во время любой команды можно нажать ❌ Отмена.\n"
                + "Просто напиши что-нибудь — я повторю.", say("/help"));
    }

    @Test
    void shouldEchoText() {
        Assertions.assertEquals("Вы ввели: «привет»", say("привет"));
    }

    @Test
    void shouldEchoUnknownCommand() {
        Assertions.assertEquals("Вы ввели: «/unknown»", say("/unknown"));
    }

    // проверка /add_task

    @Test
    void shouldAddTaskAndShowIt() {
        Assertions.assertEquals("📝 Введите текст задачи", say("/add_task"));
        Assertions.assertEquals("✅ Задача добавлена.", say("Купить хлеб"));

        Assertions.assertEquals("За какой день показать задачи?", say("/show_tasklist"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1",
                press(Button.ButtonAction.TODAY));
    }

    @Test
    void addTaskShouldTreatCommandAsText() {
        say("/add_task");
        Assertions.assertEquals("✅ Задача добавлена.", say("/help"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. /help - 1",
                press(Button.ButtonAction.TODAY));
    }

    @Test
    void shouldCancelAddTask() {
        say("/add_task");
        Assertions.assertEquals(CANCELLED, press(Button.ButtonAction.CANCEL));
        Assertions.assertEquals("Вы ввели: «Купить хлеб»", say("Купить хлеб"));
    }

    // проверка /show_tasklist

    @Test
    void shouldReportEmptyDay() {
        say("/show_tasklist");
        Assertions.assertEquals("📋 На 11.12.2026 задач нет.", press(Button.ButtonAction.TOMORROW));
    }

    @Test
    void shouldAskDateAgainOnWrongFormat() {
        say("/show_tasklist");
        Assertions.assertEquals("Введите дату в формате ДД.ММ.ГГГГ",
                press(Button.ButtonAction.CUSTOM_DATE));
        Assertions.assertEquals(BAD_DATE, say("10.12.26"));
        Assertions.assertEquals(BAD_DATE, say("31.02.2026"));
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", say("10.12.2026"));
    }

    // проверка /delete_task

    @Test
    void shouldDeleteByNumberFromList() {
        say("/add_task");
        say("Купить хлеб");
        say("/add_task");
        say("Позвонить маме");

        Assertions.assertEquals("Отправьте ID задачи или выберите за какой день удалить задачу:",
                say("/delete_task"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1\n2. Позвонить маме - 2\n"
                        + "Напишите номер задачи в списке, которую хотите удалить.",
                press(Button.ButtonAction.TODAY));
        Assertions.assertEquals("🗑 Задача удалена.", say("1"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Позвонить маме - 2",
                press(Button.ButtonAction.TODAY));
    }

    @Test
    void shouldDeleteByIdWithConfirmation() {
        say("/add_task");
        say("Купить хлеб");

        say("/delete_task");
        Assertions.assertEquals("Удалить задачу «Купить хлеб» ?", say("1"));
        Assertions.assertEquals("🗑 Задача удалена.", press(Button.ButtonAction.DELETE_CONFIRM));

        say("/show_tasklist");
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", press(Button.ButtonAction.TODAY));
    }

    @Test
    void shouldNotDeleteWhenCancelled() {
        say("/add_task");
        say("Купить хлеб");

        say("/delete_task");
        say("1");
        Assertions.assertEquals(CANCELLED, press(Button.ButtonAction.CANCEL));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1",
                press(Button.ButtonAction.TODAY));
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
        Assertions.assertEquals("❌ Введите ID задачи (число) или выберите день кнопкой.",
                say("abc"));
    }

    @Test
    void shouldRejectWrongNumberInList() {
        say("/add_task");
        say("Купить хлеб");
        say("/delete_task");
        press(Button.ButtonAction.TODAY);
        Assertions.assertEquals("❌ Введите номер задачи из списка (от 1 до 1).", say("5"));
    }

    // проверка /edit_task

    @Test
    void shouldEditTextByNumberFromList() {
        say("/add_task");
        say("Купить хлеб");

        Assertions.assertEquals("Отправьте ID задачи или выберите за какой день редактировать:",
                say("/edit_task"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1\n"
                        + "Напишите номер задачи в списке, которую хотите изменить.",
                press(Button.ButtonAction.TODAY));
        Assertions.assertEquals("Что изменить?", say("1"));
        Assertions.assertEquals("Введите новый текст для задачи",
                press(Button.ButtonAction.EDIT_TEXT));
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить масло - 1",
                press(Button.ButtonAction.TODAY));
    }

    @Test
    void shouldEditTextById() {
        say("/add_task");
        say("Купить хлеб");

        say("/edit_task");
        Assertions.assertEquals("Что изменить?", say("1"));
        press(Button.ButtonAction.EDIT_TEXT);
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));
    }

    @Test
    void shouldCancelEditOnNewTextStep() {
        say("/add_task");
        say("Купить хлеб");

        say("/edit_task");
        say("1");
        press(Button.ButtonAction.EDIT_TEXT);
        Assertions.assertEquals(CANCELLED, press(Button.ButtonAction.CANCEL));
    }

    // проверка устаревших кнопок

    @Test
    void staleButtonShouldBeHandled() {
        Assertions.assertEquals(STALE, press(Button.ButtonAction.TODAY));
        Assertions.assertEquals(STALE, join(bot.handleAction(CHAT, "UNKNOWN")));
    }
}