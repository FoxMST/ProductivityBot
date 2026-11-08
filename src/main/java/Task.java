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
     * Текст задачи
     */
    private String text;
    /**
     * Дата, на которую назначена задача
     */
    private final LocalDate date;
    /**
     * Рекомендуемое время начала.
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

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(LocalDate deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public LocalTime getDeadlineTime() {
        return deadlineTime;
    }

    public void setDeadlineTime(LocalTime deadlineTime) {
        this.deadlineTime = deadlineTime;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}