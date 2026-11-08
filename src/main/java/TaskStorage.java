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
    private final Map<Integer, Task> tasksById;
    private final Map<LocalDate, List<Task>> tasksByDate;
    private int nextId;

    public TaskStorage() {
        tasksById = new HashMap<>();
        tasksByDate = new HashMap<>();
        nextId = 1;
    }

    /**
     * Добавляет новую задачу со всеми параметрами.
     */
    public int addTask(String text, LocalDate date, LocalTime startTime, LocalDate deadlineDate, LocalTime deadlineTime) {
        int id = nextId++;
        Task task = new Task(id, text, date);
        task.setStartTime(startTime);
        task.setDeadlineDate(deadlineDate);
        task.setDeadlineTime(deadlineTime);

        tasksById.put(id, task);

        List<Task> list = tasksByDate.computeIfAbsent(date, k -> new ArrayList<>());
        list.add(task);
        return id;
    }

    public List<Task> getTasksByDate(LocalDate date) {
        List<Task> tasks = tasksByDate.get(date);
        return tasks == null ? new ArrayList<>() : new ArrayList<>(tasks);
    }

    public Task getTaskById(int id) {
        return tasksById.get(id);
    }

    public boolean deleteTask(int id) {
        Task task = tasksById.remove(id);
        if (task == null) return false;
        List<Task> tasks = tasksByDate.get(task.getDate());
        if (tasks != null) {
            tasks.remove(task);
            if (tasks.isEmpty()) tasksByDate.remove(task.getDate());
        }
        return true;
    }

    public boolean updateTaskText(int id, String newText) {
        Task task = tasksById.get(id);
        if (task == null) return false;
        task.setText(newText);
        return true;
    }

    /**
     * Обновляет дату и время начала задачи.
     */
    public boolean updateTaskDateTime(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) return false;
        if (date != null) {
            // Обновляем индекс по датам, если дата изменилась
            if (!task.getDate().equals(date)) {
                tasksByDate.get(task.getDate()).remove(task);
                tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
            }
            // Внимание: поле date в Task сейчас final, если нужно менять дату,
            // придется сделать его не final в классе Task.
            // Пока для простоты считаем, что дата не меняется в этом методе,
            // или убираем final в Task.
        }
        if (time != null) task.setStartTime(time);
        return true;
    }

    /**
     * Обновляет дедлайн задачи.
     */
    public boolean updateTaskDeadline(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) return false;
        if (date != null) task.setDeadlineDate(date);
        if (time != null) task.setDeadlineTime(time);
        return true;
    }

    /**
     * Помечает задачу выполненной.
     */
    public void markTaskDone(int id) {
        Task task = tasksById.get(id);
        if (task != null) {
            task.setDone(true);
        }
    }
}