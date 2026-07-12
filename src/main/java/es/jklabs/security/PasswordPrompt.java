package es.jklabs.security;

import java.awt.*;

@FunctionalInterface
interface PasswordPrompt {
    char[] request(Component parent, String title);
}
