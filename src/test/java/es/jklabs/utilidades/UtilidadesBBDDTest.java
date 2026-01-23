package es.jklabs.utilidades;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UtilidadesBBDDTest {

    private static Connection createConnection(Statement statement) {
        return (Connection) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("createStatement".equals(name)) {
                        return statement;
                    }
                    if ("close".equals(name)) {
                        return null;
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

    private static Statement createStatement(boolean hasResultSet, ResultSet resultSet, AtomicBoolean getResultSetCalled) {
        return (Statement) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{Statement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("execute".equals(name)) {
                        return hasResultSet;
                    }
                    if ("getResultSet".equals(name)) {
                        getResultSetCalled.set(true);
                        if (resultSet == null) {
                            throw new IllegalStateException("ResultSet not configured");
                        }
                        return resultSet;
                    }
                    if ("close".equals(name)) {
                        return null;
                    }
                    if ("toString".equals(name)) {
                        return "TestStatement";
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

    private static ResultSet createResultSet(List<String> columns, List<Object[]> rows) {
        AtomicInteger index = new AtomicInteger(-1);
        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getColumnCount".equals(name)) {
                        return columns.size();
                    }
                    if ("getColumnName".equals(name)) {
                        return columns.get(((Integer) args[0]) - 1);
                    }
                    if ("toString".equals(name)) {
                        return "TestResultSetMetaData";
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

        return (ResultSet) Proxy.newProxyInstance(
                UtilidadesBBDDTest.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getMetaData".equals(name)) {
                        return metaData;
                    }
                    if ("next".equals(name)) {
                        int nextIndex = index.incrementAndGet();
                        return nextIndex < rows.size();
                    }
                    if ("getObject".equals(name)) {
                        Object[] row = rows.get(index.get());
                        return row[((Integer) args[0]) - 1];
                    }
                    if ("close".equals(name)) {
                        return null;
                    }
                    if ("toString".equals(name)) {
                        return "TestResultSet";
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
    void executeAnyReturnsNullWhenNoResultSet() throws Exception {
        AtomicBoolean getResultSetCalled = new AtomicBoolean(false);
        Statement statement = createStatement(false, null, getResultSetCalled);
        Connection connection = createConnection(statement);

        Map.Entry<List<String>, List<Object[]>> result = UtilidadesBBDD.executeAny(connection, "update test");

        assertNull(result, "Expected null when Statement.execute returns false.");
        assertEquals(false, getResultSetCalled.get(), "getResultSet should not be called.");
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

        assertEquals(List.of("id", "name"), result.getKey());
        assertEquals(2, result.getValue().size());
        assertEquals(1, result.getValue().get(0)[0]);
        assertEquals("alpha", result.getValue().get(0)[1]);
        assertEquals(2, result.getValue().get(1)[0]);
        assertEquals("beta", result.getValue().get(1)[1]);
    }
}
