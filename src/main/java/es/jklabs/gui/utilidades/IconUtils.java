package es.jklabs.gui.utilidades;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IconUtils {
    private static final String ICONS_DIR = "img/icons/";
    private static final Map<String, ImageIcon> ICON_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ImageIcon> SCALED_CACHE = new ConcurrentHashMap<>();

    private IconUtils() {

    }

    public static ImageIcon loadIcon(String name) {
        return ICON_CACHE.computeIfAbsent(name, IconUtils::loadIconInternal);
    }

    public static Image loadImage(String name) {
        return IMAGE_CACHE.computeIfAbsent(name, IconUtils::loadImageInternal);
    }

    public static ImageIcon loadIconScaled(String name, int width, int height) {
        String key = name + ":" + width + "x" + height;
        return SCALED_CACHE.computeIfAbsent(key, k -> loadIconScaledInternal(name, width, height));
    }

    private static ImageIcon loadIconInternal(String name) {
        URL url = IconUtils.class.getClassLoader().getResource(ICONS_DIR + name);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url);
    }

    private static Image loadImageInternal(String name) {
        ImageIcon icon = loadIcon(name);
        if (icon == null) {
            return null;
        }
        return icon.getImage();
    }

    private static ImageIcon loadIconScaledInternal(String name, int width, int height) {
        ImageIcon icon = loadIcon(name);
        if (icon == null) {
            return null;
        }
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    public static void clearCaches() {
        ICON_CACHE.clear();
        IMAGE_CACHE.clear();
        SCALED_CACHE.clear();
    }
}
