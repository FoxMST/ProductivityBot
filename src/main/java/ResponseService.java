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

    /**
     * Приветственное сообщение.
     */
    private static final String START_MESSAGE = "👋 Привет! Я бот-продуктивность.\n" +
            "Напиши /help, чтобы увидеть команды.";

    /**
     * Текст справки со списком всех команд.
     */
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

    /**
     * Сообщение при отмене текущего действия.
     */
    private static final String CANCEL_MESSAGE = "Черновик задачи удален. Введите любую команду.";

    /**
     * Сообщение, если нажата устаревшая кнопка.
     */
    private static final String STALE_MESSAGE = "Это действие уже недоступно. Введите любую команду.";

    /**
     * Сообщение об ошибке формата даты.
     */
    private static final String BAD_DATE_MESSAGE = "❌ Неправильный формат. Введите ДД.ММ.ГГГГ";

    /**
     * Сообщение об ошибке формата времени.
     */
    private static final String BAD_TIME_MESSAGE = "❌ Неправильный формат. Введите ЧЧ:ММ";

    /**
     * Сообщение, если задача не найдена.
     */
    private static final String NOT_FOUND_MESSAGE = "❌ Задача не найдена.";

    /**
     * Контекст выполняемой команды.
     */
    private enum Flow {
        /**
         * Просмотр списка задач.
         */
        SHOW,
        /**
         * Удаление задачи.
         */
        DELETE,
        /**
         * Редактирование задачи.
         */
        EDIT,
        /**
         * Пометка задач выполненными.
         */
        MARK_DONE
    }

    /**
     * Хранилище задач.
     */
    private final TaskStorage storage;
    /**
     * Часы для получения текущего времени (позволяет тестировать).
     */
    private final Clock clock;
    /**
     * Форматтер для даты.
     */
    private final DateTimeFormatter dateFormat;
    /**
     * Форматтер для времени.
     */
    private final DateTimeFormatter timeFormat;
    /**
     * Множество доступных текстовых команд.
     */
    private final Set<String> commands;
    /**
     * Генератор кнопок.
     */
    private final Button buttons;
    /**
     * Сессии пользователей по их chatId.
     */
    private final Map<Long, UserSession> sessions;

    /**
     * Создает сервис с системными часами и пустым хранилищем.
     */
    public ResponseService() {
        this(Clock.systemDefaultZone(), new TaskStorage());
    }

    /**
     * Создает сервис с заданными часами и хранилищем.
     *
     * @param clock   часы
     * @param storage хранилище задач
     */
    public ResponseService(Clock clock, TaskStorage storage) {
        this.clock = clock;
        this.storage = storage;
        this.dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
        this.timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        this.commands = new HashSet<>(Arrays.asList("/start", "/help", "/add_task", "/show_tasklist", "/delete_task", "/edit_task", "/mark_as_done"));
        this.buttons = new Button();
        this.sessions = new HashMap<>();
    }

    /**
     * Обрабатывает текстовое сообщение от пользователя.
     *
     * @param chatId ID чата пользователя
     * @param text   текст сообщения
     * @return список ответов бота
     */
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

    /**
     * Обрабатывает нажатие inline-кнопки.
     *
     * @param chatId   ID чата пользователя
     * @param actionId идентификатор нажатой кнопки
     * @return список ответов бота
     */
    public synchronized List<BotResponse> handleAction(long chatId, String actionId) {
        UserSession s = sessions.computeIfAbsent(chatId, id -> new UserSession());
        State state = s.getState();
        ButtonAction action = buttons.fromId(actionId);

        if (action == ButtonAction.CANCEL && state != State.IDLE)
            return finish(s, CANCEL_MESSAGE);

        if (state == State.ADD_TASK_ASK_TIME) {
            if (action == ButtonAction.YES)
                return ask(
                        s,
                        State.ADD_TASK_WAIT_TIME,
                        buttons.cancel("⏰ Введите рекомендуемое время начала (ЧЧ:ММ)"));
            if (action == ButtonAction.NO)
                return ask(
                        s,
                        State.ADD_TASK_ASK_DEADLINE,
                        buttons.yesNo("Добавить дедлайн к задаче?"));
        }

        if (state == State.ADD_TASK_ASK_DEADLINE) {
            if (action == ButtonAction.YES)
                return ask(
                        s,
                        State.ADD_TASK_WAIT_DEADLINE,
                        buttons.cancel("⏳ Введите дедлайн: ДД.ММ.ГГГГ ЧЧ:ММ, или только ДД.ММ.ГГГГ, или только ЧЧ:ММ"));
            if (action == ButtonAction.NO)
                return createTask(s);
        }

        if (state == State.EDIT_TASK_WAIT_FIELD && action != null) {
            if (action == ButtonAction.EDIT_TEXT)
                return ask(
                        s,
                        State.EDIT_TASK_WAIT_NEW_TEXT,
                        buttons.cancel("Введите новый текст"));
            if (action == ButtonAction.EDIT_DATETIME)
                return ask(
                        s,
                        State.EDIT_TASK_WAIT_NEW_DATETIME,
                        buttons.cancel("Введите новую дату и/или время (ДД.ММ.ГГГГ ЧЧ:ММ)"));
            if (action == ButtonAction.EDIT_DEADLINE)
                return ask(
                        s,
                        State.EDIT_TASK_WAIT_NEW_DEADLINE,
                        buttons.cancel("Введите новый дедлайн (ДД.ММ.ГГГГ ЧЧ:ММ) или только его часть"));
        }

        if (action == ButtonAction.MARK_DONE && state == State.MARK_DONE_WAIT_BUTTON) {
            return ask(
                    s,
                    State.MARK_DONE_WAIT_NUMBERS,
                    buttons.cancel("Выберите номер/номера задач в списке, которые необходимо пометить выполненными."+
                            " Если задач несколько введите номера через пробел."));
        }

        if (state == State.DELETE_TASK_CONFIRM && action == ButtonAction.DELETE_CONFIRM)
            return deleteTask(s, s.getPendingTaskId());

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

    /**
     * Запускает выполнение команды.
     *
     * @param command команда
     * @param s       сессия пользователя
     * @return ответы бота
     */
    private List<BotResponse> runCommand(String command, UserSession s) {
        reset(s);
        switch (command) {
            case "/start":
                return reply(new BotResponse(START_MESSAGE));
            case "/help":
                return reply(new BotResponse(HELP_MESSAGE));
            case "/add_task":
                return ask(
                        s,
                        State.ADD_TASK_WAIT_TEXT,
                        buttons.cancel("📝 Введите текст задачи"));
            case "/show_tasklist":
                return ask(
                        s,
                        State.SHOW_TASKLIST_WAIT_DATE,
                        buttons.dayChoice("За какой день показать задачи?"));
            case "/delete_task":
                return ask(
                        s,
                        State.DELETE_TASK_WAIT_ID_OR_DATE,
                        buttons.dayChoice("Отправьте ID задачи или выберите день для удаления:"));
            case "/mark_as_done":
                return showDay(s, LocalDate.now(clock), Flow.MARK_DONE);
            default:
                return ask(
                        s,
                        State.EDIT_TASK_WAIT_ID_OR_DATE,
                        buttons.dayChoice("Отправьте ID задачи или выберите день для редактирования:"));
        }
    }

    /**
     * Обрабатывает ввод даты при добавлении задачи.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
    private List<BotResponse> handleAddTaskDate(UserSession s, String text) {
        try {
            s.setSelectedDate(LocalDate.parse(text.trim(), dateFormat));
            return ask(s, State.ADD_TASK_ASK_TIME, buttons.yesNo("Добавить рекомендуемое время начала?"));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_DATE_MESSAGE));
        }
    }

    /**
     * Обрабатывает ввод времени начала.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
    private List<BotResponse> handleAddTaskTime(UserSession s, String text) {
        try {
            s.setPendingTime(LocalTime.parse(text.trim(), timeFormat));
            return ask(s, State.ADD_TASK_ASK_DEADLINE, buttons.yesNo("Добавить дедлайн к задаче?"));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_TIME_MESSAGE));
        }
    }

    /**
     * Обрабатывает ввод дедлайна.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
    private List<BotResponse> handleAddTaskDeadline(UserSession s, String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            LocalDate dDate = null;
            LocalTime dTime = null;
            for (String part : parts) {
                if (part.contains("."))
                    dDate = LocalDate.parse(part, dateFormat);
                else if (part.contains(":"))
                    dTime = LocalTime.parse(part, timeFormat);
            }
            if (dDate == null && dTime != null)
                dDate = s.getSelectedDate();
            if (dDate == null && dTime == null)
                throw new Exception("Empty input");
            s.setPendingDeadlineDate(dDate);
            s.setPendingDeadlineTime(dTime);
            return createTask(s);
        } catch (Exception e) {
            return reply(buttons.cancel("❌ Ошибка." +
                    " Введите ДД.ММ.ГГГГ ЧЧ:ММ, или только ДД.ММ.ГГГГ, или только ЧЧ:ММ"));
        }
    }

    /**
     * Сохраняет задачу из черновика.
     *
     * @param s сессия
     * @return ответы бота
     */
    private List<BotResponse> createTask(UserSession s) {
        storage.addTask(
                s.getPendingText(),
                s.getSelectedDate(),
                s.getPendingTime(),
                s.getPendingDeadlineDate(),
                s.getPendingDeadlineTime());
        return finish(s, "✅ Задача добавлена.");
    }

    /**
     * Обрабатывает редактирование даты/времени.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
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

    /**
     * Обрабатывает редактирование дедлайна.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
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

    /**
     * Помечает задачи выполненными по их номерам в списке.
     *
     * @param s    сессия
     * @param text номера задач через пробел
     * @return ответы бота
     */
    private List<BotResponse> handleMarkDoneNumbers(UserSession s, String text) {
        List<Task> allTasks = storage.getTasksByDate(s.getSelectedDate());
        List<Task> displayTasks = new ArrayList<>();
        for (Task t : allTasks) {
            if (!t.isDone()) displayTasks.add(t);
        }
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

    /**
     * Выводит список задач на указанный день.
     *
     * @param s    сессия
     * @param date дата
     * @param flow контекст (просмотр, удаление и т.д.)
     * @return ответы бота
     */
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

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Задачи на ").append(day).append(". Формат: (Номер) - (Задача) - (ID) - (Время) - (Дедлайн) - (Статус):");
        for (int i = 0; i < displayTasks.size(); i++) {
            Task t = displayTasks.get(i);
            sb.append("\n").append(i + 1).append(". ").append(t.getText()).append(" - ").append(t.getId()).append(" - ").append(dateFormat.format(t.getDate()));
            if (t.getStartTime() != null)
                sb.append(" ").append(t.getStartTime());
            if (t.getDeadlineDate() != null) {
                sb.append(" → дедлайн ").append(dateFormat.format(t.getDeadlineDate()));
                if (t.getDeadlineTime() != null)
                    sb.append(" ").append(t.getDeadlineTime());
            }
            sb.append(" - ").append(t.isDone() ? "✅" : "⭕");
        }
        s.setSelectedDate(date);

        if (flow == Flow.SHOW || flow == Flow.MARK_DONE) {
            s.setState(State.MARK_DONE_WAIT_BUTTON);
            return reply(buttons.markDone(sb.toString()));
        }
        boolean delete = flow == Flow.DELETE;
        s.setState(delete ? State.DELETE_TASK_WAIT_LIST_NUMBER : State.EDIT_TASK_WAIT_LIST_NUMBER);
        return reply(new BotResponse(sb.toString()), buttons.cancel("Напишите номер задачи, которую хотите " +
                (delete ? "удалить." : "изменить.")));
    }

    /**
     * Обрабатывает ручной ввод даты для списка.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
    private List<BotResponse> onDateInput(UserSession s, String text) {
        try {
            return showDay(s, LocalDate.parse(text.trim(), dateFormat), flowOf(s.getState()));
        } catch (DateTimeParseException e) {
            return reply(buttons.cancel(BAD_DATE_MESSAGE));
        }
    }

    /**
     * Обрабатывает ввод номера задачи из списка.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
    private List<BotResponse> onNumberInput(UserSession s, String text) {
        List<Task> tasks = storage.getTasksByDate(s.getSelectedDate());
        Integer number = parseInt(text);
        if (number == null || number < 1 || number > tasks.size()) {
            return reply(buttons.cancel("❌ Введите номер задачи из списка (от 1 до " + tasks.size() + ")."));
        }
        return chooseTask(s, tasks.get(number - 1));
    }

    /**
     * Обрабатывает ввод ID задачи.
     *
     * @param s    сессия
     * @param text введенный текст
     * @return ответы бота
     */
    private List<BotResponse> onIdInput(UserSession s, String text) {
        Integer id = parseInt(text);
        Task task = id == null ? null : storage.getTaskById(id);
        if (task == null) {
            return reply(buttons.dayChoice(id == null ? "❌ Введите ID задачи (число) или выберите день кнопкой." : "❌ Задача с ID " + id + " не найдена."
                    + " Введите другой ID или выберите день:"));
        }
        return chooseTask(s, task);
    }

    /**
     * Переходит к работе с выбранной задачей.
     *
     * @param s    сессия
     * @param task задача
     * @return ответы бота
     */
    private List<BotResponse> chooseTask(UserSession s, Task task) {
        s.setPendingTaskId(task.getId());
        if (flowOf(s.getState()) == Flow.EDIT)
            return ask(s, State.EDIT_TASK_WAIT_FIELD, buttons.editField("Что изменить?"));
        return ask(s, State.DELETE_TASK_CONFIRM, buttons.deleteConfirm("Удалить задачу «" + task.getText() + "» ?"));
    }

    /**
     * Удаляет задачу.
     *
     * @param s  сессия
     * @param id ID задачи
     * @return ответы бота
     */
    private List<BotResponse> deleteTask(UserSession s, int id) {
        try {
            storage.deleteTask(id);
            return finish(s, "🗑 Задача удалена.");
        } catch (IllegalArgumentException e) {
            return finish(s, NOT_FOUND_MESSAGE);
        }
    }

    /**
     * Меняет состояние сессии и возвращает вопрос.
     *
     * @param s        сессия
     * @param next     следующее состояние
     * @param question вопрос
     * @return ответы бота
     */
    private List<BotResponse> ask(UserSession s, State next, BotResponse question) {
        s.setState(next);
        return reply(question);
    }

    /**
     * Завершает сценарий и сбрасывает состояние.
     *
     * @param s    сессия
     * @param text текст ответа
     * @return ответы бота
     */
    private List<BotResponse> finish(UserSession s, String text) {
        reset(s);
        return reply(new BotResponse(text));
    }

    /**
     * Сбрасывает сессию в начальное состояние.
     *
     * @param s сессия
     */
    private void reset(UserSession s) {
        s.setState(State.IDLE);
        s.setSelectedDate(null);
        s.setPendingTaskId(0);
        s.setPendingText(null);
        s.setPendingTime(null);
        s.setPendingDeadlineDate(null);
        s.setPendingDeadlineTime(null);
    }

    /**
     * Оборачивает ответы в список.
     *
     * @param responses ответы
     * @return список ответов
     */
    private List<BotResponse> reply(BotResponse... responses) {
        return Arrays.asList(responses);
    }

    /**
     * Проверяет, ожидается ли выбор дня.
     *
     * @param state состояние
     * @return true, если ожидается выбор дня
     */
    private boolean isDayChoiceState(State state) {
        return state == State.SHOW_TASKLIST_WAIT_DATE || state == State.DELETE_TASK_WAIT_ID_OR_DATE || state == State.EDIT_TASK_WAIT_ID_OR_DATE;
    }

    /**
     * Определяет контекст по состоянию.
     *
     * @param state состояние
     * @return контекст
     */
    private Flow flowOf(State state) {
        String name = state.name();
        if (name.startsWith("SHOW"))
            return Flow.SHOW;
        if (name.startsWith("DELETE"))
            return Flow.DELETE;
        if (name.startsWith("MARK"))
            return Flow.MARK_DONE;
        return name.startsWith("EDIT") ? Flow.EDIT : null;
    }

    /**
     * Извлекает команду из строки (убирает @username).
     *
     * @param text текст
     * @return команда
     */
    private String extractCommand(String text) {
        String first = text.trim().split("\\s+")[0];
        int at = first.indexOf('@');
        return at > 0 ? first.substring(0, at) : first;
    }

    /**
     * Парсит строку в число.
     *
     * @param text текст
     * @return число или null
     */
    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}