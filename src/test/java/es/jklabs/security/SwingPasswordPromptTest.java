package es.jklabs.security;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class SwingPasswordPromptTest {
    @Test
    void createsPromptWithSwingDialog() {
        assertNotNull(new SwingPasswordPrompt());
    }

    @Test
    void returnsNullWhenDialogIsCancelled() {
        SwingPasswordPrompt prompt = new SwingPasswordPrompt(
                (parent, message, title, optionType, messageType) -> JOptionPane.CANCEL_OPTION);

        assertNull(prompt.request(null, "Contraseña maestra"));
    }

    @Test
    void returnsEnteredPasswordWhenDialogIsAccepted() {
        JPanel parent = new JPanel();
        String title = "Contraseña maestra";
        boolean[] dialogParametersVerified = {false};
        SwingPasswordPrompt prompt = new SwingPasswordPrompt((actualParent, message, actualTitle, optionType,
                                                              messageType) -> {
            assertSame(parent, actualParent);
            assertEquals(title, actualTitle);
            assertEquals(JOptionPane.OK_CANCEL_OPTION, optionType);
            assertEquals(JOptionPane.PLAIN_MESSAGE, messageType);
            JPasswordField field = assertInstanceOf(JPasswordField.class, message);
            assertEquals(20, field.getColumns());
            field.setText("secreto");
            dialogParametersVerified[0] = true;
            return JOptionPane.OK_OPTION;
        });

        assertArrayEquals("secreto".toCharArray(), prompt.request(parent, title));
        assertTrue(dialogParametersVerified[0]);
    }
}
