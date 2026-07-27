package es.jklabs.security;

import javax.swing.*;
import java.awt.*;

class SwingPasswordPrompt implements PasswordPrompt {
    private final ConfirmDialog confirmDialog;

    SwingPasswordPrompt() {
        this(JOptionPane::showConfirmDialog);
    }

    SwingPasswordPrompt(ConfirmDialog confirmDialog) {
        this.confirmDialog = confirmDialog;
    }

    @Override
    public char[] request(Component parent, String title) {
        JPasswordField field = new JPasswordField(20);
        int result = confirmDialog.show(parent, field, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return field.getPassword();
    }

    @FunctionalInterface
    interface ConfirmDialog {
        int show(Component parent, Object message, String title, int optionType, int messageType);
    }
}
