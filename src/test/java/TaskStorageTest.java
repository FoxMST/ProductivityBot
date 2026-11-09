import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

public class TaskStorageTest {
    private TaskStorage storage;
    private LocalDate day1;
    private LocalDate day2;

    @BeforeEach
    public void setUp() {
        storage = new TaskStorage();
        day1 = LocalDate.of(2026, 12, 12);
        day2 = LocalDate.of(2026, 12, 13);
    }

    @Test
    public void testAddTaskReturnsIncrementingIds() {
        Assertions.assertEquals(1, storage.addTask("Задача 1", day1));
        Assertions.assertEquals(2, storage.addTask("Задача 2", day1));
        Assertions.assertEquals(3, storage.addTask("Задача 3", day2));
    }

    @Test
    public void testIdsAreGlobalAcrossDates() {
        storage.addTask("На день 1", day1);
        Assertions.assertEquals(2, storage.addTask("На день 2", day2));
    }

    @Test
    public void testAddTaskStoresTextAndDate() {
        storage.addTask("Купить хлеб", day1);
        Task task = storage.getTaskById(1);
        Assertions.assertNotNull(task);
        Assertions.assertEquals("Купить хлеб", task.getText());
        Assertions.assertEquals(day1, task.getDate());
    }

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

    @Test
    public void testGetTasksByDateForEmptyDayReturnsEmptyList() {
        List<Task> tasks = storage.getTasksByDate(day1);
        Assertions.assertNotNull(tasks);
        Assertions.assertTrue(tasks.isEmpty());
    }

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

    @Test
    public void testGetTasksByDateReturnsCopy() {
        storage.addTask("Задача", day1);
        storage.getTasksByDate(day1).clear();
        Assertions.assertEquals(1, storage.getTasksByDate(day1).size());
    }

    @Test
    public void testGetTaskByIdFindsTask() {
        storage.addTask("Задача", day1);
        Task task = storage.getTaskById(1);
        Assertions.assertNotNull(task);
        Assertions.assertEquals("Задача", task.getText());
    }

    @Test
    public void testGetTaskByIdReturnsNullForMissing() {
        Assertions.assertNull(storage.getTaskById(999));
    }

    @Test
    public void testDeleteTaskRemovesFromBothIndexes() {
        storage.addTask("Задача", day1);
        storage.deleteTask(1);
        Assertions.assertNull(storage.getTaskById(1));
        Assertions.assertTrue(storage.getTasksByDate(day1).isEmpty());
    }

    @Test
    public void testDeleteTaskKeepsOtherTasksOfSameDay() {
        storage.addTask("Задача A", day1);
        storage.addTask("Задача B", day1);
        storage.deleteTask(1);
        List<Task> tasks = storage.getTasksByDate(day1);
        Assertions.assertEquals(1, tasks.size());
        Assertions.assertEquals("Задача B", tasks.get(0).getText());
    }

    @Test
    public void testDeleteTaskThrowsIfNotFound() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> storage.deleteTask(42));
        Assertions.assertEquals("Задача с ID 42 не найдена", exception.getMessage());
    }

    @Test
    public void testUpdateTaskTextChangesTextInBothViews() {
        storage.addTask("Старый", day1);
        storage.updateTaskText(1, "Новый");
        Assertions.assertEquals("Новый", storage.getTaskById(1).getText());
        Assertions.assertEquals("Новый", storage.getTasksByDate(day1).get(0).getText());
    }

    @Test
    public void testUpdateTaskTextThrowsIfNotFound() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> storage.updateTaskText(42, "Новый"));
        Assertions.assertEquals("Задача с ID 42 не найдена", exception.getMessage());
    }
}