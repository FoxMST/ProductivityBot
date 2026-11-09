import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

/**
 * Тесты для TaskStorage.
 * Все тесты используют фиксированные даты, чтобы результат не зависел от дня запуска.
 */
public class TaskStorageTest {

    /**
     * Тестируемое хранилище.
     */
    private TaskStorage storage;

    /**
     * Первая тестовая дата.
     */
    private LocalDate day1;

    /**
     * Вторая тестовая дата.
     */
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

    /**
     * Проверяет, что ID идут по возрастанию.
     */
    @Test
    public void testAddTaskReturnsIncrementingIds() {
        Assertions.assertEquals(1, storage.addTask("Задача 1", day1));
        Assertions.assertEquals(2, storage.addTask("Задача 2", day1));
        Assertions.assertEquals(3, storage.addTask("Задача 3", day2));
    }

    /**
     * Проверяет, что ID не сбрасывается при смене дня.
     */
    @Test
    public void testIdsAreGlobalAcrossDates() {
        storage.addTask("На день 1", day1);
        Assertions.assertEquals(2, storage.addTask("На день 2", day2));
    }

    /**
     * Проверяет, что задача сохраняет текст и дату.
     */
    @Test
    public void testAddTaskStoresTextAndDate() {
        storage.addTask("Купить хлеб", day1);
        Task task = storage.getTaskById(1);
        Assertions.assertNotNull(task);
        Assertions.assertEquals("Купить хлеб", task.getText());
        Assertions.assertEquals(day1, task.getDate());
    }

    /**
     * Проверяет, что возвращаются только задачи указанного дня.
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
     * Проверяет, что пустой день возвращает пустой список, а не null.
     */
    @Test
    public void testGetTasksByDateForEmptyDayReturnsEmptyList() {
        List<Task> tasks = storage.getTasksByDate(day1);
        Assertions.assertNotNull(tasks);
        Assertions.assertTrue(tasks.isEmpty());
    }

    /**
     * Проверяет, что порядок задач сохраняется (важно для нумерации).
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
     * Проверяет, что возвращается копия списка (изменение не влияет на хранилище).
     */
    @Test
    public void testGetTasksByDateReturnsCopy() {
        storage.addTask("Задача", day1);
        storage.getTasksByDate(day1).clear();
        Assertions.assertEquals(1, storage.getTasksByDate(day1).size());
    }

    /**
     * Проверяет, что задача находится по ID.
     */
    @Test
    public void testGetTaskByIdFindsTask() {
        storage.addTask("Задача", day1);
        Task task = storage.getTaskById(1);
        Assertions.assertNotNull(task);
        Assertions.assertEquals("Задача", task.getText());
    }

    /**
     * Проверяет, что по несуществующему ID возвращается null.
     */
    @Test
    public void testGetTaskByIdReturnsNullForMissing() {
        Assertions.assertNull(storage.getTaskById(999));
    }

    /**
     * Проверяет, что удаление убирает задачу из обоих индексов.
     */
    @Test
    public void testDeleteTaskRemovesFromBothIndexes() {
        storage.addTask("Задача", day1);
        storage.deleteTask(1);
        Assertions.assertNull(storage.getTaskById(1));
        Assertions.assertTrue(storage.getTasksByDate(day1).isEmpty());
    }

    /**
     * Проверяет, что удаление одной задачи не затрагивает остальные.
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
     * Проверяет, что удаление несуществующей задачи выбрасывает исключение.
     */
    @Test
    public void testDeleteTaskThrowsIfNotFound() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> storage.deleteTask(42));
        Assertions.assertEquals("Задача с ID 42 не найдена", exception.getMessage());
    }

    /**
     * Проверяет, что новый текст виден во всех представлениях.
     */
    @Test
    public void testUpdateTaskTextChangesTextInBothViews() {
        storage.addTask("Старый", day1);
        storage.updateTaskText(1, "Новый");
        Assertions.assertEquals("Новый", storage.getTaskById(1).getText());
        Assertions.assertEquals("Новый", storage.getTasksByDate(day1).get(0).getText());
    }

    /**
     * Проверяет, что обновление несуществующей задачи выбрасывает исключение.
     */
    @Test
    public void testUpdateTaskTextThrowsIfNotFound() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> storage.updateTaskText(42, "Новый"));
        Assertions.assertEquals("Задача с ID 42 не найдена", exception.getMessage());
    }
}