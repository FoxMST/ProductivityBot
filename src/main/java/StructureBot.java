/**
 * Логика бота: формирует ответ на сообщение пользователя.
 */
public class StructureBot {
    /**
     * Ответ на команду /start.
     */
    private static final String START_MESSAGE =
            "👋 Привет! Я бот-продуктивность.\nНапиши /help, чтобы увидеть команды.";

    /**
     * Ответ на команду /help.
     */
    private static final String HELP_MESSAGE =
            "📖 Доступные команды:\n"
                    + "/start — начать\n"
                    + "/help — справка\n"
                    + "/add_task — добавить задачу\n"
                    + "/show_tasklist — показать задачи за день\n"
                    + "/edit_task — изменить задачу\n"
                    + "/delete_task — удалить задачу\n"
                    + "Во время любой команды можно нажать ❌ Отмена.\n"
                    + "Просто напиши что-нибудь — я повторю.";

    private static final String CANCEL_MESSAGE =
            "Черновик задачи удален. Введите любую команду.";
    private static final String STALE_MESSAGE =
            "Это действие уже недоступно. Введите любую команду.";
    private static final String BAD_DATE_MESSAGE =
            "❌Неправильный ввод формата для даты, напишите повторно дату в формате ДД.ММ.ГГГГ";


    /**
     * Формирует ответ на сообщение.
     *
     * @param message текст пользователя
     * @return ответ бота
     */
    public String getAnswer(String message) {
        if (message.equals("/start")) {
            return START_MESSAGE;
        }
        if (message.equals("/help")) {
            return HELP_MESSAGE;
        }
        return "Вы ввели: «" + message + "»";
    }
}