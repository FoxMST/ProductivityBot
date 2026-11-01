import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

/**
 * Тесты для TaskStorage.
 * Все тесты используют фиксированные даты (не LocalDate.now()),
 * чтобы результат не зависел от дня запуска.
 */
public class TaskStorageTest {
    private TaskStorage storage;
    private LocalDate day1;
    private LocalDate day2;

    /**
     * Подготовка чистого хранилища перед каждым тестом.
     */
    @BeforeEach
    public void setUp() {
        storage = new TaskStorage();
        day1 = LocalDate.of(2026, 12, 12);
        day2 = LocalDate.of(2026, 12, 13);
    }

    // ---------- addTask ----------

    /**
     * ID идут по возрастанию: 1, 2, 3, ...
     */
    @Test
    public void testAddTaskReturnsIncrementingIds() {
        int id1 = storage.addTask("Задача 1", day1);
        int id2 = storage.addTask("Задача 2", day1);
        int id3 = storage.addTask("Задача 3", day2);
        Assertions.assertEquals(1, id1);
        Assertions.assertEquals(2, id2);
        Assertions.assertEquals(3, id3);
    }

    /**
     * ID глобальный: не сбрасывается при смене дня.
     */
    @Test
    public void testIdsAreGlobalAcrossDates() {
        storage.addTask("На день 1", day1);
        int second = storage.addTask("На день 2", day2);
        Assertions.assertEquals(2, second);
    }

    /**
     * Добавленная задача доступна по ID с сохранённым текстом и датой.
     */
    @Test
    public void testAddTaskStoresTextAndDate() {
        storage.addTask("Купить хлеб", day1);
        Task task = storage.getTaskById(1);
        Assertions.assertNotNull(task);
        Assertions.assertEquals("Купить хлеб", task.getText());
        Assertions.assertEquals(day1, task.getDate());
    }

    // ---------- getTasksByDate ----------

    /**
     * Возвращаются только задачи указанного дня.
     */
    @Test
    public void testGetTasksByDateReturnsOnlyThatDay() {
        storage.addTask("Задача A", day1);
        storage.addTask("Задача B", day1);
        storage.addTask("Задача C", day2);

        List<Task> tasksOfDay1 = storage.getTasksByDate(day1);
        Assertions.assertEquals(2, tasksOfDay1.size());
        Assertions.assertEquals("Задача A", tasksOfDay1.get(0).getText());
        Assertions.assertEquals("Задача B", tasksOfDay1.get(1).getText());
    }

    /**
     * Пустой день возвращает пустой список (а не null).
     */
    @Test
    public void testGetTasksByDateForEmptyDayReturnsEmptyList() {
        List<Task> tasks = storage.getTasksByDate(day1);
        Assertions.assertNotNull(tasks);
        Assertions.assertTrue(tasks.isEmpty());
    }

    /**
     * Порядок задач — в порядке добавления (важно для нумерации 1, 2, 3 в выводе).
     */
    @Test
    public void testGetTasksByDatePreservesInsertionOrder() {
        storage.addTask("Первая", day1);
        storage.addTask("Вторая", day1);
        storage.addTask("Третья", day1);

        List<Task> tasks = storage.getTasksByDate(day1);
        Assertions.assertEquals("Первая", tasks.get(0).getText());
        Assertions.assertEquals("Вторая", tasks.get(1).getText());
        Assertions.assertEquals("Третья", tasks.get(2).getText());
    }

    /**
     * Возвращённый список — копия: его изменение не влияет на хранилище.
     */
    @Test
    public void testGetTasksByDateReturnsCopy() {
        storage.addTask("Задача", day1);
        storage.getTasksByDate(day1).clear();
        Assertions.assertEquals(1, storage.getTasksByDate(day1).size());
    }

    // ---------getTaskById ----------

    /**
     * По существующему ID возвращается задача.
     */
    @Test
    public void testGetTaskByIdFindsTask() {
        storage.addTask("Задача", day1);
        Task task = storage.getTaskById(1);
        Assertions.assertNotNull(task);
        Assertions.assertEquals("Задача", task.getText());
    }

    /**
     * По несуществующему ID возвращается null.
     */
    @Test
    public void testGetTaskByIdReturnsNullForMissing() {
        Assertions.assertNull(storage.getTaskById(999));
    }

    // ---------- deleteTask---------

    /**
     * Удаление убирает задачу из обоих индексов (по ID и из списка дня).
     */
    @Test
    public void testDeleteTaskRemovesFromBothIndexes() {
        storage.addTask("Задача", day1);

        Assertions.assertTrue(storage.deleteTask(1));
        Assertions.assertNull(storage.getTaskById(1));
        Assertions.assertTrue(storage.getTasksByDate(day1).isEmpty());
    }

    /**
     * Удаление одной задачи не затрагивает остальные задачи того же дня.
     */
    @Test
    public void testDeleteTaskKeepsOtherTasksOfSameDay() {
        storage.addTask("Задача A", day1);
        storage.addTask("Задача B", day1);

        storage.deleteTask(1);

        List<Task> tasks = storage.getTasksByDate(day1);
        Assertions.assertEquals(1, tasks.size());
        Assertions.assertEquals("Задача B", tasks.get(0).getText());
    }

    /**
     * Удаление несуществующей задачи возвращает false и ничего не ломает.
     */
    @Test
    public void testDeleteTaskReturnsFalseIfNotFound() {
        Assertions.assertFalse(storage.deleteTask(42));
    }

    // ----------updateTaskText------

    /**
     * Новый текст виден и через getTaskById, и через getTasksByDate
     * (доказывает, что в обоих индексах лежит один и тот же объект Task).
     */
    @Test
    public void testUpdateTaskTextChangesTextInBothViews() {
        storage.addTask("Старый", day1);

        Assertions.assertTrue(storage.updateTaskText(1, "Новый"));

        Assertions.assertEquals("Новый", storage.getTaskById(1).getText());
        Assertions.assertEquals("Новый", storage.getTasksByDate(day1).get(0).getText());
    }

    /**
     * Обновление несуществующей задачи возвращает false.
     */
    @Test
    public void testUpdateTaskTextReturnsFalseIfNotFound() {
        Assertions.assertFalse(storage.updateTaskText(42, "Новый"));
    }
}