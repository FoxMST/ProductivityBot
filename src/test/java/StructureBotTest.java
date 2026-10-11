import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты логики эхо-бота.
 */
class StructureBotTest {

    /** Тестируемый объект. */
    private final StructureBot bot = new StructureBot();

    @Test
    void shouldReturnGreetingForStart() {
        String response = bot.getAnswer("/start");
        assertTrue(response.contains("Привет"));
        assertTrue(response.contains("/help"));
    }

    @Test
    void shouldReturnHelpForHelp() {
        String response = bot.getAnswer("/help");
        assertTrue(response.contains("/start"));
        assertTrue(response.contains("/help"));
    }

    @Test
    void shouldEchoText() {
        String response = bot.getAnswer("привет");
        assertEquals("Вы ввели: «привет»", response);
    }

    @Test
    void shouldEchoUnknownCommand() {
        String response = bot.getAnswer("/unknown");
        assertEquals("Вы ввели: «/unknown»", response);
    }
}
