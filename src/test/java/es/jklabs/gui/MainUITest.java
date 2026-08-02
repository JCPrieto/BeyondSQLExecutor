package es.jklabs.gui;

import es.jklabs.gui.panels.ScriptPanel;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.gui.themes.model.EditorTheme;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.storage.FileSystemProjectStore;
import es.jklabs.utilidades.UtilidadesConfiguracion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

    private static int selectedItems(JMenu menu) {
        int selected = 0;
        for (Component component : menu.getMenuComponents()) {
            if (((AbstractButton) component).isSelected()) {
                selected++;
            }
        }
        return selected;
    }

    @Test
    void aplicarIconoIgnoraNullYAplicaUnaImagen() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        TrackingMainUI ui = (TrackingMainUI) createMainUI(new Configuracion(), serverPanel, scriptPanel);
        Image image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        ui.aplicarIcono(null);
        assertNull(ui.trackedIcon);

        ui.aplicarIcono(image);
        assertSame(image, ui.trackedIcon);
    }

    @Test
    void cargarMenuAparienciaSeleccionaSoloElThemeConfigurado() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        Configuracion configuracion = new Configuracion();
        MainUI ui = createMainUI(configuracion, serverPanel, scriptPanel);

        JMenu withoutSelection = ui.cargarMenuApariencia();
        assertEquals(EditorTheme.values().length, withoutSelection.getItemCount());
        assertEquals(0, selectedItems(withoutSelection));

        configuracion.setTheme(EditorTheme.IDEA);
        JMenu withSelection = ui.cargarMenuApariencia();
        assertEquals(1, selectedItems(withSelection));
        assertTrue(((AbstractButton) withSelection.getMenuComponent(EditorTheme.IDEA.ordinal())).isSelected());
    }

    @Test
    void aplicarThemeConfiguradoContemplaConfiguracionConYSinTheme(@TempDir Path tempDir) throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        scriptPanel.entrada = new RSyntaxTextArea();
        Configuracion configuracion = new Configuracion();
        MainUI ui = createMainUI(configuracion, serverPanel, scriptPanel);

        ui.aplicarThemeConfigurado();
        assertNull(configuracion.getTheme());

        Object previousProjectStore = getStaticField(UtilidadesConfiguracion.class, "projectStore");
        try {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", new FileSystemProjectStore(tempDir));
            configuracion.setTheme(EditorTheme.DEFAULT);
            ui.aplicarThemeConfigurado();
            ui.setTheme(EditorTheme.DARK);

            assertEquals(EditorTheme.DARK, configuracion.getTheme());
            assertTrue(Files.exists(tempDir.resolve("connections.json")));
        } finally {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", previousProjectStore);
        }
    }

    @Test
    void procesarComprobacionNuevaVersionContemplaErrorAusenciaYActualizacion() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        TrackingMainUI ui = (TrackingMainUI) createMainUI(new Configuracion(), serverPanel, scriptPanel);
        JMenuBar menu = new JMenuBar();
        IOException error = new IOException("sin red");

        ui.procesarComprobacionNuevaVersion(menu, error, false);
        assertSame(error, ui.trackedError);
        assertEquals(0, menu.getComponentCount());

        ui.procesarComprobacionNuevaVersion(menu, null, false);
        assertEquals(0, menu.getComponentCount());

        ui.procesarComprobacionNuevaVersion(menu, null, true);
        assertEquals(2, menu.getComponentCount());
        assertInstanceOf(JMenuItem.class, menu.getComponent(1));
    }

    @Test
    void procesarImportacionRespetaLaDecisionDelSelector() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        TrackingMainUI ui = (TrackingMainUI) createMainUI(new Configuracion(), serverPanel, scriptPanel);
        File selected = new File("connections.json");

        ui.procesarImportacion(JFileChooser.CANCEL_OPTION, selected);
        assertNull(ui.importedFile);

        ui.procesarImportacion(JFileChooser.APPROVE_OPTION, selected);
        assertSame(selected, ui.importedFile);
    }

    @Test
    void procesarExportacionRespetaDecisionExtensionYErrores() throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        TrackingMainUI ui = (TrackingMainUI) createMainUI(new Configuracion(), serverPanel, scriptPanel);

        ui.procesarExportacion(JFileChooser.CANCEL_OPTION, new File("cancelado"));
        assertNull(ui.exportedFile);

        ui.procesarExportacion(JFileChooser.APPROVE_OPTION, new File("proyecto"));
        assertEquals("proyecto.zip", ui.exportedFile.getPath());

        ui.procesarExportacion(JFileChooser.APPROVE_OPTION, new File("proyecto.zip"));
        assertEquals("proyecto.zip", ui.exportedFile.getPath());

        ui.exportError = new IOException("sin permisos");
        ui.procesarExportacion(JFileChooser.APPROVE_OPTION, new File("fallo.zip"));
        assertSame(ui.exportError, ui.trackedError);
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

    @Test
    void clonarCopiaTodosLosDatosPersisteYRefrescaLasConexiones(@TempDir Path tempDir) throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        Servidor original = createServer("Produccion");
        original.setDataBase("ventas");
        original.setCredentialRef("cred:produccion");
        original.setEsquemasExcluidos(new ArrayList<>(List.of("audit", "legacy")));
        Configuracion configuracion = new Configuracion();
        configuracion.getServers().add(original);
        MainUI ui = createMainUI(configuracion, serverPanel, scriptPanel);

        Object previousProjectStore = getStaticField(UtilidadesConfiguracion.class, "projectStore");
        try {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", new FileSystemProjectStore(tempDir));

            ui.clonar(new ServerItem(null, original));

            assertEquals(2, configuracion.getServers().size());
            Servidor copia = configuracion.getServers().get(1);
            assertNotSame(original, copia);
            assertNotEquals(original.getId(), copia.getId());
            assertEquals("Produccion - Copia", copia.getName());
            assertEquals(original.getTipoServidor(), copia.getTipoServidor());
            assertEquals(original.getHost(), copia.getHost());
            assertEquals(original.getPort(), copia.getPort());
            assertEquals(original.getDataBase(), copia.getDataBase());
            assertEquals(original.getUser(), copia.getUser());
            assertEquals(original.getCredentialRef(), copia.getCredentialRef());
            assertEquals(original.getEsquemasExcluidos(), copia.getEsquemasExcluidos());
            assertNotSame(original.getEsquemasExcluidos(), copia.getEsquemasExcluidos());
            assertSame(original, serverPanel.clonedOriginal.getServidor());
            assertSame(copia, serverPanel.clonedServer);
            assertTrue(Files.readString(tempDir.resolve("connections.json")).contains("Produccion - Copia"));
        } finally {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", previousProjectStore);
        }
    }

    @Test
    void eliminarQuitaLaCopiaSeleccionadaAunqueSeaIgualAOriginal(@TempDir Path tempDir) throws Exception {
        TrackingServersPanel serverPanel = allocateInstance(TrackingServersPanel.class);
        TrackingScriptPanel scriptPanel = allocateInstance(TrackingScriptPanel.class);
        Servidor original = createServer("Original");
        Servidor copia = new Servidor(original);
        copia.setName("Original - Copia");
        Configuracion configuracion = new Configuracion();
        configuracion.setServers(new ArrayList<>(List.of(original, copia)));
        MainUI ui = createMainUI(configuracion, serverPanel, scriptPanel);

        Object previousProjectStore = getStaticField(UtilidadesConfiguracion.class, "projectStore");
        try {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", new FileSystemProjectStore(tempDir));

            ui.eliminar(new ServerItem(null, copia));

            assertEquals(1, configuracion.getServers().size());
            assertSame(original, configuracion.getServers().getFirst());
        } finally {
            setStaticField(UtilidadesConfiguracion.class, "projectStore", previousProjectStore);
        }
    }

    private static class TrackingMainUI extends MainUI {
        private Cursor trackedCursor;
        private int trackedCloseOperation;
        private Image trackedIcon;
        private Exception trackedError;
        private File importedFile;
        private File exportedFile;
        private IOException exportError;

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

        @Override
        public void setIconImage(Image image) {
            trackedIcon = image;
        }

        @Override
        void mostrarError(String key, Exception error) {
            trackedError = error;
        }

        @Override
        void importarConfiguracion(File file) {
            importedFile = file;
        }

        @Override
        void guardarConfiguracion(File file) throws IOException {
            exportedFile = file;
            if (exportError != null) {
                throw exportError;
            }
        }
    }

    private static class TrackingServersPanel extends ServersPanel {
        private Dimension minimumSize = new Dimension(0, 0);
        private boolean bloquearCalled;
        private boolean desbloquearCalled;
        private Servidor updatedServer;
        private ServerItem editableServer;
        private ServerItem removedServer;
        private ServerItem clonedOriginal;
        private Servidor clonedServer;

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

        @Override
        public void clonar(ServerItem original, Servidor copia) {
            clonedOriginal = original;
            clonedServer = copia;
        }
    }

    private static class TrackingScriptPanel extends ScriptPanel {
        private boolean refresSplitCalled;
        private boolean bloquearCalled;
        private boolean desbloquearCalled;
        private Cursor receivedCursor;
        private RSyntaxTextArea entrada;

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

        @Override
        public RSyntaxTextArea getEntrada() {
            return entrada;
        }
    }

}
