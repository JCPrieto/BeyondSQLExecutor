package es.jklabs.gui.utilidades;

import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class Growls {
    private static TrayIcon trayIcon;

    private Growls() {

    }

    public static void init() {
        trayIcon = null;
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            SystemTray tray = SystemTray.getSystemTray();
            trayIcon = new TrayIcon(new ImageIcon(Objects.requireNonNull(Growls.class.getClassLoader().getResource
                    ("img/icons/s3-bucket.png"))).getImage(), Constantes.NOMBRE_APP);
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                Logger.error("establecer.icono.systray", e);
            }
        }
    }
}
