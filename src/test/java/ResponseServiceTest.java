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

    /**
     * Идентификатор чата, используемый во всех тестах.
     */
    private static final long CHAT = 1L;

    /**
     * Сообщение об отмене текущего сценария.
     */
    private static final String CANCELLED =
            "Черновик задачи удален. Введите любую команду.";

    /**
     * Сообщение о том, что действие (кнопка) больше недоступно.
     */
    private static final String STALE =
            "Это действие уже недоступно. Введите любую команду.";

    /**
     * Сообщение об ошибке формата даты.
     */
    private static final String BAD_DATE =
            "❌Неправильный ввод формата для даты, напишите повторно дату в формате ДД.ММ.ГГГГ";

    /**
     * Заголовок списка задач на 10.12.2026 (сегодняшняя дата в тестах).
     */
    private static final String LIST_HEADER_TODAY =
            "📋 Задачи на 10.12.2026. Формат вывода (Номер задачи в списке) - (задача) - (ID):";

    /**
     * Тестируемый объект (время зафиксировано: 10.12.2026).
     */
    private ResponseService bot;

    /**
     * Создает новый экземпляр {@link ResponseService} с фиксированными
     * часами и пустым хранилищем перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-10T10:00:00Z"), ZoneOffset.UTC);
        bot = new ResponseService(clock, new TaskStorage());
    }

    /**
     * Отправляет текстовое сообщение в логику бота и возвращает
     * объединенный текст всех ответов.
     *
     * @param text текст сообщения
     * @return объединенный текст ответов через перевод строки
     */
    private String say(String text) {
        return join(bot.handleMessage(CHAT, text));
    }

    /**
     * Нажимает inline-кнопку и возвращает объединенный текст всех ответов.
     *
     * @param action действие кнопки
     * @return объединенный текст ответов через перевод строки
     */
    private String press(ButtonAction action) {
        return join(bot.handleAction(CHAT, action.id()));
    }

    /**
     * Склеивает тексты всех ответов бота через перевод строки.
     *
     * @param responses список ответов бота
     * @return объединенный текст
     */
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

    /**
     * Проверяет, что на команду {@code /start} бот возвращает приветствие.
     */
    @Test
    void shouldReturnGreetingForStart() {
        Assertions.assertEquals("👋 Привет! Я бот-продуктивность.\n"
                + "Напиши /help, чтобы увидеть команды.", say("/start"));
    }

    /**
     * Проверяет, что на команду {@code /help} бот возвращает справку,
     * содержащую ключевые фрагменты.
     */
    @Test
    void shouldReturnHelp() {
        String help = say("/help");
        Assertions.assertAll(
                () -> Assertions.assertTrue(help.contains("📖 Доступные команды"),
                        "Ожидался заголовок справки, пришло: " + help),
                () -> Assertions.assertTrue(help.contains("/add_task — добавить задачу"),
                        "В справке нет команды /add_task, пришло: " + help),
                () -> Assertions.assertTrue(help.contains("Просто напиши что-нибудь — я повторю."),
                        "В справке нет фразы про эхо, пришло: " + help)
        );
    }

    /**
     * Проверяет, что в состоянии {@code IDLE} обычный текст возвращается
     * пользователю как эхо.
     */
    @Test
    void shouldEchoText() {
        Assertions.assertEquals("Вы ввели: «привет»", say("привет"));
    }

    /**
     * Проверяет, что неизвестная команда воспринимается как обычный текст
     * и возвращается как эхо.
     */
    @Test
    void shouldEchoUnknownCommand() {
        Assertions.assertEquals("Вы ввели: «/unknown»", say("/unknown"));
    }

    /**
     * Проверяет полный цикл добавления задачи и её отображения
     * в списке за сегодняшний день.
     */
    @Test
    void shouldAddTaskAndShowIt() {
        Assertions.assertEquals("📝 Введите текст задачи", say("/add_task"));
        Assertions.assertEquals("✅ Задача добавлена.", say("Купить хлеб"));

        Assertions.assertEquals("За какой день показать задачи?", say("/show_tasklist"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет, что команда прерывает сценарий добавления задачи:
     * выводится справка, а задача с текстом команды не создается.
     */
    @Test
    void commandShouldEscapeAddTaskFlow() {
        say("/add_task");

        String help = say("/help");
        Assertions.assertTrue(help.contains("📖 Доступные команды"),
                "Команда должна была прервать сценарий, пришло: " + help);

        say("/show_tasklist");
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", press(ButtonAction.TODAY));
    }

    /**
     * Проверяет, что нажатие кнопки отмены во время добавления задачи
     * сбрасывает сценарий, и следующий текст обрабатывается как эхо.
     */
    @Test
    void shouldCancelAddTask() {
        say("/add_task");
        Assertions.assertEquals(CANCELLED, press(ButtonAction.CANCEL));
        Assertions.assertEquals("Вы ввели: «Купить хлеб»", say("Купить хлеб"));
    }

    /**
     * Проверяет, что при отсутствии задач на выбранный день бот
     * сообщает об этом.
     */
    @Test
    void shouldReportEmptyDay() {
        say("/show_tasklist");
        Assertions.assertEquals("📋 На 11.12.2026 задач нет.", press(ButtonAction.TOMORROW));
    }

    /**
     * Проверяет, что при неверном формате даты бот просит ввести дату
     * повторно, включая случаи с несуществующей датой (31.02.2026).
     */
    @Test
    void shouldAskDateAgainOnWrongFormat() {
        say("/show_tasklist");
        Assertions.assertEquals("Введите дату в формате ДД.ММ.ГГГГ",
                press(ButtonAction.CUSTOM_DATE));
        Assertions.assertEquals(BAD_DATE, say("10.12.26"));
        Assertions.assertEquals(BAD_DATE, say("31.02.2026"));
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", say("10.12.2026"));
    }

    /**
     * Проверяет удаление задачи по её номеру из списка, показанного
     * после выбора дня.
     */
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
                press(ButtonAction.TODAY));
        Assertions.assertEquals("🗑 Задача удалена.", say("1"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Позвонить маме - 2",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет удаление задачи по ID с подтверждением через кнопку.
     */
    @Test
    void shouldDeleteByIdWithConfirmation() {
        say("/add_task");
        say("Купить хлеб");

        say("/delete_task");
        Assertions.assertEquals("Удалить задачу «Купить хлеб» ?", say("1"));
        Assertions.assertEquals("🗑 Задача удалена.", press(ButtonAction.DELETE_CONFIRM));

        say("/show_tasklist");
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", press(ButtonAction.TODAY));
    }

    /**
     * Проверяет, что при отмене подтверждения удаления задача остается
     * в хранилище.
     */
    @Test
    void shouldNotDeleteWhenCancelled() {
        say("/add_task");
        say("Купить хлеб");

        say("/delete_task");
        say("1");
        Assertions.assertEquals(CANCELLED, press(ButtonAction.CANCEL));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет сообщение об ошибке при вводе несуществующего ID задачи.
     */
    @Test
    void shouldReportUnknownId() {
        say("/delete_task");
        Assertions.assertEquals(
                "❌ Задача с ID 999 не найдена. Введите другой ID или выберите день:",
                say("999"));
    }

    /**
     * Проверяет сообщение об ошибке при вводе нечислового значения
     * вместо ID задачи.
     */
    @Test
    void shouldAskForIdOrDayOnNonNumber() {
        say("/delete_task");
        Assertions.assertEquals("❌ Введите ID задачи (число) или выберите день кнопкой.",
                say("abc"));
    }

    /**
     * Проверяет сообщение об ошибке при вводе номера вне диапазона
     * задач в показанном списке.
     */
    @Test
    void shouldRejectWrongNumberInList() {
        say("/add_task");
        say("Купить хлеб");
        say("/delete_task");
        press(ButtonAction.TODAY);
        Assertions.assertEquals("❌ Введите номер задачи из списка (от 1 до 1).", say("5"));
    }

    /**
     * Проверяет полный цикл редактирования задачи по номеру из списка.
     */
    @Test
    void shouldEditTextByNumberFromList() {
        say("/add_task");
        say("Купить хлеб");

        Assertions.assertEquals("Отправьте ID задачи или выберите за какой день редактировать:",
                say("/edit_task"));
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить хлеб - 1\n"
                        + "Напишите номер задачи в списке, которую хотите изменить.",
                press(ButtonAction.TODAY));
        Assertions.assertEquals("Что изменить?", say("1"));
        Assertions.assertEquals("Введите новый текст для задачи",
                press(ButtonAction.EDIT_TEXT));
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));

        say("/show_tasklist");
        Assertions.assertEquals(LIST_HEADER_TODAY + "\n1. Купить масло - 1",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет редактирование задачи по её ID (без выбора дня).
     */
    @Test
    void shouldEditTextById() {
        say("/add_task");
        say("Купить хлеб");

        say("/edit_task");
        Assertions.assertEquals("Что изменить?", say("1"));
        press(ButtonAction.EDIT_TEXT);
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));
    }

    /**
     * Проверяет, что на этапе ввода нового текста задачи можно
     * отменить редактирование кнопкой отмены.
     */
    @Test
    void shouldCancelEditOnNewTextStep() {
        say("/add_task");
        say("Купить хлеб");

        say("/edit_task");
        say("1");
        press(ButtonAction.EDIT_TEXT);
        Assertions.assertEquals(CANCELLED, press(ButtonAction.CANCEL));
    }

    /**
     * Проверяет, что нажатие устаревшей или неизвестной кнопки
     * возвращает сообщение о недоступности действия.
     */
    @Test
    void staleButtonShouldBeHandled() {
        Assertions.assertEquals(STALE, press(ButtonAction.TODAY));
        Assertions.assertEquals(STALE, join(bot.handleAction(CHAT, "UNKNOWN")));
    }
}