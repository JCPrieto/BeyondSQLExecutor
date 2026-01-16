package es.jklabs.gui.panels;

import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ServerItemTest {

    private static Servidor crearServidor() {
        Servidor servidor = new Servidor();
        servidor.setTipoServidor(TipoServidor.MYSQL);
        servidor.setName("test");
        return servidor;
    }

    private static Connection createTrackingConnection(AtomicBoolean closed, boolean throwOnClose) {
        return (Connection) Proxy.newProxyInstance(
                ServerItemTest.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("close".equals(name)) {
                        if (throwOnClose) {
                            throw new SQLException("close failed");
                        }
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

    @Test
    void closeConnectionClosesAndClearsReference() {
        ServerItem serverItem = new ServerItem(null, crearServidor());
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = createTrackingConnection(closed, false);
        serverItem.setDatabaseConnection(connection);

        serverItem.closeConnection();

        assertTrue(closed.get(), "Expected connection.close() to be called.");
        assertNull(serverItem.getDatabaseConnection(), "Expected connection reference to be cleared.");
    }

    @Test
    void closeConnectionSwallowsCloseErrors() {
        ServerItem serverItem = new ServerItem(null, crearServidor());
        Connection connection = createTrackingConnection(new AtomicBoolean(false), true);
        serverItem.setDatabaseConnection(connection);

        assertDoesNotThrow(serverItem::closeConnection);
        assertNull(serverItem.getDatabaseConnection(), "Expected connection reference to be cleared on error.");
    }
}
