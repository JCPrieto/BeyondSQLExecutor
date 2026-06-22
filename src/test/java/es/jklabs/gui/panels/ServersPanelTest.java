package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.thread.LoadSchemaWorker;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ServersPanelTest {

    private static Servidor crearServidor(String name) {
        Servidor servidor = new Servidor();
        servidor.setTipoServidor(TipoServidor.MYSQL);
        servidor.setName(name);
        return servidor;
    }

    private static Connection createTrackingConnection(AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                ServersPanelTest.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("close".equals(name)) {
                        closed.set(true);
                        return null;
                    }
                    if ("isClosed".equals(name)) {
                        return closed.get();
                    }
                    if ("toString".equals(name)) {
                        return "TestConnection";
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException("Unexpected call: " + name);
                }
        );
    }

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

    private static ServersPanel createPanel(JPanel panelServidores) throws Exception {
        ServersPanel serversPanel = allocateInstance(ServersPanel.class);
        setField(serversPanel, "panelServidores", panelServidores);
        setField(serversPanel, "btnAddServer", new JButton("add"));
        return serversPanel;
    }

    private static TrackingMainUI createMainUI(Configuracion configuracion) throws Exception {
        TrackingMainUI mainUI = allocateInstance(TrackingMainUI.class);
        setField(mainUI, "configuracion", configuracion);
        mainUI.refresSplitLatch = new CountDownLatch(1);
        return mainUI;
    }

    private static int invokeCalcularAnchoMinimoPanel(ServersPanel serversPanel, JScrollPane scrollPane)
            throws Exception {
        Method method = ServersPanel.class.getDeclaredMethod("calcularAnchoMinimoPanel", JScrollPane.class);
        method.setAccessible(true);
        return (int) method.invoke(serversPanel, scrollPane);
    }

    @Test
    void closeAllConnectionsClosesAllServerItems() throws Exception {
        JPanel panelServidores = new JPanel();
        panelServidores.setLayout(new BoxLayout(panelServidores, BoxLayout.Y_AXIS));

        ServerItem serverItem1 = new ServerItem(null, crearServidor("one"));
        AtomicBoolean closed1 = new AtomicBoolean(false);
        serverItem1.setDatabaseConnection(createTrackingConnection(closed1));

        ServerItem serverItem2 = new ServerItem(null, crearServidor("two"));
        AtomicBoolean closed2 = new AtomicBoolean(false);
        serverItem2.setDatabaseConnection(createTrackingConnection(closed2));

        panelServidores.add(serverItem1);
        panelServidores.add(serverItem2);
        ServersPanel serversPanel = createPanel(panelServidores);

        serversPanel.closeAllConnections();

        assertTrue(closed1.get(), "Expected first connection to be closed.");
        assertTrue(closed2.get(), "Expected second connection to be closed.");
        assertNull(serverItem1.getDatabaseConnection(), "Expected connection reference to be cleared.");
        assertNull(serverItem2.getDatabaseConnection(), "Expected connection reference to be cleared.");
    }

    @Test
    void constructorOrdenaServidoresPorNombre() throws Exception {
        Configuracion configuracion = new Configuracion();
        configuracion.setServers(new ArrayList<>(List.of(crearServidor("zeta"), crearServidor("alpha"))));
        ServersPanel serversPanel = new ServersPanel(createMainUI(configuracion));

        Component[] components = serversPanel.getPanelServidores().getComponents();

        assertEquals(2, components.length);
        assertEquals("alpha", ((ServerItem) components[0]).getServidor().getName());
        assertEquals("zeta", ((ServerItem) components[1]).getServidor().getName());
    }

    @Test
    void actualizarServidorAgregaNuevoServidorCuandoNoHayEditable() throws Exception {
        JPanel panelServidores = new JPanel();
        ServersPanel serversPanel = createPanel(panelServidores);
        Servidor servidor = crearServidor("new");

        serversPanel.actualizarServidor(servidor);

        assertEquals(1, panelServidores.getComponentCount());
        assertSame(servidor, ((ServerItem) panelServidores.getComponent(0)).getServidor());
    }

    @Test
    void actualizarServidorActualizaEditableYLimpiaReferencia() throws Exception {
        JPanel panelServidores = new JPanel();
        ServersPanel serversPanel = createPanel(panelServidores);
        TrackingServerItem editable = new TrackingServerItem(crearServidor("old"));
        panelServidores.add(editable);
        setField(serversPanel, "serverItemEditable", editable);
        Servidor actualizado = crearServidor("updated");

        serversPanel.actualizarServidor(actualizado);

        assertEquals(1, panelServidores.getComponentCount());
        assertSame(actualizado, editable.getServidor());
        assertTrue(editable.loadEsquemasCalled, "Expected schemas to be reloaded after editing.");
        assertNull(getField(serversPanel, "serverItemEditable"), "Expected editable reference to be cleared.");
    }

    @Test
    void eliminarQuitaSoloServidorPresenteYCierraConexion() throws Exception {
        JPanel panelServidores = new JPanel();
        TrackingServerItem removed = new TrackingServerItem(crearServidor("remove"));
        TrackingServerItem remaining = new TrackingServerItem(crearServidor("keep"));
        JLabel ignored = new JLabel("ignored");
        panelServidores.add(ignored);
        panelServidores.add(removed);
        panelServidores.add(remaining);
        ServersPanel serversPanel = createPanel(panelServidores);

        serversPanel.eliminar(removed);

        assertTrue(removed.closeConnectionCalled, "Expected removed server connection to be closed.");
        assertEquals(2, panelServidores.getComponentCount());
        assertSame(ignored, panelServidores.getComponent(0));
        assertSame(remaining, panelServidores.getComponent(1));
    }

    @Test
    void eliminarCierraConexionAunqueServidorNoEsteEnPanel() throws Exception {
        JPanel panelServidores = new JPanel();
        TrackingServerItem existing = new TrackingServerItem(crearServidor("existing"));
        TrackingServerItem missing = new TrackingServerItem(crearServidor("missing"));
        panelServidores.add(existing);
        ServersPanel serversPanel = createPanel(panelServidores);

        serversPanel.eliminar(missing);

        assertTrue(missing.closeConnectionCalled, "Expected requested server connection to be closed.");
        assertEquals(1, panelServidores.getComponentCount());
        assertSame(existing, panelServidores.getComponent(0));
    }

    @Test
    void loadEsquemasRefrescaSplitSiNoHayServidores() throws Exception {
        TrackingMainUI mainUI = createMainUI(new Configuracion());
        ServersPanel serversPanel = createPanel(new JPanel());
        setField(serversPanel, "mainUI", mainUI);

        serversPanel.loadEsquemas();

        assertEquals(1, mainUI.refresSplitCalls);
    }

    @Test
    void loadEsquemasRefrescaSplitCuandoTerminanTodosLosWorkers() throws Exception {
        TrackingMainUI mainUI = createMainUI(new Configuracion());
        JPanel panelServidores = new JPanel();
        TrackingServerItem first = new TrackingServerItem(crearServidor("first"));
        TrackingServerItem second = new TrackingServerItem(crearServidor("second"));
        panelServidores.add(first);
        panelServidores.add(new JLabel("ignored"));
        panelServidores.add(second);
        ServersPanel serversPanel = createPanel(panelServidores);
        setField(serversPanel, "mainUI", mainUI);

        serversPanel.loadEsquemas();

        assertTrue(mainUI.refresSplitLatch.await(3, TimeUnit.SECONDS), "Expected refresh after workers finish.");
        assertEquals(1, mainUI.refresSplitCalls);
        assertEquals(1, first.workerRequests.get(), "Expected a worker for the first server.");
        assertEquals(1, second.workerRequests.get(), "Expected a worker for the second server.");
    }

    @Test
    void bloquearYDesbloquearPantallaActualizanServidoresYBoton() throws Exception {
        JPanel panelServidores = new JPanel();
        TrackingServerItem serverItem = new TrackingServerItem(crearServidor("server"));
        panelServidores.add(new JLabel("ignored"));
        panelServidores.add(serverItem);
        ServersPanel serversPanel = createPanel(panelServidores);
        JButton btnAddServer = (JButton) getField(serversPanel, "btnAddServer");

        serversPanel.bloquearPantalla();

        assertFalse(btnAddServer.isEnabled(), "Expected add button to be disabled while blocked.");
        assertTrue(serverItem.bloquearCalled, "Expected server item to be blocked.");

        serversPanel.desbloquearPantalla();

        assertTrue(btnAddServer.isEnabled(), "Expected add button to be enabled after unlock.");
        assertTrue(serverItem.desbloquearCalled, "Expected server item to be unlocked.");
    }

    @Test
    void refrescarReemplazaServidoresOrdenadosYCargaEsquemas() throws Exception {
        Configuracion configuracion = new Configuracion();
        configuracion.setServers(new ArrayList<>(List.of(crearServidor("bravo"), crearServidor("alpha"))));
        TrackingMainUI mainUI = createMainUI(configuracion);
        ServersPanel serversPanel = new ServersPanel(mainUI);

        configuracion.setServers(new ArrayList<>(List.of(crearServidor("delta"), crearServidor("charlie"))));
        serversPanel.refrescar(configuracion);

        Component[] components = serversPanel.getPanelServidores().getComponents();
        assertEquals(2, components.length);
        assertEquals("charlie", ((ServerItem) components[0]).getServidor().getName());
        assertEquals("delta", ((ServerItem) components[1]).getServidor().getName());
    }

    @Test
    void calcularAnchoMinimoUsaAnchoDelBotonYValoresPorDefecto() throws Exception {
        ServersPanel serversPanel = createPanel(new JPanel());
        JButton wideButton = new JButton("very wide add server button");
        setField(serversPanel, "btnAddServer", wideButton);
        JScrollPane scrollPane = new JScrollPane(new JPanel());
        scrollPane.setBorder(null);
        Object previousWidth = UIManager.get("ScrollBar.width");
        try {
            UIManager.put("ScrollBar.width", 0);

            int width = invokeCalcularAnchoMinimoPanel(serversPanel, scrollPane);

            assertTrue(width >= wideButton.getPreferredSize().width,
                    "Expected minimum width to account for the add button.");
        } finally {
            UIManager.put("ScrollBar.width", previousWidth);
        }
    }

    @Test
    void calcularAnchoMinimoPermiteBotonAddNoInicializado() throws Exception {
        ServersPanel serversPanel = createPanel(new JPanel());
        setField(serversPanel, "btnAddServer", null);
        JScrollPane scrollPane = new JScrollPane(new JPanel());

        int width = invokeCalcularAnchoMinimoPanel(serversPanel, scrollPane);

        assertTrue(width > 0);
    }

    @Test
    void calcularAnchoMinimoSumaBordeDelScrollPane() throws Exception {
        ServersPanel serversPanel = createPanel(new JPanel());
        JScrollPane withoutBorder = new JScrollPane(new JPanel());
        withoutBorder.setBorder(null);
        JScrollPane withBorder = new JScrollPane(new JPanel());
        withBorder.setBorder(new EmptyBorder(0, 7, 0, 11));

        int widthWithoutBorder = invokeCalcularAnchoMinimoPanel(serversPanel, withoutBorder);
        int widthWithBorder = invokeCalcularAnchoMinimoPanel(serversPanel, withBorder);

        assertEquals(18, widthWithBorder - widthWithoutBorder);
    }

    private static class TrackingMainUI extends MainUI {
        private int refresSplitCalls;
        private CountDownLatch refresSplitLatch;

        private TrackingMainUI() {
            super(new Configuracion());
        }

        @Override
        public void refresSplit() {
            refresSplitCalls++;
            if (refresSplitLatch != null) {
                refresSplitLatch.countDown();
            }
        }
    }

    private static class TrackingServerItem extends ServerItem {
        private final AtomicInteger workerRequests = new AtomicInteger();
        private boolean loadEsquemasCalled;
        private boolean closeConnectionCalled;
        private boolean bloquearCalled;
        private boolean desbloquearCalled;

        private TrackingServerItem(Servidor servidor) {
            super(null, servidor);
        }

        @Override
        public void loadEsquemas() {
            loadEsquemasCalled = true;
        }

        @Override
        public void closeConnection() {
            closeConnectionCalled = true;
            super.closeConnection();
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
        public LoadSchemaWorker getLoadSchemaWorker() {
            workerRequests.incrementAndGet();
            return new EmptyLoadSchemaWorker(this);
        }
    }

    private static class EmptyLoadSchemaWorker extends LoadSchemaWorker {
        private EmptyLoadSchemaWorker(ServerItem serverItem) {
            super(serverItem);
        }

        @Override
        protected List<String> doInBackground() {
            return List.of();
        }
    }
}
