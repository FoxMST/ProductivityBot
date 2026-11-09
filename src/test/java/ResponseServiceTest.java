import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Тесты для проверки логики формирования ответов ботом.
 * Все тесты используют фиксированные часы, поэтому результат не зависит от реальной даты запуска.
 */
class ResponseServiceTest {

    /**
     * Идентификатор чата, используемый во всех тестах.
     */
    private final long chatId = 1L;

    /**
     * Сообщение об отмене текущего сценария.
     */
    private final String cancelled = "Черновик задачи удален. Введите любую команду.";

    /**
     * Сообщение о том, что действие (кнопка) больше недоступно.
     */
    private final String stale = "Это действие уже недоступно. Введите любую команду.";

    /**
     * Сообщение об ошибке формата даты.
     */
    private final String badDate = "❌ Неправильный формат. Введите ДД.ММ.ГГГГ";

    /**
     * Заголовок списка задач на 10.12.2026.
     */
    private final String listHeaderToday =
            "📋 Задачи на 10.12.2026. Формат: (Номер) - (Задача) - (ID) - (Время) - (Дедлайн) - (Статус):";

    /**
     * Полный текст справки.
     */
    private final String expectedHelp =
            "📖 Доступные команды:\n"
                    + "/start — начать\n"
                    + "/help — справка\n"
                    + "/add_task — добавить задачу\n"
                    + "/show_tasklist — показать задачи за день\n"
                    + "/edit_task — изменить задачу\n"
                    + "/delete_task — удалить задачу\n"
                    + "/mark_as_done — отметить выполненные\n"
                    + "Во время любой команды можно нажать ❌ Отмена.\n"
                    + "Просто напиши что-нибудь — я повторю.";

    /**
     * Тестируемый объект (время зафиксировано: 10.12.2026).
     */
    private ResponseService bot;

    /**
     * Создает новый экземпляр ResponseService с фиксированными часами перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-10T10:00:00Z"), ZoneOffset.UTC);
        bot = new ResponseService(clock, new TaskStorage());
    }

    /**
     * Отправляет текстовое сообщение в логику бота и возвращает объединенный текст.
     *
     * @param text текст сообщения
     * @return объединенный текст ответов
     */
    private String say(String text) {
        return join(bot.handleMessage(chatId, text));
    }

    /**
     * Нажимает inline-кнопку и возвращает объединенный текст.
     *
     * @param action действие кнопки
     * @return объединенный текст ответов
     */
    private String press(ButtonAction action) {
        return join(bot.handleAction(chatId, action.id()));
    }

    /**
     * Склеивает тексты всех ответов бота в одну строку через перенос строки.
     *
     * @param responses список ответов бота
     * @return объединенный текст
     */
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

    /**
     * Вспомогательный метод для быстрого добавления задачи без времени и дедлайна.
     */
    private void addDefaultTask() {
        say("/add_task");
        say("Купить хлеб");
        say("10.12.2026");
        press(ButtonAction.NO);
        press(ButtonAction.NO);
    }

    /**
     * Проверяет, что на команду /start бот возвращает приветствие.
     */
    @Test
    void shouldReturnGreetingForStart() {
        Assertions.assertEquals("👋 Привет! Я бот-продуктивность.\n"
                + "Напиши /help, чтобы увидеть команды.", say("/start"));
    }

    /**
     * Проверяет, что на команду /help бот возвращает полный текст справки.
     */
    @Test
    void shouldReturnHelp() {
        Assertions.assertEquals(expectedHelp, say("/help"));
    }

    /**
     * Проверяет, что в состоянии IDLE обычный текст возвращается пользователю как эхо.
     */
    @Test
    void shouldEchoText() {
        Assertions.assertEquals("Вы ввели: «привет»", say("привет"));
    }

    /**
     * Проверяет полный цикл добавления задачи и её отображения в списке за сегодняшний день.
     */
    @Test
    void shouldAddTaskAndShowIt() {
        addDefaultTask();

        Assertions.assertEquals("За какой день показать задачи?", say("/show_tasklist"));
        Assertions.assertEquals(listHeaderToday + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет, что команда прерывает сценарий добавления задачи.
     */
    @Test
    void commandShouldEscapeAddTaskFlow() {
        say("/add_task");

        Assertions.assertEquals(expectedHelp, say("/help"));

        say("/show_tasklist");
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", press(ButtonAction.TODAY));
    }

    /**
     * Проверяет, что нажатие кнопки отмены во время добавления задачи сбрасывает сценарий.
     */
    @Test
    void shouldCancelAddTask() {
        say("/add_task");
        Assertions.assertEquals(cancelled, press(ButtonAction.CANCEL));
        Assertions.assertEquals("Вы ввели: «Купить хлеб»", say("Купить хлеб"));
    }

    /**
     * Проверяет, что при отсутствии задач на выбранный день бот сообщает об этом.
     */
    @Test
    void shouldReportEmptyDay() {
        say("/show_tasklist");
        Assertions.assertEquals("📋 На 11.12.2026 задач нет.", press(ButtonAction.TOMORROW));
    }

    /**
     * Проверяет, что при неверном формате даты бот просит ввести дату повторно.
     */
    @Test
    void shouldAskDateAgainOnWrongFormat() {
        say("/show_tasklist");
        Assertions.assertEquals("Введите дату (ДД.ММ.ГГГГ)", press(ButtonAction.CUSTOM_DATE));
        Assertions.assertEquals(badDate, say("10.12.26"));
        Assertions.assertEquals(badDate, say("31.02.2026"));
        Assertions.assertEquals("📋 На 10.12.2026 задач нет.", say("10.12.2026"));
    }

    /**
     * Проверяет удаление задачи по её номеру из списка.
     */
    @Test
    void shouldDeleteByNumberFromList() {
        addDefaultTask();
        say("/add_task");
        say("Позвонить маме");
        say("10.12.2026");
        press(ButtonAction.NO);
        press(ButtonAction.NO);

        Assertions.assertEquals("Отправьте ID задачи или выберите день для удаления:", say("/delete_task"));
        Assertions.assertEquals(listHeaderToday + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕\n2. Позвонить маме - 2 - 10.12.2026 - ⭕\n"
                        + "Напишите номер задачи, которую хотите удалить.",
                press(ButtonAction.TODAY));

        // Проверяем запрос подтверждения и затем само удаление
        Assertions.assertEquals("Удалить задачу «Купить хлеб» ?", say("1"));
        Assertions.assertEquals("🗑 Задача удалена.", press(ButtonAction.DELETE_CONFIRM));

        say("/show_tasklist");
        Assertions.assertEquals(listHeaderToday + "\n1. Позвонить маме - 2 - 10.12.2026 - ⭕",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет удаление задачи по ID с подтверждением через кнопку.
     */
    @Test
    void shouldDeleteByIdWithConfirmation() {
        addDefaultTask();

        say("/delete_task");
        Assertions.assertEquals("Удалить задачу «Купить хлеб» ?", say("1"));
        Assertions.assertEquals("🗑 Задача удалена.", press(ButtonAction.DELETE_CONFIRM));
    }

    /**
     * Проверяет, что при отмене подтверждения удаления задача остается в хранилище.
     */
    @Test
    void shouldNotDeleteWhenCancelled() {
        addDefaultTask();

        say("/delete_task");
        say("1");
        Assertions.assertEquals(cancelled, press(ButtonAction.CANCEL));

        say("/show_tasklist");
        Assertions.assertEquals(listHeaderToday + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕",
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
     * Проверяет сообщение об ошибке при вводе нечислового значения вместо ID.
     */
    @Test
    void shouldAskForIdOrDayOnNonNumber() {
        say("/delete_task");
        Assertions.assertEquals("❌ Введите ID задачи (число) или выберите день кнопкой.", say("abc"));
    }

    /**
     * Проверяет сообщение об ошибке при вводе номера вне диапазона задач.
     */
    @Test
    void shouldRejectWrongNumberInList() {
        addDefaultTask();
        say("/delete_task");
        press(ButtonAction.TODAY);
        Assertions.assertEquals("❌ Введите номер задачи из списка (от 1 до 1).", say("5"));
    }

    /**
     * Проверяет полный цикл редактирования текста задачи по номеру из списка.
     */
    @Test
    void shouldEditTextByNumberFromList() {
        addDefaultTask();

        Assertions.assertEquals("Отправьте ID задачи или выберите день для редактирования:", say("/edit_task"));
        Assertions.assertEquals(listHeaderToday + "\n1. Купить хлеб - 1 - 10.12.2026 - ⭕\n"
                        + "Напишите номер задачи, которую хотите изменить.",
                press(ButtonAction.TODAY));
        Assertions.assertEquals("Что изменить?", say("1"));
        Assertions.assertEquals("Введите новый текст", press(ButtonAction.EDIT_TEXT));
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));

        say("/show_tasklist");
        Assertions.assertEquals(listHeaderToday + "\n1. Купить масло - 1 - 10.12.2026 - ⭕",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет редактирование текста задачи по её ID.
     */
    @Test
    void shouldEditTextById() {
        addDefaultTask();

        say("/edit_task");
        Assertions.assertEquals("Что изменить?", say("1"));
        press(ButtonAction.EDIT_TEXT);
        Assertions.assertEquals("✅ Задача обновлена.", say("Купить масло"));
    }

    /**
     * Проверяет, что на этапе ввода нового текста можно отменить редактирование.
     */
    @Test
    void shouldCancelEditOnNewTextStep() {
        addDefaultTask();

        say("/edit_task");
        say("1");
        press(ButtonAction.EDIT_TEXT);
        Assertions.assertEquals(cancelled, press(ButtonAction.CANCEL));
    }

    /**
     * Проверяет обработку нажатия устаревшей или неизвестной кнопки.
     */
    @Test
    void staleButtonShouldBeHandled() {
        Assertions.assertEquals(stale, press(ButtonAction.TODAY));
        Assertions.assertEquals(stale, join(bot.handleAction(chatId, "UNKNOWN")));
    }

    /**
     * Проверяет добавление задачи с указанием времени и дедлайна.
     */
    @Test
    void shouldAddTaskWithTimeAndDeadline() {
        say("/add_task");
        say("Сделать дз");
        say("10.12.2026");

        press(ButtonAction.YES);
        say("18:30");

        press(ButtonAction.YES);
        Assertions.assertEquals("✅ Задача добавлена.", say("12.12.2026 20:00"));

        say("/show_tasklist");

        String expectedList = listHeaderToday + "\n1. Сделать дз - 1 - 10.12.2026 18:30 → дедлайн 12.12.2026 20:00 - ⭕";
        Assertions.assertEquals(expectedList, press(ButtonAction.TODAY));
    }

    /**
     * Проверяет пометку задачи выполненной через список и кнопку.
     */
    @Test
    void shouldMarkTaskAsDoneFromList() {
        addDefaultTask();

        say("/show_tasklist");
        press(ButtonAction.TODAY);

        Assertions.assertEquals("Выберите номер/номера задач в списке, которые необходимо пометить выполненными. Если задач несколько введите номера через пробел.",
                press(ButtonAction.MARK_DONE));

        Assertions.assertEquals("✅ Задачи помечены как выполненные (1 шт.)", say("1"));

        say("/show_tasklist");
        Assertions.assertEquals(listHeaderToday + "\n1. Купить хлеб - 1 - 10.12.2026 - ✅",
                press(ButtonAction.TODAY));
    }

    /**
     * Проверяет редактирование даты и времени выполнения задачи.
     */
    @Test
    void shouldEditDateTimeAndDeadline() {
        addDefaultTask();

        say("/edit_task");
        say("1");

        press(ButtonAction.EDIT_DATETIME);
        Assertions.assertEquals("✅ Задача обновлена.", say("11.12.2026 15:00"));
    }
}