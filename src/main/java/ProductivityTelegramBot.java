import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

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
            long chatId = update.getMessage().getChatId();
            send(chatId, botLogic.handleMessage(chatId, update.getMessage().getText()));
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    /**
     * Обрабатывает нажатие inline-кнопки.
     */
    private void handleCallback(CallbackQuery query) {
        long chatId = query.getMessage().getChatId();

        // Убираем «часики» на кнопке.
        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build());
        } catch (TelegramApiException e) {
            System.err.println("Не удалось ответить на callback: " + e.getMessage());
        }

        // Убираем кнопки из старого сообщения, чтобы на них нельзя было нажать повторно.
        try {
            execute(EditMessageReplyMarkup.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(query.getMessage().getMessageId())
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboard(new ArrayList<>())   // ← вот эта строчка
                            .build())
                    .build());
        } catch (TelegramApiException e) {
            System.err.println("Не удалось убрать кнопки: " + e.getMessage());
        }

        send(chatId, botLogic.handleAction(chatId, query.getData()));
    }

    /**
     * Отправляет ответы логики в чат.
     */
    private void send(long chatId, List<BotResponse> responses) {
        for (BotResponse response : responses) {
            SendMessage message = new SendMessage(String.valueOf(chatId), response.getText());
            InlineKeyboardMarkup markup = toMarkup(response);
            if (markup != null) {
                message.setReplyMarkup(markup);
            }
            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Не удалось отправить сообщение в чат " + chatId
                        + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Преобразует кнопки из BotResponse в формат Telegram.
     */
    private InlineKeyboardMarkup toMarkup(BotResponse response) {
        if (response.getKeyboard().isEmpty()) {
            return null;
        }
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (List<BotResponse.Button> row : response.getKeyboard()) {
            List<InlineKeyboardButton> tgRow = new ArrayList<>();
            for (BotResponse.Button button : row) {
                tgRow.add(InlineKeyboardButton.builder()
                        .text(button.getText())
                        .callbackData(button.getActionId())
                        .build());
            }
            keyboard.add(tgRow);
        }
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}