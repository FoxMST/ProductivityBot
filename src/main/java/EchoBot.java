import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/*
* Echo-bot отправляет пользователю обратно написанный текст
* */
public class EchoBot extends TelegramLongPollingBot {

    private final String username;
    private final StructureBot botLogic;

    /*
    * Создает бота
    * */
    public EchoBot(String token, String username) {
        super(token);
        this.username = username;
        this.botLogic = new StructureBot();
    }

    @Override
    public String getBotUsername() {
        return username;
    }
    /*
    * Если пришло текстовое сообщение, отправляет в тот же чат такой же текст
    * */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String responstext = botLogic.getAnswer(update.getMessage().getText());
            SendMessage message = new SendMessage(
                    String.valueOf(update.getMessage().getChatId()),
                    responstext);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}