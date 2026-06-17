package es.jklabs.gui.utilidades;

import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Mensajes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrowlsTest {

    private final TrackingNotifier notifier = new TrackingNotifier();

    @AfterEach
    void tearDown() {
        Growls.setNotifier((title, body, type, icon) -> {
        });
    }

    @Test
    void mostrarInfoDelegatesToNotifier() {
        Growls.setNotifier(notifier);

        Growls.mostrarInfo("ejecucion.completada");

        assertEquals(Constantes.NOMBRE_APP, notifier.title);
        assertEquals(Mensajes.getMensaje("ejecucion.completada"), notifier.body);
        assertEquals(Growls.NotificationType.INFO, notifier.type);
    }

    @Test
    void mostrarAvisoDelegatesWarningToNotifier() {
        Growls.setNotifier(notifier);

        Growls.mostrarAviso("acerca.de", "nombre.servidor.vacio");

        assertEquals(Mensajes.getMensaje("acerca.de"), notifier.title);
        assertEquals(Mensajes.getError("nombre.servidor.vacio"), notifier.body);
        assertEquals(Growls.NotificationType.WARNING, notifier.type);
    }

    @Test
    void mostrarErrorDelegatesErrorToNotifier() {
        Growls.setNotifier(notifier);

        Growls.mostrarError("acerca.de", "abrir.enlace", new RuntimeException("error"));

        assertEquals(Mensajes.getMensaje("acerca.de"), notifier.title);
        assertEquals(Mensajes.getError("abrir.enlace"), notifier.body);
        assertEquals(Growls.NotificationType.ERROR, notifier.type);
    }

    private static class TrackingNotifier implements Growls.DesktopNotifier {
        private String title;
        private String body;
        private Growls.NotificationType type;
        private URL icon;

        @Override
        public void show(String title, String body, Growls.NotificationType type, URL icon) {
            this.title = title;
            this.body = body;
            this.type = type;
            this.icon = icon;
        }
    }
}
