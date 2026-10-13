/**
 * Логика эхо-бота: формирует ответ на сообщение пользователя.
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
            "📖 Доступные команды:\n/start — начать\n/help — справка\n"
                    + "Просто напиши " +
                    "что-нибудь — я повторю.";

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