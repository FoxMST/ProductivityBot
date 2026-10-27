import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты для проверки логики формирования ответов.
 */
class ResponseBotTest {

    /**
     * Тестируемый объект.
     */
    private final ResponseService bot = new ResponseService();

    /**
     * Проверяет, что на /start бот возвращает приветствие
     * с упоминанием команды /help.
     */
    @Test
    void shouldReturnGreetingForStart() {
        String expected = "👋 Привет! Я бот-продуктивность.\n" +
                "Напиши /help, чтобы увидеть команды.";
        String response = bot.getAnswer("/start");

        Assertions.assertEquals(expected, response);
    }

    /**
     * Проверяет, что на произвольный текст бот отвечает
     * в формате «Вы ввели: «...»».
     */
    @Test
    void shouldEchoText() {
        String response = bot.getAnswer("привет");
        Assertions.assertEquals("Вы ввели: «привет»", response);
    }

    /**
     * Проверяет, что неизвестная команда обрабатывается как обычный текст.
     */
    @Test
    void shouldEchoUnknownCommand() {
        String response = bot.getAnswer("/unknown");
        Assertions.assertEquals("Вы ввели: «/unknown»", response);
    }
}
