package es.jklabs.utilidades;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

public class UtilidadesString extends StringUtils {

    public static boolean isEmpty(JTextField jTextField) {
        return isEmpty(jTextField.getText().trim());
    }

    public static boolean isEmpty(JPasswordField jPasswordField) {
        return jPasswordField.getPassword().length == 0;
    }

    public static boolean isEmpty(JTextArea jTextArea) {
        return isEmpty(jTextArea.getText());
    }
}
