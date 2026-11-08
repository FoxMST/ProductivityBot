import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

/**
 * Формирует ответы на команды и произвольные сообщения.
 */
public class ResponseService {

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
                    + "/mark_as_done — отметить выполненные\n"
                    + "Во время любой команды можно нажать ❌ Отмена.\n"
                    + "Просто напиши что-нибудь — я повторю.";
    private static final String CANCEL_MESSAGE =
            "Черновик задачи удален. Введите любую команду.";
    private static final String STALE_MESSAGE =
            "Это действие уже недоступно. Введите любую команду.";
    private static final String BAD_DATE_MESSAGE =
            "❌ Неправильный формат. Введите ДД.ММ.ГГГГ";
    private static final String BAD_TIME_MESSAGE =
            "❌ Неправильный формат. Введите ЧЧ:ММ";
    private static final String NOT_FOUND_MESSAGE = "❌ Задача не найдена.";

    /**
     * Какая команда выполняется в текущем диалоге.
     */
    private enum Flow {
        SHOW, DELETE, EDIT, MARK_DONE
    }

    private final TaskStorage storage;
    private final Clock clock;
    private final DateTimeFormatter dateFormat;
    private final DateTimeFormatter timeFormat;
    private final Set<String> commands;
    private final Button buttons;
    private final Map<Long, UserSession> sessions;

    /**
     * Бот с системными часами и пустым хранилищем.
     */
    public ResponseService() {
        this(Clock.systemDefaultZone(), new TaskStorage());
    }

    /**
     * @param clock   часы
     * @param storage хранилище задач
     */
    public ResponseService(Clock clock, TaskStorage storage) {
        this.clock = clock;
        this.storage = storage;
        this.dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
                .withResolverStyle(ResolverStyle.STRICT);
        this.timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        this.commands = new HashSet<>(Arrays.asList(
                "/start", "/help", "/add_task", "/show_tasklist",
                "/delete_task", "/edit_task", "/mark_as_done"));
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
        String command = extractCommand(text);

        if (commands.contains(command)) {
            return runCommand(command, s);
        }

        switch (state) {
            case ADD_TASK_WAIT_TEXT:
                s.setPendingText(text);
                return ask(s, State.ADD_TASK_WAIT_DATE,
                        buttons.cancel("📅 Введите дату задачи (ДД.ММ.ГГГГ)"));
            case ADD_TASK_WAIT_DATE:
                return handleAddTaskDate(s, text);
            case ADD_TASK_WAIT_TIME:
                return handleAddTaskTime(s, text);
            case ADD_TASK_WAIT_DEADLINE:
                return handleAddTaskDeadline(s, text);
            case EDIT_TASK_WAIT_NEW_TEXT:
                storage.updateTaskText(s.getPendingTaskId(), text);
                return finish(s, "✅ Задача обновлена.");
            case EDIT_TASK_WAIT_NEW_DATETIME:
                return handleEditDateTime(s, text);
            case EDIT_TASK_WAIT_NEW_DEADLINE:
                return handleEditDeadline(s, text);
            case MARK_DONE_WAIT_NUMBERS:
                return handleMarkDoneNumbers(s, text);
            case IDLE:
                return reply(new BotResponse("Вы ввели: «" + text + "»"));
            case SHOW_TASKLIST_WAIT_CUSTOM_DATE:
            case DELETE_TASK_WAIT_CUSTOM_DATE:
            case EDIT_TASK_WAIT_CUSTOM_DATE:
                return onDateInput(s, text);
            case DELETE_TASK_WAIT_ID_OR_DATE:
            case EDIT_TASK_WAIT_ID_OR_DATE:
                return onIdInput(s, text);
            case DELETE_TASK_WAIT_LIST_NUMBER:
            case EDIT_TASK_WAIT_LIST_NUMBER:
                return onNumberInput(s, text);
            default:
                return reply(buttons.cancel(
                        "Выберите действие кнопками или нажмите ❌ Отмена."));
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
        ButtonAction action = buttons.fromId(actionId);

        if (action == ButtonAction.CANCEL && state != State.IDLE) {
            return finish(s, CANCEL_MESSAGE);
        }

        if (state == State.ADD_TASK_ASK_TIME) {
            if (action == ButtonAction.YES) {
                return ask(s, State.ADD_TASK_WAIT_TIME,
                        buttons.cancel("⏰ Введите время (ЧЧ:ММ)"));
            }
            if (action == ButtonAction.NO) {
                return ask(s, State.ADD_TASK_ASK_DEADLINE,
                        buttons.yesNo("Добавить дедлайн к задаче?"));
            }
        }

        if (state == State.ADD_TASK_ASK_DEADLINE) {
            if (action == ButtonAction.YES) {
                return ask(s, State.ADD_TASK_WAIT_DEADLINE,
                        buttons.cancel("⏰ Введите дедлайн (ДД.ММ.ГГГГ ЧЧ:ММ)"));
            }
            if (action == ButtonAction.NO) {
                return createTask(s);
            }
        }

        if (state == State.EDIT_TASK_WAIT_FIELD && action != null) {
            if (action == ButtonAction.EDIT_TEXT) {
                return ask(s, State.EDIT_TASK_WAIT_NEW_TEXT,
                        buttons.cancel("Введите новый текст"));
            }
            if (action == ButtonAction.EDIT_DATETIME) {
                return ask(s, State.EDIT_TASK_WAIT_NEW_DATETIME,
                        buttons.cancel("Введите дату и время (ДД.ММ.ГГГГ ЧЧ:ММ)"));
            }
            if (action == ButtonAction.EDIT_DEADLINE) {
                return ask(s, State.EDIT_TASK_WAIT_NEW_DEADLINE,
                        buttons.cancel("Введите новый дедлайн (ДД.ММ.ГГГГ ЧЧ:ММ)"));
            }
        }

        if (action == ButtonAction.MARK_DONE && state == State.MARK_DONE_WAIT_BUTTON) {
            return ask(s, State.MARK_DONE_WAIT_NUMBERS,
                    buttons.cancel("Введите номера задач через пробел (например: 1 2)"));
        }

        if (state == State.DELETE_TASK_CONFIRM && action == ButtonAction.DELETE_CONFIRM) {
            return deleteTask(s, s.getPendingTaskId());
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
                    return ask(s, next, buttons.cancel("Введите дату (ДД.ММ.ГГГГ)"));
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
                        "Отправьте ID задачи или выберите день для удаления:"));
            case "/mark_as_done":
                return showDay(s, LocalDate.now(clock), Flow.MARK_DONE);
            default:
                return ask(s, State.EDIT_TASK_WAIT_ID_OR_DATE, buttons.dayChoice(
                        "Отправьте ID задачи или выберите день для редактирования:"));
        }
    }

    private List<BotResponse> handleAddTaskDate(UserSession s, String text) {
        try {
            s.setSelectedDate(LocalDate.parse(text.trim(), dateFormat));
            return ask(s, State.ADD_TASK_ASK_TIME,
                    buttons.yesNo("Добавить рекомендуемое время начала?"));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_DATE_MESSAGE));
        }
    }

    private List<BotResponse> handleAddTaskTime(UserSession s, String text) {
        try {
            s.setPendingTime(LocalTime.parse(text.trim(), timeFormat));
            return ask(s, State.ADD_TASK_ASK_DEADLINE,
                    buttons.yesNo("Добавить дедлайн к задаче?"));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_TIME_MESSAGE));
        }
    }

    private List<BotResponse> handleAddTaskDeadline(UserSession s, String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            LocalDate dDate = LocalDate.parse(parts[0], dateFormat);
            LocalTime dTime = parts.length > 1 ? LocalTime.parse(parts[1], timeFormat) : null;
            s.setPendingDeadlineDate(dDate);
            s.setPendingDeadlineTime(dTime);
            return createTask(s);
        } catch (Exception e) {
            return reply(buttons.cancel(
                    "❌ Ошибка. Введите ДД.ММ.ГГГГ ЧЧ:ММ или ДД.ММ.ГГГГ"));
        }
    }

    private List<BotResponse> createTask(UserSession s) {
        storage.addTask(s.getPendingText(), s.getSelectedDate(),
                s.getPendingTime(), s.getPendingDeadlineDate(),
                s.getPendingDeadlineTime());
        return finish(s, "✅ Задача добавлена.");
    }

    private List<BotResponse> handleEditDateTime(UserSession s, String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            LocalDate date = LocalDate.parse(parts[0], dateFormat);
            LocalTime time = parts.length > 1
                    ? LocalTime.parse(parts[1], timeFormat) : null;
            storage.updateTaskDateTime(s.getPendingTaskId(), date, time);
            return finish(s, "✅ Задача обновлена.");
        } catch (Exception e) {
            return reply(buttons.cancel(
                    "❌ Ошибка. Введите ДД.ММ.ГГГГ или ДД.ММ.ГГГГ ЧЧ:ММ"));
        }
    }

    private List<BotResponse> handleEditDeadline(UserSession s, String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            LocalDate date = LocalDate.parse(parts[0], dateFormat);
            LocalTime time = parts.length > 1
                    ? LocalTime.parse(parts[1], timeFormat) : null;
            storage.updateTaskDeadline(s.getPendingTaskId(), date, time);
            return finish(s, "✅ Задача обновлена.");
        } catch (Exception e) {
            return reply(buttons.cancel(
                    "❌ Ошибка. Введите ДД.ММ.ГГГГ или ДД.ММ.ГГГГ ЧЧ:ММ"));
        }
    }

    private List<BotResponse> handleMarkDoneNumbers(UserSession s, String text) {
        List<Task> tasks = storage.getTasksByDate(s.getSelectedDate());
        String[] parts = text.trim().split("\\s+");
        int count = 0;
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num >= 1 && num <= tasks.size()) {
                    storage.markTaskDone(tasks.get(num - 1).getId());
                    count++;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (count == 0) {
            return reply(buttons.cancel("❌ Неверные номера. Попробуйте еще раз:"));
        }
        return finish(s, "✅ Задачи помечены как выполненные (" + count + " шт.)");
    }

    private List<BotResponse> showDay(UserSession s, LocalDate date, Flow flow) {
        List<Task> tasks = storage.getTasksByDate(date);
        String day = dateFormat.format(date);

        if (tasks.isEmpty()) {
            return finish(s, "📋 На " + day + " задач нет.");
        }

        List<Task> displayTasks = new java.util.ArrayList<>();
        for (Task t : tasks) {
            if (flow == Flow.MARK_DONE && t.isDone()) {
                continue;
            }
            displayTasks.add(t);
        }

        if (displayTasks.isEmpty() && flow == Flow.MARK_DONE) {
            return finish(s, "📋 На " + day + " нет невыполненных задач.");
        }

        StringBuilder sb = new StringBuilder(
                "📋 Задачи на " + day + ". Формат: (Номер) - (Задача) - (ID) - (Время) - (Дедлайн) - (Статус):");
        for (int i = 0; i < displayTasks.size(); i++) {
            Task t = displayTasks.get(i);
            sb.append("\n").append(i + 1).append(". ").append(t.getText())
                    .append(" - ").append(t.getId())
                    .append(" - ").append(dateFormat.format(t.getDate()))
                    .append(t.getStartTime() != null ? " " + t.getStartTime() : "")
                    .append(t.getDeadlineDate() != null
                            ? " → DL: " + dateFormat.format(t.getDeadlineDate())
                            + (t.getDeadlineTime() != null ? " " + t.getDeadlineTime() : "")
                            : "")
                    .append(" - ").append(t.isDone() ? "✅" : "⭕");
        }

        s.setSelectedDate(date);

        // ИСПРАВЛЕНИЕ ЗДЕСЬ: отдаем один ответ с текстом и кнопками
        if (flow == Flow.SHOW) {
            s.setState(State.MARK_DONE_WAIT_BUTTON);
            return reply(buttons.markDone(sb.toString()));
        }
        if (flow == Flow.MARK_DONE) {
            s.setState(State.MARK_DONE_WAIT_NUMBERS);
            return reply(buttons.cancel("Выберите номера задач через пробел.")); // Тут просто кнопка отмены
        }

        boolean delete = flow == Flow.DELETE;
        s.setState(delete ? State.DELETE_TASK_WAIT_LIST_NUMBER
                : State.EDIT_TASK_WAIT_LIST_NUMBER);
        return reply(new BotResponse(sb.toString()),
                buttons.cancel("Напишите номер задачи, которую хотите "
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
        s.setPendingText(null);
        s.setPendingTime(null);
        s.setPendingDeadlineDate(null);
        s.setPendingDeadlineTime(null);
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
        if (name.startsWith("MARK")) {
            return Flow.MARK_DONE;
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