/**
 * Логика эхо-бота: формирует ответ на сообщение пользователя.
 */
public class StructureBot {

    /** Ответ на команду /start. */
    private final String startMessage =
            "👋 Привет! Я бот-продуктивность.\nНапиши /help, чтобы увидеть команды.";

    /** Ответ на команду /help. */
    private final String helpMessage =
            "📖 Доступные команды:\n/start — начать\n/help — справка\n"
                    + "Просто напиши что-нибудь — я повторю.";

    /**
     * Формирует ответ на сообщение.
     *
     * @param message текст пользователя
     * @return ответ бота
     */
    public String getAnswer(String message) {
        if (message.equals("/start")) {
            return startMessage;
        }
        if (message.equals("/help")) {
            return helpMessage;
        }
        return "Вы ввели: «" + message + "»";
    }
}