import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

/**
 * Логика бота.
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

    /**
     * Какая команда выполняется в текущем диалоге.
     */
    private enum Flow {
        SHOW, DELETE, EDIT
    }

    private final TaskStorage storage;
    private final Clock clock;
    private final DateTimeFormatter dateFormat;
    private final Set<String> commands;
    private final Button buttons;
    private final Map<Long, UserSession> sessions;

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
        this.dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
                .withResolverStyle(ResolverStyle.STRICT);
        this.commands = new HashSet<>(Arrays.asList(
                "/start", "/help", "/add_task", "/show_tasklist", "/delete_task", "/edit_task"));
        this.buttons = new Button();
        this.sessions = new HashMap<>();
    }

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

        if (state == State.ADD_TASK_WAIT_TEXT) {
            storage.addTask(text, LocalDate.now(clock));
            return finish(s, "✅ Задача добавлена.");
        }
        if (state == State.EDIT_TASK_WAIT_NEW_TEXT) {
            boolean updated = storage.updateTaskText(s.getPendingTaskId(), text);
            return finish(s, updated ? "✅ Задача обновлена." : NOT_FOUND_MESSAGE);
        }

        String command = extractCommand(text);
        if (commands.contains(command)) {
            return runCommand(command, s);
        }

        switch (state) {
            case IDLE:
                return reply(new BotResponse("Вы ввели: «" + text + "»"));
            case SHOW_TASKLIST_WAIT_DATE:
                return reply(buttons.dayChoice("Выберите день с помощью кнопок."));
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
                return reply(buttons.cancel(
                        "Выберите действие кнопками выше или нажмите ❌ Отмена."));
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
        Button.ButtonAction action = buttons.fromId(actionId);

        if (action == Button.ButtonAction.CANCEL && state != State.IDLE) {
            return finish(s, CANCEL_MESSAGE);
        }
        if (state == State.DELETE_TASK_CONFIRM && action == Button.ButtonAction.DELETE_CONFIRM) {
            return deleteTask(s, s.getPendingTaskId());
        }
        if (state == State.EDIT_TASK_WAIT_FIELD && action == Button.ButtonAction.EDIT_TEXT) {
            return ask(s, State.EDIT_TASK_WAIT_NEW_TEXT,
                    buttons.cancel("Введите новый текст для задачи"));
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
                    return ask(s, next, buttons.cancel("Введите дату в формате ДД.ММ.ГГГГ"));
                default:
                    break;
            }
        }
        return reply(new BotResponse(STALE_MESSAGE));
    }

    private List<BotResponse> runCommand(String command, UserSession s) {
        reset(s);
        switch (command) {
            case "/start":
                return reply(new BotResponse(START_MESSAGE));
            case "/help":
                return reply(new BotResponse(HELP_MESSAGE));
            case "/add_task":
                return ask(s, State.ADD_TASK_WAIT_TEXT,
                        buttons.cancel("📝 Введите текст задачи"));
            case "/show_tasklist":
                return ask(s, State.SHOW_TASKLIST_WAIT_DATE,
                        buttons.dayChoice("За какой день показать задачи?"));
            case "/delete_task":
                return ask(s, State.DELETE_TASK_WAIT_ID_OR_DATE, buttons.dayChoice(
                        "Отправьте ID задачи или выберите за какой день удалить задачу:"));
            default:
                return ask(s, State.EDIT_TASK_WAIT_ID_OR_DATE, buttons.dayChoice(
                        "Отправьте ID задачи или выберите за какой день редактировать:"));
        }
    }

    private List<BotResponse> showDay(UserSession s, LocalDate date, Flow flow) {
        List<Task> tasks = storage.getTasksByDate(date);
        String day = dateFormat.format(date);
        if (tasks.isEmpty()) {
            return finish(s, "📋 На " + day + " задач нет.");
        }

        StringBuilder sb = new StringBuilder("📋 Задачи на " + day
                + ". Формат вывода (Номер задачи в списке) - (задача) - (ID):");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append("\n").append(i + 1).append(". ")
                    .append(tasks.get(i).getText()).append(" - ").append(tasks.get(i).getId());
        }
        BotResponse list = new BotResponse(sb.toString());

        if (flow == Flow.SHOW) {
            reset(s);
            return reply(list);
        }
        boolean delete = flow == Flow.DELETE;
        s.setSelectedDate(date);
        s.setState(delete ? State.DELETE_TASK_WAIT_LIST_NUMBER : State.EDIT_TASK_WAIT_LIST_NUMBER);
        return reply(list, buttons.cancel("Напишите номер задачи в списке, которую хотите "
                + (delete ? "удалить." : "изменить.")));
    }

    private List<BotResponse> onDateInput(UserSession s, String text) {
        try {
            return showDay(s, LocalDate.parse(text.trim(), dateFormat), flowOf(s.getState()));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_DATE_MESSAGE));
        }
    }

    private List<BotResponse> onNumberInput(UserSession s, String text) {
        List<Task> tasks = storage.getTasksByDate(s.getSelectedDate());
        Integer number = parseInt(text);
        if (number == null || number < 1 || number > tasks.size()) {
            return reply(buttons.cancel("❌ Введите номер задачи из списка (от 1 до "
                    + tasks.size() + ")."));
        }
        return chooseTask(s, tasks.get(number - 1), true);
    }

    private List<BotResponse> onIdInput(UserSession s, String text) {
        Integer id = parseInt(text);
        Task task = id == null ? null : storage.getTaskById(id);
        if (task == null) {
            return reply(buttons.dayChoice(id == null
                    ? "❌ Введите ID задачи (число) или выберите день кнопкой."
                    : "❌ Задача с ID " + id + " не найдена. Введите другой ID или выберите день:"));
        }
        return chooseTask(s, task, false);
    }

    private List<BotResponse> chooseTask(UserSession s, Task task, boolean byListNumber) {
        s.setPendingTaskId(task.getId());
        if (flowOf(s.getState()) == Flow.EDIT) {
            return ask(s, State.EDIT_TASK_WAIT_FIELD, buttons.editField("Что изменить?"));
        }
        if (byListNumber) {
            return deleteTask(s, task.getId());
        }
        return ask(s, State.DELETE_TASK_CONFIRM,
                buttons.deleteConfirm("Удалить задачу «" + task.getText() + "» ?"));
    }

    private List<BotResponse> deleteTask(UserSession s, int id) {
        return finish(s, storage.deleteTask(id) ? "🗑 Задача удалена." : NOT_FOUND_MESSAGE);
    }

    private List<BotResponse> ask(UserSession s, State next, BotResponse question) {
        s.setState(next);
        return reply(question);
    }

    private List<BotResponse> finish(UserSession s, String text) {
        reset(s);
        return reply(new BotResponse(text));
    }

    private void reset(UserSession s) {
        s.setState(State.IDLE);
        s.setSelectedDate(null);
        s.setPendingTaskId(0);
    }

    private List<BotResponse> reply(BotResponse... responses) {
        return Arrays.asList(responses);
    }

    private boolean isDayChoiceState(State state) {
        return state == State.SHOW_TASKLIST_WAIT_DATE
                || state == State.DELETE_TASK_WAIT_ID_OR_DATE
                || state == State.EDIT_TASK_WAIT_ID_OR_DATE;
    }

    private Flow flowOf(State state) {
        String name = state.name();
        if (name.startsWith("SHOW")) {
            return Flow.SHOW;
        }
        if (name.startsWith("DELETE")) {
            return Flow.DELETE;
        }
        return name.startsWith("EDIT") ? Flow.EDIT : null;
    }

    private String extractCommand(String text) {
        String first = text.trim().split("\\s+")[0];
        int at = first.indexOf('@');
        return at > 0 ? first.substring(0, at) : first;
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}