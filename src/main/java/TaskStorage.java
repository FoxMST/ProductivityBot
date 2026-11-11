import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище задач в оперативной памяти.
 */
public class TaskStorage {
    /**
     * Все задачи по идентификатору: ключ — идентификатор задачи (id),
     * значение — сама задача.
     */
    private final Map<Integer, Task> tasksById;
    /**
     * Задачи по датам: ключ — дата задачи, значение — список задач этого дня
     * в порядке добавления. Дни без задач в карте не хранятся.
     */
    private final Map<LocalDate, List<Task>> tasksByDate;
    /**
     * Идентификатор, который получит следующая добавленная задача (начиная с 1).
     */
    private int nextId;

    /**
     * Создает пустое хранилище.
     */
    public TaskStorage() {
        tasksById = new HashMap<>();
        tasksByDate = new HashMap<>();
        nextId = 1;
    }

    /**
     * Добавляет новую задачу (без времени и дедлайна).
     *
     * @param text текст задачи
     * @param date дата задачи
     * @return идентификатор добавленной задачи
     */
    public int addTask(String text, LocalDate date) {

        return addTask(text, date, null, null, null);
    }

    /**
     * Добавляет новую задачу со всеми параметрами.
     *
     * @param text         текст задачи
     * @param date         дата задачи
     * @param startTime    время начала (может быть null)
     * @param deadlineDate дата дедлайна (может быть null)
     * @param deadlineTime время дедлайна (может быть null)
     * @return идентификатор добавленной задачи
     */
    public int addTask(String text, LocalDate date, LocalTime startTime,
                       LocalDate deadlineDate, LocalTime deadlineTime) {
        int id = nextId++;
        Task task = new Task(id, text, date);
        task.setStartTime(startTime);
        task.setDeadlineDate(deadlineDate);
        task.setDeadlineTime(deadlineTime);

        tasksById.put(id, task);
        tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
        return id;
    }

    /**
     * Возвращает список задач на указанную дату.
     *
     * @param date дата
     * @return список задач (может быть пустым)
     */
    public List<Task> getTasksByDate(LocalDate date) {
        List<Task> tasks = tasksByDate.get(date);
        return tasks == null
                ? new ArrayList<>()
                : new ArrayList<>(tasks);
    }

    /**
     * Находит задачу по идентификатору.
     *
     * @param id идентификатор
     * @return задача или null, если не найдена
     */
    public Task getTaskById(int id) {

        return tasksById.get(id);
    }

    /**
     * Удаляет задачу по идентификатору.
     *
     * @param id идентификатор
     * @throws IllegalArgumentException если задача с таким ID не найдена
     */
    public void deleteTask(int id) {
        Task task = tasksById.remove(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        List<Task> tasks = tasksByDate.get(task.getDate());
        if (tasks != null) {
            tasks.remove(task);
            if (tasks.isEmpty()) {
                tasksByDate.remove(task.getDate());
            }
        }
    }

    /**
     * Обновляет текст задачи.
     *
     * @param id      идентификатор
     * @param newText новый текст
     * @throws IllegalArgumentException если задача с таким ID не найдена
     */
    public void updateTaskText(int id, String newText) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        task.setText(newText);
    }

    /**
     * Обновляет дату и время начала задачи.
     *
     * @param id   идентификатор
     * @param date новая дата (может быть null)
     * @param time новое время (может быть null)
     * @throws IllegalArgumentException если задача с таким ID не найдена
     */
    public void updateTaskDateTime(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        if (date != null && !task.getDate().equals(date)) {
            List<Task> oldList = tasksByDate.get(task.getDate());
            if (oldList != null) {
                oldList.remove(task);
                if (oldList.isEmpty()) {
                    tasksByDate.remove(task.getDate());
                }
            }
            task.setDate(date);
            tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
        }
        if (time != null) {
            task.setStartTime(time);
        }
    }

    /**
     * Обновляет дедлайн задачи.
     *
     * @param id   идентификатор
     * @param date новая дата дедлайна (может быть null)
     * @param time новое время дедлайна (может быть null)
     * @throws IllegalArgumentException если задача с таким ID не найдена
     */
    public void updateTaskDeadline(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        if (date != null)
            task.setDeadlineDate(date);
        if (time != null)
            task.setDeadlineTime(time);
    }

    /**
     * Помечает задачу выполненной.
     *
     * @param id идентификатор
     * @throws IllegalArgumentException если задача с таким ID не найдена
     */
    public void markTaskDone(int id) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        task.setDone(true);
    }
}