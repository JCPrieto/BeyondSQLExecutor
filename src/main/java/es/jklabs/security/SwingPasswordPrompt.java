package es.jklabs.security;

import javax.swing.*;
import java.awt.*;

class SwingPasswordPrompt implements PasswordPrompt {
    @Override
    public char[] request(Component parent, String title) {
        JPasswordField field = new JPasswordField(20);
        int result = JOptionPane.showConfirmDialog(parent, field, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return field.getPassword();
    }
}
