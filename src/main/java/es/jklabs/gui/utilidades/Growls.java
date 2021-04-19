package es.jklabs.gui.utilidades;

import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class Growls {

    private static final String NOTIFY_SEND = "notify-send";
    private static TrayIcon trayIcon;

    private Growls() {

    }

    public static void init() {
        trayIcon = null;
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            SystemTray tray = SystemTray.getSystemTray();
            trayIcon = new TrayIcon(new ImageIcon(Objects.requireNonNull(Growls.class.getClassLoader().getResource
                    ("img/icons/database.png"))).getImage(), Constantes.NOMBRE_APP);
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                Logger.error("establecer.icono.systray", e);
            }
        }
    }

    public static void mostrarError(String cuerpo, Exception e) {
        mostrarError(null, cuerpo, null, e);
    }

    private static void mostrarError(String titulo, String cuerpo, String[] parametros, Exception e) {
        mostrarGrowl(titulo, Mensajes.getError(cuerpo, parametros), TrayIcon.MessageType.ERROR, "--icon=dialog-error");
        Logger.error(cuerpo, e);
    }

    public static void mostrarError(String titulo, String cuerpo, Exception e) {
        mostrarError(titulo, cuerpo, null, e);
    }

    public static void mostrarInfo(String titulo, String cuerpo) {
        mostrarInfo(titulo, cuerpo, null);
    }

    private static void mostrarInfo(String titulo, String cuerpo, String[] parametros) {
        mostrarGrowl(titulo, Mensajes.getMensaje(cuerpo, parametros), TrayIcon.MessageType.INFO, "--icon=dialog-information");
    }

    public static void mostrarAviso(String titulo, String cuerpo) {
        mostrarGrowl(titulo, Mensajes.getError(cuerpo), TrayIcon.MessageType.WARNING, "--icon=dialog-warning");
    }

    private static void mostrarGrowl(String titulo, String cuerpo, TrayIcon.MessageType type, String icon) {
        if (trayIcon != null) {
            trayIcon.displayMessage(titulo != null ? Mensajes.getMensaje(titulo) : null, cuerpo, type);
        } else {
            try {
                Runtime.getRuntime().exec(new String[]{NOTIFY_SEND,
                        titulo != null ? Mensajes.getMensaje(titulo) : Constantes.NOMBRE_APP,
                        cuerpo,
                        icon});
            } catch (IOException e2) {
                Logger.error(e2);
            }
        }
    }
}
