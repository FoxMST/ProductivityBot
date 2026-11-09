import java.util.Arrays;
import java.util.List;

/**
 * Набор готовых ответов бота с inline-кнопками.
 */
public class Button {

    /**
     * Создает экземпляр набора ответов.
     */
    public Button() {
    }

    /**
     * @param text текст сообщения @return ответ с кнопкой отмены
     */
    public BotResponse cancel(String text) {
        return new BotResponse(text, rows(row(button(ButtonAction.CANCEL))));
    }

    /**
     * @param text текст сообщения @return ответ с выбором дня
     */
    public BotResponse dayChoice(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.TODAY), button(ButtonAction.TOMORROW)),
                row(button(ButtonAction.CUSTOM_DATE), button(ButtonAction.CANCEL))));
    }

    /**
     * @param text текст сообщения @return ответ с подтверждением удаления
     */
    public BotResponse deleteConfirm(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.DELETE_CONFIRM), button(ButtonAction.CANCEL))));
    }

    /**
     * @param text текст сообщения @return ответ с выбором поля для редактирования
     */
    public BotResponse editField(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.EDIT_TEXT), button(ButtonAction.EDIT_DATETIME)),
                row(button(ButtonAction.EDIT_DEADLINE), button(ButtonAction.CANCEL))));
    }

    /**
     * @param text текст сообщения @return ответ с кнопками Да/Нет
     */
    public BotResponse yesNo(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.YES), button(ButtonAction.NO), button(ButtonAction.CANCEL))));
    }

    /**
     * @param text текст сообщения @return ответ с кнопкой пометки выполнения
     */
    public BotResponse markDone(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.MARK_DONE))));
    }

    /**
     * @param id идентификатор из нажатой кнопки @return действие или null
     */
    public ButtonAction fromId(String id) {
        if (id == null) return null;
        try {
            return ButtonAction.valueOf(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ResponseButton button(ButtonAction action) {
        return new ResponseButton(action.label(), action.id());
    }

    private List<ResponseButton> row(ResponseButton... buttons) {
        return Arrays.asList(buttons);
    }

    @SafeVarargs
    private final List<List<ResponseButton>> rows(List<ResponseButton>... rows) {
        return Arrays.asList(rows);
    }
}