import java.util.Arrays;
import java.util.List;

/**
 * Набор готовых ответов бота с inline-кнопками.
 */
public class Button {

    /**
     * Создает экземпляр набора готовых ответов.
     * Класс не хранит состояния, поэтому один экземпляр может
     * использоваться многократно для сборки любых типовых сообщений.
     */
    public Button() {
    }

    /**
     * Сообщение с единственной кнопкой [❌ Отмена].
     *
     * @param text текст сообщения
     * @return ответ
     */
    public BotResponse cancel(String text) {
        return new BotResponse(text, rows(row(button(ButtonAction.CANCEL))));
    }

    /**
     * Сообщение с выбором дня.
     *
     * @param text текст сообщения
     * @return ответ
     */
    public BotResponse dayChoice(String text) {
        return new BotResponse(text, rows(row(button(ButtonAction.TODAY), button(ButtonAction.TOMORROW)),
                row(button(ButtonAction.CUSTOM_DATE), button(ButtonAction.CANCEL))));
    }

    /**
     * Подтверждение удаления.
     *
     * @param text текст сообщения
     * @return ответ
     */
    public BotResponse deleteConfirm(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.DELETE_CONFIRM), button(ButtonAction.CANCEL))));
    }

    /**
     * Выбор поля для редактирования.
     *
     * @param text текст сообщения
     * @return ответ
     */
    public BotResponse editField(String text) {
        return new BotResponse(text, rows(
                row(button(ButtonAction.EDIT_TEXT), button(ButtonAction.CANCEL))));
    }

    /**
     * Находит действие по идентификатору.
     *
     * @param id идентификатор из нажатой кнопки
     * @return действие или null, если кнопка неизвестна
     */
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