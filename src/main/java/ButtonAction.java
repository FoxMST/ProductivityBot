/**
 * Перечень всех возможных действий кнопок в боте.
 */
public enum ButtonAction {
    CANCEL("❌ Отмена"),
    TODAY("📅 Сегодня"),
    TOMORROW("📅 Завтра"),
    CUSTOM_DATE("📅 Ввести дату"),
    DELETE_CONFIRM("🗑 Удалить"),
    EDIT_TEXT("📝 Текст"),
    EDIT_DATETIME("📅 Дату/время"),
    EDIT_DEADLINE("⏰ Дедлайн"),
    YES("Да"),
    NO("Нет"),
    MARK_DONE("🖊️ Пометить выполненные задачи");

    private final String label;

    /**
     * @param label надпись
     */
    ButtonAction(String label) {
        this.label = label;
    }

    /**
     * @return надпись на кнопке
     */
    public String label() {
        return label;
    }

    /**
     * @return технический идентификатор
     */
    public String id() {
        return name();
    }
}