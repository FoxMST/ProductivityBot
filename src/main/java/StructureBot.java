import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Логика бот
 *
 */
public class StructureBot {
    private static final String START_MESSAGE =
            "👋 Привет! Я бот-продуктивность.\nНапиши /help, чтобы увидеть команды.";
    private static final String HELP_MESSAGE =
            "📖 Доступные команды:\n"
                    + "/start — начать\n"
                    + "/help — справка\n"
                    + "/add_task — добавить задачу\n"
                    + "/show_tasklist — показать задачи за день\n"
                    + "/edit_task — изменить задачу\n"
                    + "/delete_task — удалить задачу\n"
                    + "Во время любой команды можно нажать ❌ Отмена.\n"
                    + "Просто напиши что-нибудь — я повторю.";
    private static final String CANCEL_MESSAGE =
            "Черновик задачи удален. Введите любую команду.";
    private static final String STALE_MESSAGE =
            "Это действие уже недоступно. Введите любую команду.";
    private static final String BAD_DATE_MESSAGE =
            "❌Неправильный ввод формата для даты, напишите повторно дату в формате ДД.ММ.ГГГГ";
    private static final String NOT_FOUND_MESSAGE = "❌ Задача не найдена.";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Set<String> COMMANDS = new HashSet<>(Arrays.asList(
            "/start", "/help", "/add_task", "/show_tasklist", "/delete_task", "/edit_task"));

    /**
     * Какая команда выполняется
     */
    private enum Flow {
        SHOW, DELETE, EDIT
    }

    private final TaskStorage storage;
    private final Clock clock;
    private final Map<Long, UserSession> sessions = new HashMap<>();

    /**
     * Бот с системными часами и пустым хранилищем.
     */
    public StructureBot() {
        this(Clock.systemDefaultZone(), new TaskStorage());
    }

    /**
     * @param clock   часы
     * @param storage хранилище задач
     */
    public StructureBot(Clock clock, TaskStorage storage) {
        this.clock = clock;
        this.storage = storage;
    }

    // Вход: сообщения и кнопки

    /**
     * Обрабатывает текстовое сообщение пользователя.
     *
     * @param chatId ID чата
     * @param text   текст сообщения
     * @return ответы бота по порядку
     */
    public synchronized List<BotResponse> handleMessage(long chatId, String text) {
        UserSession s = sessions.computeIfAbsent(chatId, id -> new UserSession());
        State state = s.getState();

        // Ввод текста: любые команды считаются содержимым.
        if (state == State.ADD_TASK_WAIT_TEXT) {
            storage.addTask(text, LocalDate.now(clock));
            return finish(s, "✅ Задача добавлена.");
        }
        if (state == State.EDIT_TASK_WAIT_NEW_TEXT) {
            boolean updated = storage.updateTaskText(s.getPendingTaskId(), text);
            return finish(s, updated ? "✅ Задача обновлена." : NOT_FOUND_MESSAGE);
        }

        String command = extractCommand(text);
        if (COMMANDS.contains(command)) {
            return runCommand(command, s);
        }

        switch (state) {
            case IDLE:
                return reply(BotResponse.of("Вы ввели: «" + text + "»"));
            case SHOW_TASKLIST_WAIT_DATE:
                return reply(Button.dayChoice("Выберите день с помощью кнопок."));
            case DELETE_TASK_WAIT_ID_OR_DATE:
            case EDIT_TASK_WAIT_ID_OR_DATE:
                return onIdInput(s, text);
            case SHOW_TASKLIST_WAIT_CUSTOM_DATE:
            case DELETE_TASK_WAIT_CUSTOM_DATE:
            case EDIT_TASK_WAIT_CUSTOM_DATE:
                return onDateInput(s, text);
            case DELETE_TASK_WAIT_LIST_NUMBER:
            case EDIT_TASK_WAIT_LIST_NUMBER:
                return onNumberInput(s, text);
            default:
                return reply(Button.cancel("Выберите действие кнопками выше или нажмите ❌ Отмена."));
        }
    }

    /**
     * Обрабатывает действие, выбранное нажатием кнопки.
     *
     * @param chatId   ID чата
     * @param actionId идентификатор действия кнопки
     * @return ответы бота по порядку
     */
    public synchronized List<BotResponse> handleAction(long chatId, String actionId) {
        UserSession s = sessions.computeIfAbsent(chatId, id -> new UserSession());
        State state = s.getState();
        Button.ButtonAction action = Button.ButtonAction.fromId(actionId);

        if (action == Button.ButtonAction.CANCEL && state != State.IDLE) {
            return finish(s, CANCEL_MESSAGE);
        }
        if (state == State.DELETE_TASK_CONFIRM && action == Button.ButtonAction.DELETE_CONFIRM) {
            return deleteTask(s, s.getPendingTaskId());
        }
        if (state == State.EDIT_TASK_WAIT_FIELD && action == Button.ButtonAction.EDIT_TEXT) {
            return ask(s, State.EDIT_TASK_WAIT_NEW_TEXT, Button.cancel("Введите новый текст для задачи"));
        }
        if (isDayChoiceState(state) && action != null) {
            Flow flow = flowOf(state);
            switch (action) {
                case TODAY:
                    return showDay(s, LocalDate.now(clock), flow);
                case TOMORROW:
                    return showDay(s, LocalDate.now(clock).plusDays(1), flow);
                case CUSTOM_DATE:
                    State next = flow == Flow.SHOW ? State.SHOW_TASKLIST_WAIT_CUSTOM_DATE
                            : flow == Flow.DELETE ? State.DELETE_TASK_WAIT_CUSTOM_DATE
                            : State.EDIT_TASK_WAIT_CUSTOM_DATE;
                    return ask(s, next, Button.cancel("Введите дату в формате ДД.ММ.ГГГГ"));
                default:
                    break;
            }
        }
        return reply(BotResponse.of(STALE_MESSAGE));
    }

    // Команды

    private List<BotResponse> runCommand(String command, UserSession s) {
        reset(s);
        switch (command) {
            case "/start":
                return reply(BotResponse.of(START_MESSAGE));
            case "/help":
                return reply(BotResponse.of(HELP_MESSAGE));
            case "/add_task":
                return ask(s, State.ADD_TASK_WAIT_TEXT, Button.cancel("📝 Введите текст задачи"));
            case "/show_tasklist":
                return ask(s, State.SHOW_TASKLIST_WAIT_DATE,
                        Button.dayChoice("За какой день показать задачи?"));
            case "/delete_task":
                return ask(s, State.DELETE_TASK_WAIT_ID_OR_DATE, Button.dayChoice(
                        "Отправьте ID задачи или выберите за какой день удалить задачу:"));
            default: // "/edit_task"
                return ask(s, State.EDIT_TASK_WAIT_ID_OR_DATE, Button.dayChoice(
                        "Отправьте ID задачи или выберите за какой день редактировать:"));
        }
    }

    //  выбор дня / ID / номера

    private List<BotResponse> showDay(UserSession s, LocalDate date, Flow flow) {
        List<Task> tasks = storage.getTasksByDate(date);
        String day = DATE_FORMAT.format(date);
        if (tasks.isEmpty()) {
            return finish(s, "📋 На " + day + " задач нет.");
        }

        StringBuilder sb = new StringBuilder("📋 Задачи на " + day
                + ". Формат вывода (Номер задачи в списке) - (задача) - (ID):");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append("\n").append(i + 1).append(". ")
                    .append(tasks.get(i).getText()).append(" - ").append(tasks.get(i).getId());
        }
        BotResponse list = BotResponse.of(sb.toString());

        if (flow == Flow.SHOW) {
            reset(s);
            return reply(list);
        }
        boolean delete = flow == Flow.DELETE;
        s.setSelectedDate(date);
        s.setState(delete ? State.DELETE_TASK_WAIT_LIST_NUMBER : State.EDIT_TASK_WAIT_LIST_NUMBER);
        return reply(list, Button.cancel("Напишите номер задачи в списке, которую хотите "
                + (delete ? "удалить." : "изменить.")));
    }

    private List<BotResponse> onDateInput(UserSession s, String text) {
        try {
            return showDay(s, LocalDate.parse(text.trim(), DATE_FORMAT), flowOf(s.getState()));
        } catch (DateTimeParseException e) {
            return reply(Button.cancel(BAD_DATE_MESSAGE));
        }
    }

    private List<BotResponse> onNumberInput(UserSession s, String text) {
        List<Task> tasks = storage.getTasksByDate(s.getSelectedDate());
        Integer number = parseInt(text);
        if (number == null || number < 1 || number > tasks.size()) {
            return reply(Button.cancel("❌ Введите номер задачи из списка (от 1 до "
                    + tasks.size() + ")."));
        }
        return chooseTask(s, tasks.get(number - 1), true);
    }

    private List<BotResponse> onIdInput(UserSession s, String text) {
        Integer id = parseInt(text);
        Task task = id == null ? null : storage.getTaskById(id);
        if (task == null) {
            return reply(Button.dayChoice(id == null
                    ? "❌ Введите ID задачи (число) или выберите день кнопкой."
                    : "❌ Задача с ID " + id + " не найдена. Введите другой ID или выберите день:"));
        }
        return chooseTask(s, task, false);
    }

    /**
     * Задача выбрана (по ID или по номеру из списка): дальше редактирование или удаление.
     */
    private List<BotResponse> chooseTask(UserSession s, Task task, boolean byListNumber) {
        s.setPendingTaskId(task.getId());
        if (flowOf(s.getState()) == Flow.EDIT) {
            return ask(s, State.EDIT_TASK_WAIT_FIELD, Button.editField("Что изменить?"));
        }
        if (byListNumber) { // по номеру из списка удаляем сразу, по ID — с подтверждением
            return deleteTask(s, task.getId());
        }
        return ask(s, State.DELETE_TASK_CONFIRM,
                Button.deleteConfirm("Удалить задачу «" + task.getText() + "» ?"));
    }

    private List<BotResponse> deleteTask(UserSession s, int id) {
        return finish(s, storage.deleteTask(id) ? "🗑 Задача удалена." : NOT_FOUND_MESSAGE);
    }

    //  Помощники

    /**
     * Переводит диалог в новое состояние и задаёт пользователю вопрос.
     */
    private static List<BotResponse> ask(UserSession s, State next, BotResponse question) {
        s.setState(next);
        return reply(question);
    }

    /**
     * Завершает диалог: сбрасывает состояние и отвечает текстом.
     */
    private static List<BotResponse> finish(UserSession s, String text) {
        reset(s);
        return reply(BotResponse.of(text));
    }

    private static void reset(UserSession s) {
        s.setState(State.IDLE);
        s.setSelectedDate(null);
        s.setPendingTaskId(0);
    }

    private static List<BotResponse> reply(BotResponse... responses) {
        return Arrays.asList(responses);
    }

    /**
     * Ожидает ли бот нажатия кнопок выбора дня.
     */
    private static boolean isDayChoiceState(State state) {
        return state == State.SHOW_TASKLIST_WAIT_DATE
                || state == State.DELETE_TASK_WAIT_ID_OR_DATE
                || state == State.EDIT_TASK_WAIT_ID_OR_DATE;
    }

    /**
     * Определяет команду по названию состояния (SHOW_..., DELETE_..., EDIT_...).
     */
    private static Flow flowOf(State state) {
        String name = state.name();
        if (name.startsWith("SHOW")) {
            return Flow.SHOW;
        }
        if (name.startsWith("DELETE")) {
            return Flow.DELETE;
        }
        return name.startsWith("EDIT") ? Flow.EDIT : null;
    }

    private static String extractCommand(String text) {
        String first = text.trim().split("\\s+")[0];
        int at = first.indexOf('@');
        return at > 0 ? first.substring(0, at) : first;
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}