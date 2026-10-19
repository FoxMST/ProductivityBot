import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ответ логики бота: текст и inline-кнопки.
 */
public final class BotResponse {

    /**
     * Inline-кнопка.
     */
    public static final class Button {
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
        public Button(String text, String actionId) {
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

    /**
     * Текст сообщения.
     */
    private final String text;
    /**
     * Кнопки: список рядов.
     */
    private final List<List<Button>> keyboard;

    /**
     * Ответ без кнопок.
     *
     * @param text текст сообщения
     */
    public BotResponse(String text) {
        this(text, Collections.<List<Button>>emptyList());
    }

    /**
     * Ответ с кнопками.
     *
     * @param text     текст сообщения
     * @param keyboard кнопки (список рядов)
     */
    public BotResponse(String text, List<List<Button>> keyboard) {
        this.text = text;
        this.keyboard = new ArrayList<>(keyboard);
    }

    /**
     * Возвращает текст сообщения.
     *
     * @return текст
     */
    public String getText() {
        return text;
    }

    /**
     * Возвращает кнопки.
     *
     * @return список рядов кнопок (может быть пустым)
     */
    public List<List<Button>> getKeyboard() {
        return keyboard;
    }
}