import java.time.LocalDate;

/**
 * Сессия одного пользователя: состояние диалога и временные данные.
 */
public class UserSession {
    /**
     * Текущее состояние диалога.
     */
    private State state;
    /**
     * Выбранная дата (для удаления и редактирования).
     */
    private LocalDate selectedDate;
    /**
     * ID задачи, с которой выполняется действие (0 = нет).
     */
    private int pendingTaskId;

    /**
     * Создает сессию в состоянии IDLE.
     */
    public UserSession() {
        state = State.IDLE;
        pendingTaskId = 0;
    }

    /**
     * Возвращает состояние.
     *
     * @return состояние
     */
    public State getState() {
        return state;
    }

    /**
     * Устанавливает состояние.
     *
     * @param state новое состояние
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Возвращает выбранную дату.
     *
     * @return дата или null
     */
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    /**
     * Устанавливает выбранную дату.
     *
     * @param selectedDate дата
     */
    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    /**
     * Возвращает ID задачи.
     *
     * @return ID или 0
     */
    public int getPendingTaskId() {
        return pendingTaskId;
    }

    /**
     * Устанавливает ID задачи.
     *
     * @param pendingTaskId ID
     */
    public void setPendingTaskId(int pendingTaskId) {
        this.pendingTaskId = pendingTaskId;
    }
}