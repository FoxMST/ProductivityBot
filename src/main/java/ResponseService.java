import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

/**
 * Формирует ответы на команды и произвольные сообщения.
 * Реализует логику конечного автомата (FSM) для ведения диалогов.
 */
public class ResponseService {

    private static final String START_MESSAGE = "👋 Привет! Я бот-продуктивность.\n" +
            "Напиши /help, чтобы увидеть команды.";

    private static final String HELP_MESSAGE = """
            📖 Доступные команды:
            /start — начать
            /help — справка
            /add_task — добавить задачу
            /show_tasklist — показать задачи за день
            /edit_task — изменить задачу
            /delete_task — удалить задачу
            /mark_as_done — отметить выполненные
            Во время любой команды можно нажать ❌ Отмена.
            Просто напиши что-нибудь — я повторю.""";

    private static final String CANCEL_MESSAGE = "Черновик задачи удален. Введите любую команду.";
    private static final String STALE_MESSAGE = "Это действие уже недоступно. Введите любую команду.";
    private static final String BAD_DATE_MESSAGE = "❌ Неправильный формат. Введите ДД.ММ.ГГГГ";
    private static final String BAD_TIME_MESSAGE = "❌ Неправильный формат. Введите ЧЧ:ММ";
    private static final String NOT_FOUND_MESSAGE = "❌ Задача не найдена.";

    private static final String ASK_INTERVAL_MESSAGE =
            "Введите интервал в формате ДД.ММ.ГГГГ-ДД.ММ.ГГГГ, чтобы показать "
                    + "задачи с дедлайном за этот период, или выберите диапазон кнопкой:";

    private static final String BAD_INTERVAL_MESSAGE =
            "❌ Неправильный формат. Введите ДД.ММ.ГГГГ-ДД.ММ.ГГГГ "
                    + "(начало не позже конца) или выберите диапазон кнопкой:";

    private static final String ASK_DEADLINE_DONE_MESSAGE =
            "Выберите номер/номера задач в списке, которые необходимо "
                    + "пометить выполненными. Если задач несколько введите "
                    + "номера через пробел.";

    private static final int PAGE_SIZE = 10;

    private enum Flow {SHOW, DELETE, EDIT, MARK_DONE}

    private final TaskStorage storage;
    private final Clock clock;
    private final DateTimeFormatter dateFormat;
    private final DateTimeFormatter timeFormat;
    private final Set<String> commands;
    private final Button buttons;
    private final Map<Long, UserSession> sessions;

    public ResponseService() {
        this(Clock.systemDefaultZone(), new TaskStorage());
    }

    public ResponseService(Clock clock, TaskStorage storage) {
        this.clock = clock;
        this.storage = storage;
        this.dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
        this.timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        this.commands = new HashSet<>(Arrays.asList(
                "/start", "/help", "/add_task", "/show_tasklist",
                "/delete_task", "/edit_task", "/mark_as_done",
                "/debug_fill" // TODO: DEBUG-ONLY — удалить после отладки пагинации
        ));
        this.buttons = new Button();
        this.sessions = new HashMap<>();
    }

    public synchronized List<BotResponse> handleMessage(long chatId, String text) {
        UserSession s = sessions.computeIfAbsent(chatId, id -> new UserSession());
        State state = s.getState();
        String command = extractCommand(text);

        if (commands.contains(command))
            return runCommand(command, s);

        switch (state) {
            case ADD_TASK_WAIT_TEXT:
                s.setPendingText(text);
                return ask(s, State.ADD_TASK_WAIT_DATE, buttons.cancel("📅 Введите дату задачи (ДД.ММ.ГГГГ)"));
            case ADD_TASK_WAIT_DATE:
                return handleAddTaskDate(s, text);
            case ADD_TASK_WAIT_TIME:
                return handleAddTaskTime(s, text);
            case ADD_TASK_WAIT_DEADLINE:
                return handleAddTaskDeadline(s, text);
            case EDIT_TASK_WAIT_NEW_TEXT:
                try {
                    storage.updateTaskText(s.getPendingTaskId(), text);
                    return finish(s, "✅ Задача обновлена.");
                } catch (IllegalArgumentException e) {
                    return finish(s, NOT_FOUND_MESSAGE);
                }
            case EDIT_TASK_WAIT_NEW_DATETIME:
                return handleEditDateTime(s, text);
            case EDIT_TASK_WAIT_NEW_DEADLINE:
                return handleEditDeadline(s, text);
            case MARK_DONE_WAIT_NUMBERS:
                return handleMarkDoneNumbers(s, text);
            case SHOW_DEADLINE_WAIT_INTERVAL:
                return handleDeadlineInterval(s, text);
            case DEADLINE_MARK_WAIT_NUMBERS:
                return handleDeadlineMarkDone(s, text);
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
                return reply(buttons.cancel("Выберите действие кнопками или нажмите ❌ Отмена."));
        }
    }

    public synchronized List<BotResponse> handleAction(long chatId, String actionId) {
        UserSession s = sessions.computeIfAbsent(chatId, id -> new UserSession());
        State state = s.getState();
        ButtonAction action = buttons.fromId(actionId);

        if (action == ButtonAction.CANCEL && state != State.IDLE)
            return finish(s, CANCEL_MESSAGE);

        if ((state == State.MARK_DONE_WAIT_BUTTON
                || state == State.DEADLINE_MARK_WAIT_BUTTON)
                && (action == ButtonAction.PREV_PAGE
                || action == ButtonAction.NEXT_PAGE
                || action == ButtonAction.PAGE_INFO)) {
            return changePage(s, action);
        }

        if (state == State.ADD_TASK_ASK_TIME) {
            if (action == ButtonAction.YES)
                return ask(s, State.ADD_TASK_WAIT_TIME,
                        buttons.cancel("⏰ Введите рекомендуемое время начала (ЧЧ:ММ)"));
            if (action == ButtonAction.NO)
                return ask(s, State.ADD_TASK_ASK_DEADLINE, buttons.yesNo("Добавить дедлайн к задаче?"));
        }

        if (state == State.ADD_TASK_ASK_DEADLINE) {
            if (action == ButtonAction.YES)
                return ask(s, State.ADD_TASK_WAIT_DEADLINE,
                        buttons.cancel("⏳ Введите дедлайн: ДД.ММ.ГГГГ ЧЧ:ММ, или только ДД.ММ.ГГГГ, или только ЧЧ:ММ"));
            if (action == ButtonAction.NO)
                return createTask(s);
        }

        if (state == State.EDIT_TASK_WAIT_FIELD && action != null) {
            if (action == ButtonAction.EDIT_TEXT)
                return ask(s, State.EDIT_TASK_WAIT_NEW_TEXT, buttons.cancel("Введите новый текст"));
            if (action == ButtonAction.EDIT_DATETIME)
                return ask(s, State.EDIT_TASK_WAIT_NEW_DATETIME,
                        buttons.cancel("Введите новую дату и/или время (ДД.ММ.ГГГГ ЧЧ:ММ)"));
            if (action == ButtonAction.EDIT_DEADLINE)
                return ask(s, State.EDIT_TASK_WAIT_NEW_DEADLINE,
                        buttons.cancel("Введите новый дедлайн (ДД.ММ.ГГГГ ЧЧ:ММ) или только его часть"));
        }

        if (action == ButtonAction.MARK_DONE && state == State.MARK_DONE_WAIT_BUTTON) {
            return ask(s, State.MARK_DONE_WAIT_NUMBERS,
                    buttons.cancel("Выберите номер/номера задач в списке, которые необходимо пометить выполненными." +
                            " Если задач несколько введите номера через пробел."));
        }

        if (state == State.DELETE_TASK_CONFIRM && action == ButtonAction.DELETE_CONFIRM)
            return deleteTask(s, s.getPendingTaskId());

        if (state == State.SHOW_TASKLIST_WAIT_DATE && action == ButtonAction.ONLY_DEADLINE)
            return ask(s, State.SHOW_DEADLINE_WAIT_INTERVAL, buttons.intervalChoice(ASK_INTERVAL_MESSAGE));

        if (state == State.SHOW_DEADLINE_WAIT_INTERVAL) {
            LocalDate today = LocalDate.now(clock);
            if (action == ButtonAction.WEEK)
                return showDeadlineTasks(s, today, today.plusDays(6));
            if (action == ButtonAction.TWO_WEEKS)
                return showDeadlineTasks(s, today, today.plusDays(13));
            if (action == ButtonAction.MONTH)
                return showDeadlineTasks(s, today, today.plusMonths(1));
        }

        if (state == State.DEADLINE_MARK_WAIT_BUTTON && action == ButtonAction.MARK_DONE)
            return ask(s, State.DEADLINE_MARK_WAIT_NUMBERS, buttons.cancel(ASK_DEADLINE_DONE_MESSAGE));

        if (isDayChoiceState(state) && action != null) {
            Flow flow = flowOf(state);
            switch (action) {
                case TODAY:
                    return showDay(s, LocalDate.now(clock), flow);
                case TOMORROW:
                    return showDay(s, LocalDate.now(clock).plusDays(1), flow);
                case CUSTOM_DATE:
                    State next = flow == Flow.SHOW
                            ? State.SHOW_TASKLIST_WAIT_CUSTOM_DATE : flow == Flow.DELETE
                            ? State.DELETE_TASK_WAIT_CUSTOM_DATE : State.EDIT_TASK_WAIT_CUSTOM_DATE;
                    return ask(s, next, buttons.cancel("Введите дату (ДД.ММ.ГГГГ)"));
                default:
                    break;
            }
        }
        return reply(new BotResponse(STALE_MESSAGE));
    }

    private List<BotResponse> changePage(UserSession s, ButtonAction action) {
        List<Task> tasks = s.getPageTasks();
        if (tasks.isEmpty()) return reply(new BotResponse(STALE_MESSAGE));
        int total = totalPages(tasks.size());
        int page = s.getPage();
        if (action == ButtonAction.PREV_PAGE) page--;
        else if (action == ButtonAction.NEXT_PAGE) page++;
        s.setPage(clampPage(page, total));
        return renderTaskPage(s);
    }

    private List<BotResponse> renderTaskPage(UserSession s) {
        List<Task> tasks = s.getPageTasks();
        int total = totalPages(tasks.size());
        int page = clampPage(s.getPage(), total);
        s.setPage(page);

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, tasks.size());

        StringBuilder sb = new StringBuilder(s.getPageTitle());
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');
        for (int i = from; i < to; i++) {
            sb.append(formatTaskLine(i + 1, tasks.get(i)));
            if (i < to - 1) sb.append('\n');
        }
        return reply(buttons.markDone(sb.toString(), page, total));
    }

    private String formatTaskLine(int number, Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append(number).append(". ")
                .append(task.getText()).append(" - ")
                .append(task.getId()).append(" - ")
                .append(dateFormat.format(task.getDate()));
        if (task.getStartTime() != null) sb.append(" ").append(task.getStartTime());
        if (task.getDeadlineDate() != null) {
            sb.append(" → дедлайн ").append(dateFormat.format(task.getDeadlineDate()));
            if (task.getDeadlineTime() != null) sb.append(" ").append(task.getDeadlineTime());
        }
        sb.append(" - ").append(task.isDone() ? "✅" : "⭕");
        return sb.toString();
    }

    private int totalPages(int size) {
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int clampPage(int page, int total) {
        if (page < 0) return 0;
        if (page >= total) return total - 1;
        return page;
    }

    private List<BotResponse> runCommand(String command, UserSession s) {
        reset(s);
        switch (command) {
            case "/start":
                return reply(new BotResponse(START_MESSAGE));
            case "/help":
                return reply(new BotResponse(HELP_MESSAGE));
            case "/add_task":
                return ask(s, State.ADD_TASK_WAIT_TEXT, buttons.cancel("📝 Введите текст задачи"));
            case "/show_tasklist":
                return ask(s, State.SHOW_TASKLIST_WAIT_DATE,
                        buttons.showDayChoice("За какой день показать задачи?"));
            case "/delete_task":
                return ask(s, State.DELETE_TASK_WAIT_ID_OR_DATE,
                        buttons.dayChoice("Отправьте ID задачи или выберите день для удаления:"));
            case "/mark_as_done":
                return showDay(s, LocalDate.now(clock), Flow.MARK_DONE);

            // ===== TODO: DEBUG-ONLY — удалить после отладки пагинации =====
            case "/debug_fill":
                storage.fillTestTasks(LocalDate.now(clock), 25);
                return reply(new BotResponse("Добавлено 25 тестовых задач на сегодня."));
            // ===== конец DEBUG-ONLY =====

            default:
                return ask(s, State.EDIT_TASK_WAIT_ID_OR_DATE,
                        buttons.dayChoice("Отправьте ID задачи или выберите день для редактирования:"));
        }
    }

    private List<BotResponse> handleAddTaskDate(UserSession s, String text) {
        try {
            s.setSelectedDate(LocalDate.parse(text.trim(), dateFormat));
            return ask(s, State.ADD_TASK_ASK_TIME, buttons.yesNo("Добавить рекомендуемое время начала?"));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_DATE_MESSAGE));
        }
    }

    private List<BotResponse> handleAddTaskTime(UserSession s, String text) {
        try {
            s.setPendingTime(LocalTime.parse(text.trim(), timeFormat));
            return ask(s, State.ADD_TASK_ASK_DEADLINE, buttons.yesNo("Добавить дедлайн к задаче?"));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_TIME_MESSAGE));
        }
    }

    private List<BotResponse> handleAddTaskDeadline(UserSession s, String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            LocalDate dDate = null;
            LocalTime dTime = null;
            for (String part : parts) {
                if (part.contains(".")) dDate = LocalDate.parse(part, dateFormat);
                else if (part.contains(":")) dTime = LocalTime.parse(part, timeFormat);
            }
            if (dDate == null && dTime != null) dDate = s.getSelectedDate();
            if (dDate == null && dTime == null) throw new Exception("Empty input");
            s.setPendingDeadlineDate(dDate);
            s.setPendingDeadlineTime(dTime);
            return createTask(s);
        } catch (Exception e) {
            return reply(buttons.cancel("❌ Ошибка." +
                    " Введите ДД.ММ.ГГГГ ЧЧ:ММ, или только ДД.ММ.ГГГГ, или только ЧЧ:ММ"));
        }
    }

    private List<BotResponse> createTask(UserSession s) {
        storage.addTask(
                s.getPendingText(),
                s.getSelectedDate(),
                s.getPendingTime(),
                s.getPendingDeadlineDate(),
                s.getPendingDeadlineTime());
        return finish(s, "✅ Задача добавлена.");
    }

    private List<BotResponse> handleEditDateTime(UserSession s, String text) {
        try {
            Task task = storage.getTaskById(s.getPendingTaskId());
            LocalDate date = task.getDate();
            LocalTime time = task.getStartTime();
            boolean changed = false;
            String[] parts = text.trim().split("\\s+");
            for (String part : parts) {
                if (part.contains(".")) {
                    date = LocalDate.parse(part, dateFormat);
                    changed = true;
                }
                if (part.contains(":")) {
                    time = LocalTime.parse(part, timeFormat);
                    changed = true;
                }
            }
            if (!changed) throw new Exception("Empty input");
            storage.updateTaskDateTime(s.getPendingTaskId(), date, time);
            return finish(s, "✅ Задача обновлена.");
        } catch (IllegalArgumentException e) {
            return finish(s, NOT_FOUND_MESSAGE);
        } catch (Exception e) {
            return reply(buttons.cancel("❌ Ошибка. Введите дату и/или время (ДД.ММ.ГГГГ ЧЧ:ММ)"));
        }
    }

    private List<BotResponse> handleEditDeadline(UserSession s, String text) {
        try {
            Task task = storage.getTaskById(s.getPendingTaskId());
            LocalDate date = task.getDeadlineDate();
            LocalTime time = task.getDeadlineTime();
            boolean changed = false;
            String[] parts = text.trim().split("\\s+");
            for (String part : parts) {
                if (part.contains(".")) {
                    date = LocalDate.parse(part, dateFormat);
                    changed = true;
                }
                if (part.contains(":")) {
                    time = LocalTime.parse(part, timeFormat);
                    changed = true;
                }
            }
            if (!changed) throw new Exception("Empty input");
            storage.updateTaskDeadline(s.getPendingTaskId(), date, time);
            return finish(s, "✅ Задача обновлена.");
        } catch (IllegalArgumentException e) {
            return finish(s, NOT_FOUND_MESSAGE);
        } catch (Exception e) {
            return reply(buttons.cancel("❌ Ошибка. Введите дату и/или время (ДД.ММ.ГГГГ ЧЧ:ММ)"));
        }
    }

    private List<BotResponse> handleMarkDoneNumbers(UserSession s, String text) {
        List<Task> displayTasks = s.getPageTasks();
        if (displayTasks.isEmpty())
            return reply(buttons.cancel("❌ Список задач устарел. Откройте список заново."));
        String[] parts = text.trim().split("\\s+");
        int count = 0;
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num >= 1 && num <= displayTasks.size()) {
                    try {
                        storage.markTaskDone(displayTasks.get(num - 1).getId());
                        count++;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (count == 0) return reply(buttons.cancel("❌ Неверные номера. Попробуйте еще раз:"));
        return finish(s, "✅ Задачи помечены как выполненные (" + count + " шт.)");
    }

    private List<BotResponse> handleDeadlineInterval(UserSession s, String text) {
        String[] parts = text.trim().split("-");
        if (parts.length != 2)
            return reply(buttons.intervalChoice(BAD_INTERVAL_MESSAGE));
        try {
            LocalDate from = LocalDate.parse(parts[0].trim(), dateFormat);
            LocalDate to = LocalDate.parse(parts[1].trim(), dateFormat);
            if (to.isBefore(from))
                return reply(buttons.intervalChoice(BAD_INTERVAL_MESSAGE));
            return showDeadlineTasks(s, from, to);
        } catch (DateTimeParseException e) {
            return reply(buttons.intervalChoice(BAD_INTERVAL_MESSAGE));
        }
    }

    private List<BotResponse> showDeadlineTasks(UserSession s, LocalDate from, LocalDate to) {
        List<Task> tasks = storage.getTasksWithDeadline(from, to);
        String interval = dateFormat.format(from) + "-" + dateFormat.format(to);
        if (tasks.isEmpty())
            return finish(s, "📋 На интервале " + interval + " задач с дедлайном нет.");
        s.setDeadlineTasks(tasks);
        s.setPageTasks(tasks);
        s.setPageTitle("📋 Задачи с дедлайном на интервале " + interval
                + " в порядке срочности.\n"
                + "Формат: (Номер) - (Задача) - (ID) - (Дата, время) - "
                + "(Дедлайн) - (Статус):\n");
        s.setPage(0);
        s.setState(State.DEADLINE_MARK_WAIT_BUTTON);
        return renderTaskPage(s);
    }

    private List<BotResponse> handleDeadlineMarkDone(UserSession s, String text) {
        List<Task> shown = s.getDeadlineTasks();
        Set<Integer> numbers = new TreeSet<>();
        for (String part : text.trim().split("\\s+")) {
            Integer number = parseInt(part);
            if (number != null && number >= 1 && number <= shown.size())
                numbers.add(number);
        }
        if (numbers.isEmpty())
            return reply(buttons.cancel("❌ Неверные номера. Попробуйте еще раз:"));
        try {
            for (int number : numbers)
                storage.markTaskDone(shown.get(number - 1).getId());
        } catch (IllegalArgumentException e) {
            return finish(s, NOT_FOUND_MESSAGE);
        }
        return finish(s, "✅ Задачи помечены как выполненные (" + numbers.size() + " шт.)");
    }

    private List<BotResponse> showDay(UserSession s, LocalDate date, Flow flow) {
        List<Task> tasks = storage.getTasksByDate(date);
        String day = dateFormat.format(date);
        if (tasks.isEmpty()) {
            if (flow == Flow.MARK_DONE) return finish(s, "📋 На " + day + " нет невыполненных задач.");
            return finish(s, "📋 На " + day + " задач нет.");
        }
        List<Task> displayTasks = new ArrayList<>();
        for (Task t : tasks) {
            if (flow == Flow.MARK_DONE && t.isDone()) continue;
            displayTasks.add(t);
        }
        if (displayTasks.isEmpty() && flow == Flow.MARK_DONE)
            return finish(s, "📋 На " + day + " нет невыполненных задач.");

        s.setSelectedDate(date);

        if (flow == Flow.SHOW || flow == Flow.MARK_DONE) {
            s.setPageTasks(displayTasks);
            s.setPageTitle("📋 Задачи на " + day
                    + ". Формат: (Номер) - (Задача) - (ID) - (Дата, время) - (Дедлайн) - (Статус):\n");
            s.setPage(0);
            s.setState(State.MARK_DONE_WAIT_BUTTON);
            return renderTaskPage(s);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Задачи на ").append(day)
                .append(". Формат: (Номер) - (Задача) - (ID) - (Время) - (Дедлайн) - (Статус):");
        for (int i = 0; i < displayTasks.size(); i++)
            sb.append("\n").append(formatTaskLine(i + 1, displayTasks.get(i)));

        boolean delete = flow == Flow.DELETE;
        s.setState(delete ? State.DELETE_TASK_WAIT_LIST_NUMBER : State.EDIT_TASK_WAIT_LIST_NUMBER);
        return reply(new BotResponse(sb.toString()), buttons.cancel("Напишите номер задачи, которую хотите " +
                (delete ? "удалить." : "изменить.")));
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
        if (number == null || number < 1 || number > tasks.size())
            return reply(buttons.cancel("❌ Введите номер задачи из списка (от 1 до " + tasks.size() + ")."));
        return chooseTask(s, tasks.get(number - 1));
    }

    private List<BotResponse> onIdInput(UserSession s, String text) {
        Integer id = parseInt(text);
        Task task = id == null ? null : storage.getTaskById(id);
        if (task == null) {
            return reply(buttons.dayChoice(id == null
                    ? "❌ Введите ID задачи (число) или выберите день кнопкой."
                    : "❌ Задача с ID " + id + " не найдена. Введите другой ID или выберите день:"));
        }
        return chooseTask(s, task);
    }

    private List<BotResponse> chooseTask(UserSession s, Task task) {
        s.setPendingTaskId(task.getId());
        if (flowOf(s.getState()) == Flow.EDIT)
            return ask(s, State.EDIT_TASK_WAIT_FIELD, buttons.editField("Что изменить?"));
        return ask(s, State.DELETE_TASK_CONFIRM,
                buttons.deleteConfirm("Удалить задачу «" + task.getText() + "» ?"));
    }

    private List<BotResponse> deleteTask(UserSession s, int id) {
        try {
            storage.deleteTask(id);
            return finish(s, "🗑 Задача удалена.");
        } catch (IllegalArgumentException e) {
            return finish(s, NOT_FOUND_MESSAGE);
        }
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
        s.setDeadlineTasks(new ArrayList<>());
        s.setPageTasks(new ArrayList<>());
        s.setPageTitle("");
        s.setPage(0);
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
        if (name.startsWith("SHOW")) return Flow.SHOW;
        if (name.startsWith("DELETE")) return Flow.DELETE;
        if (name.startsWith("MARK")) return Flow.MARK_DONE;
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