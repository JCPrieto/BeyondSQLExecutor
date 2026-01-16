package es.jklabs.gui.panels;

import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void closeAllConnectionsClosesAllServerItems() throws Exception {
        ServersPanel serversPanel = allocateInstance(ServersPanel.class);
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
        setField(serversPanel, "panelServidores", panelServidores);

        serversPanel.closeAllConnections();

        assertTrue(closed1.get(), "Expected first connection to be closed.");
        assertTrue(closed2.get(), "Expected second connection to be closed.");
        assertNull(serverItem1.getDatabaseConnection(), "Expected connection reference to be cleared.");
        assertNull(serverItem2.getDatabaseConnection(), "Expected connection reference to be cleared.");
    }
}
