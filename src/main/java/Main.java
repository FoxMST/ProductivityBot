import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void Main(String[] args) throws Exception {
        String token = System.getenv("BOT_TOKEN");
        String username = System.getenv("BOT_USERNAME");

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(new EchoBot(token, username));

        System.out.println("Bot started");
    }
}