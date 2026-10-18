import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Связывает Telegram API с StructureBot.
 */
public class EchoBot extends TelegramLongPollingBot {

    /**
     * Имя бота в Telegram.
     */
    private final String username;
    /**
     * Логика формирования ответов.
     */
    private final StructureBot botLogic;

    /**
     * Создает Telegram-адаптер с указанными учётными данными:
     *
     * @param token    токен бота
     * @param username имя бота
     */
    public EchoBot(String token, String username) {
        super(token);
        this.username = username;
        this.botLogic = new StructureBot();
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String responsetext = botLogic.getAnswer(update.getMessage().getText());
            SendMessage message = new SendMessage(
                    String.valueOf(update.getMessage().getChatId()),
                    responsetext);
            try {
                execute(message);
            } catch (TelegramApiException e) {

                String chatId = String.valueOf(update.getMessage().getChatId());

                System.err.println("Не удалось отправить сообщение в чат " + chatId
                        + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}