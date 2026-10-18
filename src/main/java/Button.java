import java.util.Arrays;
import java.util.List;

/**
 * Кнопки бота: действия (надпись + идентификатор) и готовые клавиатуры.
 */
public final class Button {

    /**
     * Все действия кнопок бота. Для каждой кнопки надпись и идентификатор
     * описываются в одном месте: идентификатор берётся из имени константы.
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
         * @return идентификатор, который придёт логике при нажатии кнопки
         */
        public String id() {
            return name();
        }

        /**
         * Находит действие по идентификатору.
         *
         * @param id идентификатор из нажатой кнопки
         * @return действие или null, если кнопка неизвестна
         */
        public static ButtonAction fromId(String id) {
            if (id == null) {
                return null;
            }
            try {
                return valueOf(id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private Button() {
    }

    /**
     * Сообщение с единственной кнопкой [❌ Отмена].
     */
    public static BotResponse cancel(String text) {
        return BotResponse.withButtons(text, rows(
                row(button(ButtonAction.CANCEL))));
    }

    /**
     * Сообщение с выбором дня: [Сегодня] [Завтра] [Ввести дату] [❌ Отмена].
     */
    public static BotResponse dayChoice(String text) {
        return BotResponse.withButtons(text, rows(
                row(button(ButtonAction.TODAY), button(ButtonAction.TOMORROW)),
                row(button(ButtonAction.CUSTOM_DATE), button(ButtonAction.CANCEL))));
    }

    /**
     * Подтверждение удаления: [🗑 Удалить] [❌ Отмена].
     */
    public static BotResponse deleteConfirm(String text) {
        return BotResponse.withButtons(text, rows(
                row(button(ButtonAction.DELETE_CONFIRM), button(ButtonAction.CANCEL))));
    }

    /**
     * Выбор поля для редактирования: [📝 Текст] [❌ Отмена].
     * Сюда же в будущем добавятся кнопки «Дата», «Время», «Дедлайн».
     */
    public static BotResponse editField(String text) {
        return BotResponse.withButtons(text, rows(
                row(button(ButtonAction.EDIT_TEXT), button(ButtonAction.CANCEL))));
    }

    private static BotResponse.Button button(ButtonAction action) {
        return new BotResponse.Button(action.label(), action.id());
    }

    private static List<BotResponse.Button> row(BotResponse.Button... buttons) {
        return Arrays.asList(buttons);
    }

    @SafeVarargs
    private static List<List<BotResponse.Button>> rows(List<BotResponse.Button>... rows) {
        return Arrays.asList(rows);
    }
}