package es.jklabs.utilidades;

import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UtilidadesBBDDTest {

    private static Servidor createServer(TipoServidor tipoServidor) {
        Servidor servidor = new Servidor();
        servidor.setTipoServidor(tipoServidor);
        servidor.setTipoLogin(TipoLogin.USUARIO_CONTRASENA);
        servidor.setUser("db-user");
        return servidor;
    }

    private static Connection createConnection(Statement statement) {
        return (Connection) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    return switch (name) {
                        case "createStatement" -> statement;
                        case "close" -> null;
                        case "toString" -> "TestConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException("Unexpected call: " + name);
                    };
                }
        );
    }

    private static Statement createStatement(boolean hasResultSet, ResultSet resultSet, AtomicBoolean getResultSetCalled) {
        return (Statement) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{Statement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "execute" -> {
                            return hasResultSet;
                        }
                        case "getResultSet" -> {
                            getResultSetCalled.set(true);
                            if (resultSet == null) {
                                throw new IllegalStateException("ResultSet not configured");
                            }
                            return resultSet;
                        }
                        case "close" -> {
                            return null;
                        }
                        case "toString" -> {
                            return "TestStatement";
                        }
                        case "hashCode" -> {
                            return System.identityHashCode(proxy);
                        }
                        case "equals" -> {
                            return proxy == args[0];
                        }
                    }
                    throw new UnsupportedOperationException("Unexpected call: " + name);
                }
        );
    }

    private static ResultSet createResultSet(List<String> columns, List<Object[]> rows) {
        AtomicInteger index = new AtomicInteger(-1);
        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    return switch (name) {
                        case "getColumnCount" -> columns.size();
                        case "getColumnName" -> columns.get(((Integer) args[0]) - 1);
                        case "toString" -> "TestResultSetMetaData";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException("Unexpected call: " + name);
                    };
                }
        );

        return (ResultSet) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "getMetaData" -> {
                            return metaData;
                        }
                        case "next" -> {
                            int nextIndex = index.incrementAndGet();
                            return nextIndex < rows.size();
                        }
                        case "getObject" -> {
                            Object[] row = rows.get(index.get());
                            return row[((Integer) args[0]) - 1];
                        }
                        case "close" -> {
                            return null;
                        }
                        case "toString" -> {
                            return "TestResultSet";
                        }
                        case "hashCode" -> {
                            return System.identityHashCode(proxy);
                        }
                        case "equals" -> {
                            return proxy == args[0];
                        }
                    }
                    throw new UnsupportedOperationException("Unexpected call: " + name);
                }
        );
    }

    @Test
    void postgresConnectionPropertiesIdentifyApplicationAndVersion() {
        Servidor servidor = createServer(TipoServidor.POSTGRESQL);

        Properties properties = UtilidadesBBDD.getConnectionProperties(servidor, "secret");

        assertEquals(Constantes.NOMBRE_APP + " " + Constantes.VERSION,
                properties.getProperty("ApplicationName"));
        assertEquals("db-user", properties.getProperty("user"));
        assertEquals("secret", properties.getProperty("password"));
    }

    @Test
    void nonPostgresConnectionPropertiesDoNotSetApplicationName() {
        Servidor servidor = createServer(TipoServidor.MYSQL);

        Properties properties = UtilidadesBBDD.getConnectionProperties(servidor, "secret");

        assertFalse(properties.containsKey("ApplicationName"));
    }

    @Test
    void executeAnyReturnsNullWhenNoResultSet() throws Exception {
        AtomicBoolean getResultSetCalled = new AtomicBoolean(false);
        Statement statement = createStatement(false, null, getResultSetCalled);
        Connection connection = createConnection(statement);

        Map.Entry<List<String>, List<Object[]>> result = UtilidadesBBDD.executeAny(connection, "update test");

        assertNull(result, "Expected null when Statement.execute returns false.");
        assertFalse(getResultSetCalled.get(), "getResultSet should not be called.");
    }

    @Test
    void executeAnyReadsResultSetWhenPresent() throws Exception {
        ResultSet resultSet = createResultSet(
                List.of("id", "name"),
                List.of(new Object[]{1, "alpha"}, new Object[]{2, "beta"})
        );
        Statement statement = createStatement(true, resultSet, new AtomicBoolean(false));
        Connection connection = createConnection(statement);

        Map.Entry<List<String>, List<Object[]>> result = UtilidadesBBDD.executeAny(connection, "select * from test");

        if (result != null) {
            assertEquals(List.of("id", "name"), result.getKey());
            assertEquals(2, result.getValue().size());
            assertEquals(1, result.getValue().get(0)[0]);
            assertEquals("alpha", result.getValue().get(0)[1]);
            assertEquals(2, result.getValue().get(1)[0]);
            assertEquals("beta", result.getValue().get(1)[1]);
        }
    }
}
