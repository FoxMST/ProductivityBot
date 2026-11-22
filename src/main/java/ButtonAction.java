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
    MARK_DONE("🖊️ Пометить выполненные задачи"),
    /**
     * Показать только задачи с дедлайном.
     */
    ONLY_DEADLINE("⏳ Только с дедлайном"),
    /**
     * Интервал «ближайшие 7 дней».
     */
    WEEK("📅 За эту неделю"),
    /**
     * Интервал «ближайшие 14 дней».
     */
    TWO_WEEKS("📅 На эти 2 недели"),
    /**
     * Интервал «ближайший месяц».
     */
    MONTH("📅 За месяц"),
    /**
     * Переход на предыдущую страницу списка.
     */
    PREV_PAGE("◀️"),
    /**
     * Переход на следующую страницу списка.
     */
    NEXT_PAGE("▶️"),
    /**
     * Индикатор текущей страницы (служебная кнопка).
     */
    PAGE_INFO("");

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