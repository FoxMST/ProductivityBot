import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ответ логики бота: текст и (необязательно) inline-кнопки.
 * Не зависит от Telegram API.
 */
public final class BotResponse {

    /**
     * Inline-кнопка.
     */
    public static final class Button {
        private final String text;
        private final String actionId;

        /**
         * @param text         надпись на кнопке
         * @param actionId     идентификатор действия, который придёт логике при нажатии
         */
        public Button(String text, String actionId) {
            this.text = text;
            this.actionId = actionId;
        }

        public String getText() {
            return text;
        }

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

    private BotResponse(String text, List<List<Button>> keyboard) {
        this.text = text;
        this.keyboard = keyboard;
    }

    /**
     * Ответ без кнопок.
     */
    public static BotResponse of(String text) {
        return new BotResponse(text, Collections.<List<Button>>emptyList());
    }

    /**
     * Ответ с кнопками.
     */
    public static BotResponse withButtons(String text, List<List<Button>> keyboard) {
        return new BotResponse(text, new ArrayList<>(keyboard));
    }

    public String getText() {
        return text;
    }

    public List<List<Button>> getKeyboard() {
        return keyboard;
    }
}