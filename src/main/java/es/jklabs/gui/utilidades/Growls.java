package es.jklabs.gui.utilidades;

import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Growls {

    private static final String NOTIFY_SEND = "notify-send";
    private static TrayIcon trayIcon;
    private static Boolean notifySendAvailable;

    private Growls() {

    }

    public static void init() {
        trayIcon = null;
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            SystemTray tray = SystemTray.getSystemTray();
            Image icon = IconUtils.loadImage("database.png");
            if (icon != null) {
                trayIcon = new TrayIcon(icon, Constantes.NOMBRE_APP);
                trayIcon.setImageAutoSize(true);
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    Logger.error("establecer.icono.systray", e);
                }
            }
        }
    }

    public static void mostrarError(String cuerpo, Exception e) {
        mostrarError(null, cuerpo, null, e);
    }

    public static void mostrarError(String titulo, String cuerpo, String[] parametros, Exception e) {
        mostrarGrowl(titulo, Mensajes.getError(cuerpo, parametros), TrayIcon.MessageType.ERROR, "--icon=dialog-error");
        Logger.error(Mensajes.getError(cuerpo, parametros), e);
    }

    public static void mostrarError(String titulo, String cuerpo, Exception e) {
        mostrarError(titulo, cuerpo, null, e);
    }

    public static void mostrarInfo(String cuerpo) {
        mostrarGrowl(null, Mensajes.getMensaje(cuerpo, null), TrayIcon.MessageType.INFO, "--icon=dialog-information");
    }

    public static void mostrarAviso(String titulo, String cuerpo) {
        mostrarAviso(titulo, cuerpo, null);
    }

    private static void mostrarGrowl(String titulo, String cuerpo, TrayIcon.MessageType type, String icon) {
        if (trayIcon != null) {
            trayIcon.displayMessage(titulo != null ? Mensajes.getMensaje(titulo) : null, cuerpo, type);
        } else {
            if (isNotifySendAvailable()) {
                try {
                    Runtime.getRuntime().exec(new String[]{NOTIFY_SEND,
                            titulo != null ? Mensajes.getMensaje(titulo) : Constantes.NOMBRE_APP,
                            cuerpo,
                            icon});
                    return;
                } catch (IOException e2) {
                    Logger.error(e2);
                }
            }
            JOptionPane.showMessageDialog(null,
                    cuerpo,
                    titulo != null ? Mensajes.getMensaje(titulo) : Constantes.NOMBRE_APP,
                    messageTypeToOption(type));
        }
    }

    private static boolean isNotifySendAvailable() {
        if (notifySendAvailable != null) {
            return notifySendAvailable;
        }
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            notifySendAvailable = false;
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            java.io.File candidate = new java.io.File(dir, NOTIFY_SEND);
            if (candidate.isFile() && candidate.canExecute()) {
                notifySendAvailable = true;
                return true;
            }
        }
        notifySendAvailable = false;
        return false;
    }

    private static int messageTypeToOption(TrayIcon.MessageType type) {
        if (type == TrayIcon.MessageType.ERROR) {
            return JOptionPane.ERROR_MESSAGE;
        }
        if (type == TrayIcon.MessageType.WARNING) {
            return JOptionPane.WARNING_MESSAGE;
        }
        return JOptionPane.INFORMATION_MESSAGE;
    }

    public static void mostrarAviso(String titulo, String cuerpo, String[] parametros) {
        mostrarGrowl(titulo, Mensajes.getError(cuerpo, parametros), TrayIcon.MessageType.WARNING, "--icon=dialog-warning");
    }
}
