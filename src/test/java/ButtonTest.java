import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ButtonTest {
    private Button buttons;

    @BeforeEach
    public void setUp() {
        buttons = new Button();
    }

    @Test
    public void fromIdShouldRecognizeKnownActions() {
        Assertions.assertEquals(ButtonAction.CANCEL, buttons.fromId("CANCEL"));
        Assertions.assertEquals(ButtonAction.TODAY, buttons.fromId("TODAY"));
        Assertions.assertEquals(ButtonAction.TOMORROW, buttons.fromId("TOMORROW"));
        Assertions.assertEquals(ButtonAction.CUSTOM_DATE, buttons.fromId("CUSTOM_DATE"));
        Assertions.assertEquals(ButtonAction.DELETE_CONFIRM, buttons.fromId("DELETE_CONFIRM"));
        Assertions.assertEquals(ButtonAction.EDIT_TEXT, buttons.fromId("EDIT_TEXT"));
    }

    @Test
    public void fromIdShouldReturnNullForNullInput() {
        Assertions.assertNull(buttons.fromId(null));
    }

    @Test
    public void fromIdShouldReturnNullForUnknownId() {
        Assertions.assertNull(buttons.fromId("UNKNOWN"));
        Assertions.assertNull(buttons.fromId(""));
        Assertions.assertNull(buttons.fromId("123"));
    }

    @Test
    public void fromIdShouldBeCaseSensitive() {
        Assertions.assertNull(buttons.fromId("today"));
        Assertions.assertNull(buttons.fromId("Today"));
        Assertions.assertNull(buttons.fromId("cancel"));
    }
}