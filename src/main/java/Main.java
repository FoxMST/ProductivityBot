import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


public class Main {

    /**
     * Читает имя и токен из файла .env, запускает бота
     */
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("BOT_TOKEN");
        String username = dotenv.get("BOT_USERNAME");


        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(new EchoBot(token, username));

        System.out.println("Бот запустился");
    }
}