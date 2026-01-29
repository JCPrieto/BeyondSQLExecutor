package es.jklabs.gui;

import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.json.configuracion.Configuracion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainUITest {

    private static Object getField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to access field: " + name, e);
        }
    }

    private static void runOnEdt(ThrowingRunnable runnable) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] != null) {
            if (failure[0] instanceof Exception e) {
                throw e;
            }
            if (failure[0] instanceof Error e) {
                throw e;
            }
            throw new RuntimeException(failure[0]);
        }
    }

    @Test
    void refresSplitAjustaMinimoCuandoEsPequeno() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "UI test requires non-headless mode.");
        runOnEdt(() -> {
            MainUI ui = new MainUI(new Configuracion());
            try {
                ui.setSize(1200, 800);
                ui.doLayout();
                JSplitPane splitPane = (JSplitPane) getField(ui, "splitPane");
                ServersPanel serverPanel = (ServersPanel) getField(ui, "serverPanel");
                int minWidth = serverPanel.getMinimumSize().width;
                splitPane.setDividerLocation(Math.max(0, minWidth - 50));

                ui.refresSplit();

                assertTrue(splitPane.getDividerLocation() >= minWidth,
                        "Divider should be at least the server panel minimum width.");
            } finally {
                ui.dispose();
            }
        });
    }

    @Test
    void refresSplitNoReduceCuandoYaEsMayor() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "UI test requires non-headless mode.");
        runOnEdt(() -> {
            MainUI ui = new MainUI(new Configuracion());
            try {
                ui.setSize(1200, 800);
                ui.doLayout();
                JSplitPane splitPane = (JSplitPane) getField(ui, "splitPane");
                ServersPanel serverPanel = (ServersPanel) getField(ui, "serverPanel");
                int minWidth = serverPanel.getMinimumSize().width;
                splitPane.setDividerLocation(minWidth + 120);
                int expected = splitPane.getDividerLocation();

                ui.refresSplit();

                assertEquals(expected, splitPane.getDividerLocation(),
                        "Divider should not be reduced if already above minimum.");
            } finally {
                ui.dispose();
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
