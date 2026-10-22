import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram-слой бота.
 * Принимает входящие обновления от Telegram API, передает текст
 * в {@link ResponseService} для формирования ответа и отправляет результат обратно.
 */
public class ProductivityTelegramBot extends TelegramLongPollingBot {

    /**
     * Имя бота в Telegram.
     */
    private final String username;
    /**
     * Логика формирования ответов.
     */
    private final ResponseService botLogic;

    /**
     * Создает Telegram-адаптер с указанными учётными данными:
     *
     * @param token    токен бота
     * @param username имя бота
     */
    public ProductivityTelegramBot(String token, String username) {
        super(token);
        this.username = username;
        this.botLogic = new ResponseService();
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String responseText = botLogic.getAnswer(update.getMessage().getText());
            SendMessage message = new SendMessage(
                    String.valueOf(update.getMessage().getChatId()),
                    responseText);
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