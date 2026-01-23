package es.jklabs.gui.utilidades;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class IconUtils {
    private static final String ICONS_DIR = "img/icons/";

    private IconUtils() {

    }

    public static ImageIcon loadIcon(String name) {
        URL url = IconUtils.class.getClassLoader().getResource(ICONS_DIR + name);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url);
    }

    public static Image loadImage(String name) {
        ImageIcon icon = loadIcon(name);
        if (icon == null) {
            return null;
        }
        return icon.getImage();
    }

    public static ImageIcon loadIconScaled(String name, int width, int height) {
        ImageIcon icon = loadIcon(name);
        if (icon == null) {
            return null;
        }
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}
