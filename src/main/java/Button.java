import java.util.Arrays;
import java.util.List;

/**
 * Набор готовых ответов бота с inline-кнопками.
 */
public class Button {

    public Button() {
    }

    public BotResponse cancel(String text) {
        return new BotResponse(text, rows(row(button(ButtonAction.CANCEL))));
    }

    public BotResponse dayChoice(String text) {
        return new BotResponse(text, rows(row(button(ButtonAction.TODAY), button(ButtonAction.TOMORROW)),
                row(button(ButtonAction.CUSTOM_DATE), button(ButtonAction.CANCEL))));
    }

    public BotResponse deleteConfirm(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.DELETE_CONFIRM), button(ButtonAction.CANCEL))));
    }

    public BotResponse editField(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.EDIT_TEXT), button(ButtonAction.EDIT_DATETIME)),
                row(button(ButtonAction.EDIT_DEADLINE), button(ButtonAction.CANCEL))));
    }

    public BotResponse yesNo(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.YES), button(ButtonAction.NO), button(ButtonAction.CANCEL))));
    }

    /**
     * Сообщение с кнопкой «Пометить выполненные задачи».
     * Убрана кнопка Отмена, так как это просто вывод списка.
     */
    public BotResponse markDone(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.MARK_DONE))));
    }

    public ButtonAction fromId(String id) {
        if (id == null) {
            return null;
        }
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