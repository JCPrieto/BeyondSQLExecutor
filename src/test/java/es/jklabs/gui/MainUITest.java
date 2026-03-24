package es.jklabs.gui;

import es.jklabs.gui.panels.ScriptPanel;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.storage.FileSystemProjectStore;
import es.jklabs.utilidades.UtilidadesConfiguracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainUITest {

    private static <T> T allocateInstance(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        return type.cast(unsafe.allocateInstance(type));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object getStaticField(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static MainUI createMainUI(Configuracion configuracion, TrackingServersPanel serverPanel,
                                       TrackingScriptPanel scriptPanel) throws Exception {
        TrackingMainUI ui = allocateInstance(TrackingMainUI.class);
        setField(ui, "configuracion", configuracion);
        setField(ui, "serverPanel", serverPanel);
        setField(ui, "scriptPanel", scriptPanel);
        setField(ui, "splitPane", new JSplitPane(JSplitPane.HORIZONTAL_SPLIT));
        setField(ui, "jmArchivo", new JMenu("Archivo"));
        setField(ui, "jmAyuda", new JMenu("Ayuda"));
        return ui;
    }

    private static Servidor createServer(String name) {
        Servidor servidor = new Servidor();
        servidor.setName(name);
        servidor.setTipoServidor(TipoServidor.MYSQL);
        servidor.setHost(name.toLowerCase() + ".example.test");
        servidor.setPort(String.valueOf(3000 + name.length()));
        servidor.setUser(name.toLowerCase());
        return servidor;
    }

    @Test
    void refresSplitAjustaElMinimoYRefrescaScriptPanel() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        serverPanel.minimumSize = new Dimension(320, 0);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        MainUI ui = createMainUI(new Configuracion(), serverPanel, scriptPanel);
        JSplitPane splitPane = (JSplitPane) getField(ui, "splitPane");
        splitPane.setDividerLocation(120);

        ui.refresSplit();

        assertEquals(320, splitPane.getDividerLocation());
        assertTrue(scriptPanel.refresSplitCalled, "Expected ScriptPanel.refresSplit() to be called.");
    }

    @Test
    void refresSplitNoReduceElDivisorCuandoYaSuperaElMinimo() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        serverPanel.minimumSize = new Dimension(280, 0);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        MainUI ui = createMainUI(new Configuracion(), serverPanel, scriptPanel);
        JSplitPane splitPane = (JSplitPane) getField(ui, "splitPane");
        splitPane.setDividerLocation(450);

        ui.refresSplit();

        assertEquals(450, splitPane.getDividerLocation());
        assertTrue(scriptPanel.refresSplitCalled, "Expected ScriptPanel.refresSplit() to be called.");
    }

    @Test
    void bloquearPantallaDeshabilitaMenusYDelegaEnLosPaneles() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        TrackingMainUI ui = (TrackingMainUI) createMainUI(new Configuracion(), serverPanel, scriptPanel);
        JMenu jmArchivo = (JMenu) getField(ui, "jmArchivo");
        JMenu jmAyuda = (JMenu) getField(ui, "jmAyuda");

        ui.bloquearPantalla();

        assertEquals(Cursor.WAIT_CURSOR, ui.trackedCursor.getType());
        assertFalse(jmArchivo.isEnabled(), "Archivo should be disabled while the UI is blocked.");
        assertFalse(jmAyuda.isEnabled(), "Ayuda should be disabled while the UI is blocked.");
        assertTrue(serverPanel.bloquearCalled, "Expected ServersPanel.bloquearPantalla() to be called.");
        assertTrue(scriptPanel.bloquearCalled, "Expected ScriptPanel.bloquearPantalla() to be called.");
        assertSame(ui.trackedCursor, scriptPanel.receivedCursor);
        assertEquals(WindowConstants.DO_NOTHING_ON_CLOSE, ui.trackedCloseOperation);
    }

    @Test
    void desbloquearPantallaHabilitaMenusYDelegaEnLosPaneles() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        TrackingMainUI ui = (TrackingMainUI) createMainUI(new Configuracion(), serverPanel, scriptPanel);
        JMenu jmArchivo = (JMenu) getField(ui, "jmArchivo");
        JMenu jmAyuda = (JMenu) getField(ui, "jmAyuda");
        jmArchivo.setEnabled(false);
        jmAyuda.setEnabled(false);

        ui.desbloquearPantalla();

        assertNull(ui.trackedCursor, "Expected wait cursor to be cleared.");
        assertTrue(jmArchivo.isEnabled(), "Archivo should be enabled after unlocking the UI.");
        assertTrue(jmAyuda.isEnabled(), "Ayuda should be enabled after unlocking the UI.");
        assertTrue(serverPanel.desbloquearCalled, "Expected ServersPanel.desbloquearPantalla() to be called.");
        assertTrue(scriptPanel.desbloquearCalled, "Expected ScriptPanel.desbloquearPantalla() to be called.");
        assertEquals(WindowConstants.EXIT_ON_CLOSE, ui.trackedCloseOperation);
    }

    @Test
    void actualizarServidorDelegaEnServersPanel() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        MainUI ui = createMainUI(new Configuracion(), serverPanel, scriptPanel);
        Servidor servidor = createServer("Servidor 1");

        ui.actualizarServidor(servidor);

        assertSame(servidor, serverPanel.updatedServer);
    }

    @Test
    void setEditableDelegaEnServersPanel() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        MainUI ui = createMainUI(new Configuracion(), serverPanel, scriptPanel);
        ServerItem servidor = new ServerItem(null, createServer("Servidor editable"));

        ui.setEditable(servidor);

        assertSame(servidor, serverPanel.editableServer);
    }

    @Test
    void eliminarQuitaElServidorDeLaConfiguracionYPersisteElCambio(@TempDir Path tempDir) throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        Servidor eliminado = createServer("Eliminar");
        Servidor restante = createServer("Mantener");
        Configuracion configuracion = new Configuracion();
        configuracion.setServers(new ArrayList<>(List.of(eliminado, restante)));
        MainUI ui = createMainUI(configuracion, serverPanel, scriptPanel);
        ServerItem serverItem = new ServerItem(null, eliminado);

        Object previousProjectStore = getStaticField(UtilidadesConfiguracion.class, "projectStore");
        try {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", new FileSystemProjectStore(tempDir));

            ui.eliminar(serverItem);

            assertFalse(configuracion.getServers().contains(eliminado), "Expected removed server to disappear from config.");
            assertTrue(configuracion.getServers().contains(restante), "Expected remaining server to stay in config.");
            assertSame(serverItem, serverPanel.removedServer);
            Path persisted = tempDir.resolve("connections.json");
            assertTrue(Files.exists(persisted), "Expected configuration to be saved.");
            String json = Files.readString(persisted);
            assertFalse(json.contains("Eliminar"), "Removed server should not be persisted.");
            assertTrue(json.contains("Mantener"), "Remaining server should be persisted.");
        } finally {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", previousProjectStore);
        }
    }

    private static class TrackingMainUI extends MainUI {
        private Cursor trackedCursor;
        private int trackedCloseOperation;

        private TrackingMainUI() {
            super(new Configuracion());
        }

        @Override
        public void setCursor(Cursor cursor) {
            this.trackedCursor = cursor;
        }

        @Override
        public void setDefaultCloseOperation(int operation) {
            this.trackedCloseOperation = operation;
        }
    }

    private static class TrackingServersPanel extends ServersPanel {
        private Dimension minimumSize = new Dimension(0, 0);
        private boolean bloquearCalled;
        private boolean desbloquearCalled;
        private Servidor updatedServer;
        private ServerItem editableServer;
        private ServerItem removedServer;

        private TrackingServersPanel() {
            super(null);
        }

        @Override
        public Dimension getMinimumSize() {
            return minimumSize;
        }

        @Override
        public void bloquearPantalla() {
            bloquearCalled = true;
        }

        @Override
        public void desbloquearPantalla() {
            desbloquearCalled = true;
        }

        @Override
        public void actualizarServidor(Servidor servidor) {
            updatedServer = servidor;
        }

        @Override
        public void eliminar(ServerItem servidor) {
            removedServer = servidor;
        }

        @Override
        public void setEditable(ServerItem servidor) {
            editableServer = servidor;
        }
    }

    private static class TrackingScriptPanel extends ScriptPanel {
        private boolean refresSplitCalled;
        private boolean bloquearCalled;
        private boolean desbloquearCalled;
        private Cursor receivedCursor;

        private TrackingScriptPanel() {
            super(null);
        }

        @Override
        public void refresSplit() {
            refresSplitCalled = true;
        }

        @Override
        public void bloquearPantalla(Cursor waitCursor) {
            bloquearCalled = true;
            receivedCursor = waitCursor;
        }

        @Override
        public void desbloquearPantalla() {
            desbloquearCalled = true;
        }
    }

}
