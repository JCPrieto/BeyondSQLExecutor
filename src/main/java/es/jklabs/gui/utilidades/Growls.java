package es.jklabs.gui.utilidades;

import com.sshtools.twoslices.*;
import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import java.net.URL;

public class Growls {

    private static DesktopNotifier notifier = new TwoSlicesDesktopNotifier();
    private static URL notificationIcon;

    private Growls() {

    }

    public static void init() {
        notificationIcon = Growls.class.getResource("/img/icons/database.png");
        ToasterSettings settings = new ToasterSettings()
                .setAppName(Constantes.NOMBRE_APP)
                .setTimeout(10);
        if (notificationIcon != null) {
            settings.setDefaultImage(notificationIcon);
        }
        ToasterFactory.setSettings(settings);
    }

    public static void mostrarError(String cuerpo, Exception e) {
        mostrarError(null, cuerpo, null, e);
    }

    public static void mostrarError(String titulo, String cuerpo, String[] parametros, Exception e) {
        mostrarGrowl(titulo, Mensajes.getError(cuerpo, parametros), NotificationType.ERROR);
        Logger.error(Mensajes.getError(cuerpo, parametros), e);
    }

    public static void mostrarError(String titulo, String cuerpo, Exception e) {
        mostrarError(titulo, cuerpo, null, e);
    }

    public static void mostrarInfo(String cuerpo) {
        mostrarGrowl(null, Mensajes.getMensaje(cuerpo, null), NotificationType.INFO);
    }

    public static void mostrarAviso(String titulo, String cuerpo) {
        mostrarAviso(titulo, cuerpo, null);
    }

    private static void mostrarGrowl(String titulo, String cuerpo, NotificationType type) {
        String resolvedTitle = titulo != null ? Mensajes.getMensaje(titulo) : Constantes.NOMBRE_APP;
        try {
            notifier.show(resolvedTitle, cuerpo, type, notificationIcon);
        } catch (RuntimeException e) {
            Logger.error(e);
            JOptionPane.showMessageDialog(null,
                    cuerpo,
                    resolvedTitle,
                    type.optionPaneMessageType());
        }
    }

    public static void mostrarAviso(String titulo, String cuerpo, String[] parametros) {
        mostrarGrowl(titulo, Mensajes.getError(cuerpo, parametros), NotificationType.WARNING);
    }

    static void setNotifier(DesktopNotifier notifier) {
        Growls.notifier = notifier;
    }

    enum NotificationType {
        INFO(ToastType.INFO, JOptionPane.INFORMATION_MESSAGE),
        WARNING(ToastType.WARNING, JOptionPane.WARNING_MESSAGE),
        ERROR(ToastType.ERROR, JOptionPane.ERROR_MESSAGE);

        private final ToastType toastType;
        private final int optionPaneMessageType;

        NotificationType(ToastType toastType, int optionPaneMessageType) {
            this.toastType = toastType;
            this.optionPaneMessageType = optionPaneMessageType;
        }

        private ToastType toastType() {
            return toastType;
        }

        private int optionPaneMessageType() {
            return optionPaneMessageType;
        }
    }

    interface DesktopNotifier {
        void show(String title, String body, NotificationType type, URL icon);
    }

    private static class TwoSlicesDesktopNotifier implements DesktopNotifier {
        @Override
        public void show(String title, String body, NotificationType type, URL icon) {
            ToastBuilder builder = Toast.builder()
                    .type(type.toastType())
                    .title(title)
                    .content(body);
            if (icon != null) {
                builder.icon(icon);
            }
            builder.toast();
        }
    }
}
