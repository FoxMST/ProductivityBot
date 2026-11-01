import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * Тесты для UserSession.
 * Проверяют начальные значения и работу сеттеров
 */
public class UserSessionTest {
    private UserSession session;

    /**
     * Создает новую сессию перед каждым тестом.
     */
    @BeforeEach
    public void setUp() {
        session = new UserSession();
    }

    /**
     * Новая сессия должна быть в состоянии IDLE
     * Без этого switch упал бы на null.
     */
    @Test
    public void testNewSessionStartsInIdle() {
        Assertions.assertEquals(State.IDLE, session.getState());
    }

    /**
     * Новая сессия не должна иметь выбранной даты (null).
     * Если бы было LocalDate.now() - можно было бы случайно
     * удалить задачу не того дня, который пользователь ещё не выбирал.
     */
    @Test
    public void testNewSessionHasNoSelectedDate() {
        Assertions.assertNull(session.getSelectedDate());
    }

    /**
     * Новая сессия не должна иметь ожидающей задачи (0)
     * 0 - маркер "нет задачи", т.к. валидные ID начинаются с 1.
     */
    @Test
    public void testNewSessionHasNoPendingTask() {
        Assertions.assertEquals(0, session.getPendingTaskId());
    }

    /**
     * setState меняет состояние сессии.
     */
    @Test
    public void testSetState() {
        session.setState(State.ADD_TASK_WAIT_TEXT);
        Assertions.assertEquals(State.ADD_TASK_WAIT_TEXT, session.getState());
    }

    /**
     * setSelectedDate сохраняет дату.
     */
    @Test
    public void testSetSelectedDate() {
        LocalDate date = LocalDate.of(2026, 12, 12);
        session.setSelectedDate(date);
        Assertions.assertEquals(date, session.getSelectedDate());
    }

    /**
     * setSelectedDate(null) сбрасывает дату.
     * Полезно, если понадобится явно очистить выбор.
     */
    @Test
    public void testClearSelectedDate() {
        session.setSelectedDate(LocalDate.of(2026, 12, 12));
        session.setSelectedDate(null);
        Assertions.assertNull(session.getSelectedDate());
    }

    /**
     * setPendingTaskId сохраняет ID задачи.
     */
    @Test
    public void testSetPendingTaskId() {
        session.setPendingTaskId(42);
        Assertions.assertEquals(42, session.getPendingTaskId());
    }

    /**
     * setPendingTaskId(0) сбрасывает ID.
     */
    @Test
    public void testResetPendingTaskId() {
        session.setPendingTaskId(42);
        session.setPendingTaskId(0);
        Assertions.assertEquals(0, session.getPendingTaskId());
    }
}