package es.jklabs.gui.thread;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LoadSchemaWorkerTest {

    private static void assertSchemaExists(ServerItem serverItem, String schema) {
        JCheckBox checkBox = serverItem.getEsquemas().get(schema);
        assertNotNull(checkBox);
        assertEquals(schema, checkBox.getText());
    }

    private static LoadSchemaWorker runWorker(ServerItem serverItem,
                                              LoadSchemaWorker.SchemaLoader schemaLoader,
                                              LoadSchemaWorker.ErrorNotifier errorNotifier) throws Exception {
        LoadSchemaWorker worker = new LoadSchemaWorker(serverItem, schemaLoader, errorNotifier);
        CountDownLatch latch = new CountDownLatch(1);
        worker.setOnDone(latch::countDown);
        worker.run();
        SwingUtilities.invokeAndWait(() -> {
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return worker;
    }

    private static ServerItem createServerItem(List<String> excludedSchemas) {
        Servidor servidor = new Servidor();
        servidor.setName("Test server");
        servidor.setTipoServidor(TipoServidor.POSTGRESQL);
        servidor.setTipoLogin(TipoLogin.USUARIO_CONTRASENA);
        servidor.setHost("localhost");
        servidor.setPort("5432");
        servidor.setEsquemasExcluidos(excludedSchemas);
        return new ServerItem(null, servidor);
    }

    private static Exception getError(LoadSchemaWorker worker) throws Exception {
        Field errorField = LoadSchemaWorker.class.getDeclaredField("error");
        errorField.setAccessible(true);
        return (Exception) errorField.get(worker);
    }

    private static Connection createConnectionProxy() {
        return (Connection) Proxy.newProxyInstance(
                LoadSchemaWorkerTest.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "test-connection";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "close" -> null;
                    case "isClosed" -> false;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void loadsSchemasFiltersExcludedAndUpdatesUi() throws Exception {
        ServerItem serverItem = createServerItem(List.of("sys", "pg_*"));
        Connection connection = createConnectionProxy();
        TestSchemaLoader schemaLoader = new TestSchemaLoader(connection, List.of("public", "sys", "pg_catalog", "custom"));
        TestErrorNotifier errorNotifier = new TestErrorNotifier();

        LoadSchemaWorker worker = runWorker(serverItem, schemaLoader, errorNotifier);

        assertSame(connection, serverItem.getDatabaseConnection());
        assertEquals(2, serverItem.getEsquemas().size());
        assertSchemaExists(serverItem, "public");
        assertSchemaExists(serverItem, "custom");
        assertNull(serverItem.getEsquemas().get("sys"));
        assertNull(serverItem.getEsquemas().get("pg_catalog"));
        assertEquals(new Dimension(200, (250 / 9) * 2), serverItem.getScrollEsquemas().getPreferredSize());
        assertNull(getError(worker));
        assertNull(errorNotifier.error);
    }

    @Test
    void returnsEmptyWhenConnectionCannotBeCreated() throws Exception {
        ServerItem serverItem = createServerItem(List.of());
        TestSchemaLoader schemaLoader = new TestSchemaLoader(null, List.of("public"));
        TestErrorNotifier errorNotifier = new TestErrorNotifier();

        LoadSchemaWorker worker = runWorker(serverItem, schemaLoader, errorNotifier);

        assertTrue(serverItem.getEsquemas().isEmpty());
        assertEquals(new Dimension(200, 0), serverItem.getScrollEsquemas().getPreferredSize());
        assertNull(getError(worker));
        assertNull(errorNotifier.error);
    }

    @Test
    void storesErrorAndNotifiesWhenSchemaLoadingFails() throws Exception {
        ServerItem serverItem = createServerItem(List.of());
        SQLException sqlException = new SQLException("boom");
        TestSchemaLoader schemaLoader = new TestSchemaLoader(createConnectionProxy(), sqlException);
        TestErrorNotifier errorNotifier = new TestErrorNotifier();

        LoadSchemaWorker worker = runWorker(serverItem, schemaLoader, errorNotifier);

        assertTrue(serverItem.getEsquemas().isEmpty());
        assertSame(sqlException, getError(worker));
        assertSame(sqlException, errorNotifier.error);
        assertSame(serverItem.getServidor(), errorNotifier.servidor);
    }

    private static final class TestSchemaLoader implements LoadSchemaWorker.SchemaLoader {
        private final Connection connection;
        private final List<String> schemas;
        private final SQLException schemaError;

        private TestSchemaLoader(Connection connection, List<String> schemas) {
            this.connection = connection;
            this.schemas = schemas;
            this.schemaError = null;
        }

        private TestSchemaLoader(Connection connection, SQLException schemaError) {
            this.connection = connection;
            this.schemas = List.of();
            this.schemaError = schemaError;
        }

        @Override
        public Connection getConnection(Servidor servidor) {
            return connection;
        }

        @Override
        public List<String> getSchemas(Connection connection) throws SQLException {
            if (schemaError != null) {
                throw schemaError;
            }
            return schemas;
        }
    }

    private static final class TestErrorNotifier implements LoadSchemaWorker.ErrorNotifier {
        private Servidor servidor;
        private Exception error;

        @Override
        public void showSchemaLoadError(Servidor servidor, Exception error) {
            this.servidor = servidor;
            this.error = error;
        }
    }
}
