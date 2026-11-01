/**
 * Перечень всех возможных действий кнопок в боте.
 * Каждое действие имеет две характеристики:
 *  {@link #label()} — надпись, которую видит пользователь;
 *  {@link #id()} — технический идентификатор, который Telegram
 *  присылает обратно в {@code callbackData} при нажатии.
 */
public enum ButtonAction {
    CANCEL("❌ Отмена"),
    TODAY("📅 Сегодня"),
    TOMORROW("📅 Завтра"),
    CUSTOM_DATE("📅 Ввести дату"),
    DELETE_CONFIRM("🗑 Удалить"),
    EDIT_TEXT("📝 Текст");

    /**
     * Надпись на кнопке.
     */
    private final String label;

    /**
     * @param label надпись
     */
    ButtonAction(String label) {
        this.label = label;
    }

    /**
     * @return надпись, которую видит пользователь
     */
    public String label() {
        return label;
    }

    /**
     * @return идентификатор для нажатия
     */
    public String id() {
        return name();
    }
}