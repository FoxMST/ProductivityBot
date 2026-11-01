import java.time.LocalDate;
import java.time.LocalTime;

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
     * Черновик новой задачи: текст.
     */
    private String pendingText;
    /**
     * Черновик новой задачи: время начала.
     */
    private LocalTime pendingTime;
    /**
     * Черновик новой задачи: дата дедлайна.
     */
    private LocalDate pendingDeadlineDate;
    /**
     * Черновик новой задачи: время дедлайна.
     */
    private LocalTime pendingDeadlineTime;
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

    /**
     * Возвращает текст черновика.
     *
     * @return значение или null
     */
    public String getPendingText() {
        return pendingText;
    }

    /**
     * Устанавливает текст черновика.
     *
     * @param pendingText значение
     */
    public void setPendingText(String pendingText) {
        this.pendingText = pendingText;
    }

    /**
     * Возвращает время начала черновика.
     *
     * @return значение или null
     */
    public LocalTime getPendingTime() {
        return pendingTime;
    }

    /**
     * Устанавливает время начала черновика.
     *
     * @param pendingTime значение
     */
    public void setPendingTime(LocalTime pendingTime) {
        this.pendingTime = pendingTime;
    }

    /**
     * Возвращает дату дедлайна черновика.
     *
     * @return значение или null
     */
    public LocalDate getPendingDeadlineDate() {
        return pendingDeadlineDate;
    }

    /**
     * Устанавливает дату дедлайна черновика.
     *
     * @param pendingDeadlineDate значение
     */
    public void setPendingDeadlineDate(LocalDate pendingDeadlineDate) {
        this.pendingDeadlineDate = pendingDeadlineDate;
    }

    /**
     * Возвращает время дедлайна черновика.
     *
     * @return значение или null
     */
    public LocalTime getPendingDeadlineTime() {
        return pendingDeadlineTime;
    }

    /**
     * Устанавливает время дедлайна черновика.
     *
     * @param pendingDeadlineTime значение
     */
    public void setPendingDeadlineTime(LocalTime pendingDeadlineTime) {
        this.pendingDeadlineTime = pendingDeadlineTime;
    }
}