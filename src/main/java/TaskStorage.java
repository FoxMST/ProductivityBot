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
     * Задачи по идентификатору.
     */
    private final Map<Integer, Task> tasksById;
    /**
     * Задачи по дате.
     */
    private final Map<LocalDate, List<Task>> tasksByDate;
    /**
     * Следующий свободный идентификатор.
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
     * Делегирует в полную версию.
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

        List<Task> list = tasksByDate.get(date);
        if (list == null) {
            list = new ArrayList<>();
            tasksByDate.put(date, list);
        }
        list.add(task);

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
        if (tasks == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tasks);
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
     * @return true, если задача была удалена
     */
    public boolean deleteTask(int id) {
        Task task = tasksById.remove(id);
        if (task == null) {
            return false;
        }
        List<Task> tasks = tasksByDate.get(task.getDate());
        if (tasks != null) {
            tasks.remove(task);
            if (tasks.isEmpty()) {
                tasksByDate.remove(task.getDate());
            }
        }
        return true;
    }

    /**
     * Обновляет текст задачи.
     *
     * @param id      идентификатор
     * @param newText новый текст
     * @return true, если задача найдена и обновлена
     */
    public boolean updateTaskText(int id, String newText) {
        Task task = tasksById.get(id);
        if (task == null) {
            return false;
        }
        task.setText(newText);
        return true;
    }

    /**
     * Обновляет дату и время начала задачи.
     *
     * @param id   идентификатор
     * @param date новая дата (может быть null)
     * @param time новое время (может быть null)
     * @return true, если задача найдена и обновлена
     */
    public boolean updateTaskDateTime(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) {
            return false;
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
        return true;
    }

    /**
     * Обновляет дедлайн задачи.
     *
     * @param id   идентификатор
     * @param date новая дата дедлайна (может быть null)
     * @param time новое время дедлайна (может быть null)
     * @return true, если задача найдена и обновлена
     */
    public boolean updateTaskDeadline(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) {
            return false;
        }
        if (date != null) {
            task.setDeadlineDate(date);
        }
        if (time != null) {
            task.setDeadlineTime(time);
        }
        return true;
    }

    /**
     * Помечает задачу выполненной.
     *
     * @param id идентификатор
     */
    public void markTaskDone(int id) {
        Task task = tasksById.get(id);
        if (task != null) {
            task.setDone(true);
        }
    }
}