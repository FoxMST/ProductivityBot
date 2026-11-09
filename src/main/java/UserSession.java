import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Сессия одного пользователя: состояние диалога и временные данные.
 */
public class UserSession {
    private State state;
    private LocalDate selectedDate;
    private int pendingTaskId;
    private String pendingText;
    private LocalTime pendingTime;
    private LocalDate pendingDeadlineDate;
    private LocalTime pendingDeadlineTime;

    /**
     * Создает сессию в состоянии IDLE.
     */
    public UserSession() {
        state = State.IDLE;
        pendingTaskId = 0;
    }

    /**
     * @return состояние
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