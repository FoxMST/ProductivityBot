import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты логики эхо-бота.
 */

class StructureBotTest {

    /**
     * Тестируемый объект.
     */
    private final StructureBot bot = new StructureBot();

    /**
     * Проверяет, что на /start бот возвращает приветствие
     * с упоминанием команды /help.
     */
    @Test
    void shouldReturnGreetingForStart() {
        String response = bot.getAnswer("/start");
        Assertions.assertEquals("Привет", response);
        Assertions.assertEquals("/help", response);
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
