package es.jklabs.utilidades;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilidadesStringTest {

    @Test
    void isEmptyTextFieldTreatsBlankAsEmpty() {
        JTextField field = new JTextField("   ");

        assertTrue(UtilidadesString.isEmpty(field));
    }

    @Test
    void isEmptyTextFieldReturnsFalseForText() {
        JTextField field = new JTextField("value");

        assertFalse(UtilidadesString.isEmpty(field));
    }

    @Test
    void isEmptyTextAreaReturnsTrueForEmpty() {
        JTextArea area = new JTextArea();

        assertTrue(UtilidadesString.isEmpty(area));
    }

    @Test
    void isEmptyTextAreaReturnsFalseForWhitespace() {
        JTextArea area = new JTextArea(" ");

        assertFalse(UtilidadesString.isEmpty(area));
    }
}
