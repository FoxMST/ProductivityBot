import java.util.Arrays;
import java.util.List;

/**
 * Фабрика готовых ответов с inline-кнопками.
 */
public class Button {

    /**
     * Все действия кнопок бота.
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

    /**
     * Создает фабрику кнопок.
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
        return new BotResponse(text, rows(
                row(button(ButtonAction.TODAY), button(ButtonAction.TOMORROW)),
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

    private BotResponse.Button button(ButtonAction action) {
        return new BotResponse.Button(action.label(), action.id());
    }

    private List<BotResponse.Button> row(BotResponse.Button... buttons) {
        return Arrays.asList(buttons);
    }

    @SafeVarargs
    private final List<List<BotResponse.Button>> rows(List<BotResponse.Button>... rows) {
        return Arrays.asList(rows);
    }
}