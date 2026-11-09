import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Тесты для набора готовых ответов Button.
 * Проверяют обратное преобразование идентификатора нажатой кнопки.
 */
public class ButtonTest {

    /**
     * Тестируемый объект.
     */
    private Button buttons;

    /**
     * Создает новый экземпляр набора ответов перед каждым тестом.
     */
    @BeforeEach
    public void setUp() {
        buttons = new Button();
    }

    /**
     * Проверяет, что известные ID корректно преобразуются в действия.
     */
    @Test
    public void fromIdShouldRecognizeKnownActions() {
        Assertions.assertEquals(ButtonAction.CANCEL, buttons.fromId("CANCEL"));
        Assertions.assertEquals(ButtonAction.TODAY, buttons.fromId("TODAY"));
        Assertions.assertEquals(ButtonAction.TOMORROW, buttons.fromId("TOMORROW"));
        Assertions.assertEquals(ButtonAction.CUSTOM_DATE, buttons.fromId("CUSTOM_DATE"));
        Assertions.assertEquals(ButtonAction.DELETE_CONFIRM, buttons.fromId("DELETE_CONFIRM"));
        Assertions.assertEquals(ButtonAction.EDIT_TEXT, buttons.fromId("EDIT_TEXT"));
    }

    /**
     * Проверяет, что null на входе возвращает null.
     */
    @Test
    public void fromIdShouldReturnNullForNullInput() {
        Assertions.assertNull(buttons.fromId(null));
    }

    /**
     * Проверяет, что неизвестный ID возвращает null.
     */
    @Test
    public void fromIdShouldReturnNullForUnknownId() {
        Assertions.assertNull(buttons.fromId("UNKNOWN"));
        Assertions.assertNull(buttons.fromId(""));
        Assertions.assertNull(buttons.fromId("123"));
    }

    /**
     * Проверяет, что преобразование чувствительно к регистру.
     */
    @Test
    public void fromIdShouldBeCaseSensitive() {
        Assertions.assertNull(buttons.fromId("today"));
        Assertions.assertNull(buttons.fromId("Today"));
        Assertions.assertNull(buttons.fromId("cancel"));
    }
}