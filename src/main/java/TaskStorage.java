import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище задач в оперативной памяти
 */
public class TaskStorage {
    /**
     * Задачи по идентификатору
     */
    private final Map<Integer, Task> tasksById;
    /**
     * Задачи по дате
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
     * Добавляет новую задачу.
     *
     * @param text текст задачи
     * @param date дата задачи
     * @return идентификатор добавленной задачи
     */
    public int addTask(String text, LocalDate date) {
        int id = nextId++;
        Task task = new Task(id, text, date);
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
}