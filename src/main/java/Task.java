import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Класс для задач.
 */
public class Task {
    /**
     * Уникальный идентификатор задачи.
     */
    private final int id;
    /**
     * Текст задачи.
     */
    private String text;
    /**
     * Дата, на которую назначена задача.
     */
    private LocalDate date;
    /**
     * Рекомендуемое время начала выполнения.
     */
    private LocalTime startTime;
    /**
     * Дата дедлайна.
     */
    private LocalDate deadlineDate;
    /**
     * Время дедлайна.
     */
    private LocalTime deadlineTime;
    /**
     * Статус выполнения (true = выполнена).
     */
    private boolean done;

    /**
     * Создает задачу.
     *
     * @param id   идентификатор
     * @param text текст задачи
     * @param date дата задачи
     */
    public Task(int id, String text, LocalDate date) {
        this.id = id;
        this.text = text;
        this.date = date;
        this.done = false;
    }

    /**
     * Возвращает идентификатор задачи.
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Возвращает текст задачи.
     *
     * @return текст
     */
    public String getText() {
        return text;
    }

    /**
     * Устанавливает новый текст задачи.
     *
     * @param text новый текст
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Возвращает дату задачи.
     *
     * @return дата
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Устанавливает новую дату задачи.
     *
     * @param date новая дата
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Возвращает рекомендуемое время начала.
     *
     * @return время или null
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Устанавливает рекомендуемое время начала.
     *
     * @param startTime время или null
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Возвращает дату дедлайна.
     *
     * @return дата или null
     */
    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    /**
     * Устанавливает дату дедлайна.
     *
     * @param deadlineDate дата или null
     */
    public void setDeadlineDate(LocalDate deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    /**
     * Возвращает время дедлайна.
     *
     * @return время или null
     */
    public LocalTime getDeadlineTime() {
        return deadlineTime;
    }

    /**
     * Устанавливает время дедлайна.
     *
     * @param deadlineTime время или null
     */
    public void setDeadlineTime(LocalTime deadlineTime) {
        this.deadlineTime = deadlineTime;
    }

    /**
     * Возвращает статус выполнения.
     *
     * @return true, если задача выполнена
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Устанавливает статус выполнения.
     *
     * @param done новый статус
     */
    public void setDone(boolean done) {
        this.done = done;
    }
}