package es.jklabs.utilidades;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Mensajes {

    private Mensajes() {

    }

    public static String getError(String key) {
        return getResource("i18n/errores", key);
    }

    private static String getResource(String resource, String key) {
        ResourceBundle bundle = ResourceBundle.getBundle(resource, Locale.getDefault());
        String text;
        try {
            text = bundle.getString(key);
        } catch (MissingResourceException e) {
            text = key;
        }
        return text;
    }

    public static String getMensaje(String key) {
        return getResource("i18n/mensajes", key);
    }

    public static String getError(String key, String[] param) {
        String text = getError(key);
        return addParametros(param, text);
    }

    private static String addParametros(String[] param, String text) {
        if (param != null) {
            MessageFormat mf = new MessageFormat(text, Locale.getDefault());
            text = mf.format(param, new StringBuffer(), null).toString();
        }
        return text;
    }
}
