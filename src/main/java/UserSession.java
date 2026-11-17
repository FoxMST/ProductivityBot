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
     * Возвращает текущее состояние диалога.
     *
     * @return состояние диалога, не null
     */
    public State getState() {
        return state;
    }

    /**
     * @param state новое состояние
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * @return выбранная дата
     */
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    /**
     * @param selectedDate выбранная дата
     */
    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    /**
     * @return ID задачи
     */
    public int getPendingTaskId() {
        return pendingTaskId;
    }

    /**
     * @param pendingTaskId ID задачи
     */
    public void setPendingTaskId(int pendingTaskId) {
        this.pendingTaskId = pendingTaskId;
    }

    /**
     * @return текст черновика
     */
    public String getPendingText() {
        return pendingText;
    }

    /**
     * @param pendingText текст черновика
     */
    public void setPendingText(String pendingText) {
        this.pendingText = pendingText;
    }

    /**
     * @return время начала черновика
     */
    public LocalTime getPendingTime() {
        return pendingTime;
    }

    /**
     * @param pendingTime время начала черновика
     */
    public void setPendingTime(LocalTime pendingTime) {
        this.pendingTime = pendingTime;
    }

    /**
     * @return дата дедлайна черновика
     */
    public LocalDate getPendingDeadlineDate() {
        return pendingDeadlineDate;
    }

    /**
     * @param pendingDeadlineDate дата дедлайна черновика
     */
    public void setPendingDeadlineDate(LocalDate pendingDeadlineDate) {
        this.pendingDeadlineDate = pendingDeadlineDate;
    }

    /**
     * @return время дедлайна черновика
     */
    public LocalTime getPendingDeadlineTime() {
        return pendingDeadlineTime;
    }

    /**
     * @param pendingDeadlineTime время дедлайна черновика
     */
    public void setPendingDeadlineTime(LocalTime pendingDeadlineTime) {
        this.pendingDeadlineTime = pendingDeadlineTime;
    }
}