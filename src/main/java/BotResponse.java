import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Класс ответа логики бота: текст и inline-кнопки.
 */
public final class BotResponse {

    /**
     * Текст сообщения.
     */
    private final String text;
    /**
     * Кнопки: список рядов.
     */
    private final List<List<ResponseButton>> keyboard;

    /**
     * Ответ без кнопок.
     *
     * @param text текст сообщения
     */
    public BotResponse(String text) {
        this(text, Collections.<List<ResponseButton>>emptyList());
    }

    /**
     * Ответ с кнопками.
     *
     * @param text     текст сообщения
     * @param keyboard кнопки (список рядов)
     */
    public BotResponse(String text, List<List<ResponseButton>> keyboard) {
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
    public List<List<ResponseButton>> getKeyboard() {
        return keyboard;
    }
}