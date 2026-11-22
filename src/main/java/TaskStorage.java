import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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

    public int addTask(String text, LocalDate date) {
        return addTask(text, date, null, null, null);
    }

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

    public List<Task> getTasksByDate(LocalDate date) {
        List<Task> tasks = tasksByDate.get(date);
        return tasks == null ? new ArrayList<>() : new ArrayList<>(tasks);
    }

    public List<Task> getTasksWithDeadline(LocalDate from, LocalDate to) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasksById.values()) {
            if (task.getDeadlineDate() == null) continue;
            if (task.getDate().isBefore(from) || task.getDate().isAfter(to)) continue;
            result.add(task);
        }
        result.sort(Comparator
                .comparing(Task::getDeadlineDate)
                .thenComparing(Task::getDeadlineTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Task::getId));
        return result;
    }

    public Task getTaskById(int id) {
        return tasksById.get(id);
    }

    public void deleteTask(int id) {
        Task task = tasksById.remove(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        List<Task> tasks = tasksByDate.get(task.getDate());
        if (tasks != null) {
            tasks.remove(task);
            if (tasks.isEmpty()) tasksByDate.remove(task.getDate());
        }
    }

    public void updateTaskText(int id, String newText) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        task.setText(newText);
    }

    public void updateTaskDateTime(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        if (date != null && !task.getDate().equals(date)) {
            List<Task> oldList = tasksByDate.get(task.getDate());
            if (oldList != null) {
                oldList.remove(task);
                if (oldList.isEmpty()) tasksByDate.remove(task.getDate());
            }
            task.setDate(date);
            tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
        }
        if (time != null) task.setStartTime(time);
    }

    public void updateTaskDeadline(int id, LocalDate date, LocalTime time) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        if (date != null) task.setDeadlineDate(date);
        if (time != null) task.setDeadlineTime(time);
    }

    public void markTaskDone(int id) {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Задача с ID " + id + " не найдена");
        }
        task.setDone(true);
    }

    // ===== TODO: DEBUG-ONLY — удалить после отладки пагинации =====

    /**
     * Заполняет хранилище тестовыми задачами для проверки пагинации.
     * Создает {@code count} задач на указанную дату; у части задач есть
     * дедлайн, у части — время начала, каждая пятая помечена выполненной.
     *
     * @param date  дата, на которую добавить задачи
     * @param count количество задач (рекомендуется 25, чтобы было 3 страницы)
     */
    public void fillTestTasks(LocalDate date, int count) {
        for (int i = 1; i <= count; i++) {
            String text = "Тестовая задача #" + i;
            LocalTime startTime = (i % 2 == 0) ? LocalTime.of(9 + (i % 10), 0) : null;
            LocalDate deadlineDate = (i % 3 == 0) ? date.plusDays(i % 7) : null;
            LocalTime deadlineTime = (deadlineDate != null) ? LocalTime.of(18, 0) : null;
            int id = addTask(text, date, startTime, deadlineDate, deadlineTime);
            if (i % 5 == 0) markTaskDone(id);
        }
    }

    // ===== конец DEBUG-ONLY =====
}