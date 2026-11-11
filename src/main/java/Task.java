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
     * @return целое число больше 0, уникальное для каждой задачи
     */
    public int getId() {
        return id;
    }

    /**
     * Возвращает текст задачи.
     *
     * @return текст задачи в том виде, в котором его ввел пользователь
     */
    public String getText() {
        return text;
    }

    /**
     * Устанавливает новый текст задачи.
     *
     * @param text новый текст задачи
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return дата задачи
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * @param date новая дата задачи
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * @return рекомендуемое время начала
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * @param startTime рекомендуемое время начала
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * @return дата дедлайна
     */
    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    /**
     * @param deadlineDate дата дедлайна
     */
    public void setDeadlineDate(LocalDate deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    /**
     * @return время дедлайна
     */
    public LocalTime getDeadlineTime() {
        return deadlineTime;
    }

    /**
     * @param deadlineTime время дедлайна
     */
    public void setDeadlineTime(LocalTime deadlineTime) {
        this.deadlineTime = deadlineTime;
    }

    /**
     * @return true, если задача выполнена
     */
    public boolean isDone() {
        return done;
    }

    /**
     * @param done новый статус выполнения
     */
    public void setDone(boolean done) {
        this.done = done;
    }
}