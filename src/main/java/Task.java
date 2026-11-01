import java.time.LocalDate;

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
     * Создает задачу
     *
     * @param id   идентификатор
     * @param text текст задачи
     * @param date дата задачи
     */
    public Task(int id, String text, LocalDate date) {
        this.id = id;
        this.text = text;
        this.date = date;
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
     * Возвращает дату задачи
     *
     * @return дата
     */
    public LocalDate getDate() {
        return date;
    }
}