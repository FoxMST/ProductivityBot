/**
 * Класс Inline-кнопки, которую видит пользователь.
 */
public final class ResponseButton {
    /**
     * Надпись на кнопке.
     */
    private final String text;
    /**
     * Идентификатор действия.
     */
    private final String actionId;

    /**
     * @param text     надпись на кнопке
     * @param actionId идентификатор действия
     */
    public ResponseButton(String text, String actionId) {
        this.text = text;
        this.actionId = actionId;
    }

    /**
     * Возвращает надпись на кнопке.
     *
     * @return надпись
     */
    public String getText() {
        return text;
    }

    /**
     * Возвращает идентификатор действия.
     *
     * @return идентификатор
     */
    public String getActionId() {
        return actionId;
    }
}