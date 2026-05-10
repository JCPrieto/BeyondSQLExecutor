package es.jklabs.gui.utilidades.worker;

import es.jklabs.gui.panels.ScriptPanel;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlExecutorTest {

    private static SqlExecutor executor(RecordingScriptPanel scriptPanel,
                                        RecordingDatabaseExecutor databaseExecutor,
                                        Component[] components) {
        return executor(scriptPanel, databaseExecutor, new RecordingConnectionErrorNotifier(),
                prompt(JOptionPane.YES_OPTION), components);
    }

    private static SqlExecutor executor(RecordingScriptPanel scriptPanel,
                                        RecordingDatabaseExecutor databaseExecutor,
                                        RecordingConnectionErrorNotifier connectionErrorNotifier,
                                        RecordingSqlErrorPrompt sqlErrorPrompt,
                                        Component[] components) {
        return new SqlExecutor(scriptPanel, List.of("select mysql"), 10, List.of("select postgres"), 10,
                () -> components,
                databaseExecutor,
                connectionErrorNotifier,
                sqlErrorPrompt,
                () -> {
                },
                () -> {
                }
        );
    }

    @SafeVarargs
    private static ServerItem serverItem(String name,
                                         TipoServidor tipoServidor,
                                         Connection connection,
                                         Map.Entry<String, JCheckBox>... schemas) {
        Servidor servidor = new Servidor();
        servidor.setName(name);
        servidor.setTipoServidor(tipoServidor);
        ServerItem serverItem = new ServerItem(null, servidor);
        serverItem.setDatabaseConnection(connection);
        for (Map.Entry<String, JCheckBox> schema : schemas) {
            serverItem.getEsquemas().put(schema.getKey(), schema.getValue());
        }
        return serverItem;
    }

    private static Map.Entry<String, JCheckBox> schema(String name, boolean selected) {
        JCheckBox checkBox = new JCheckBox(name);
        checkBox.setSelected(selected);
        return new AbstractMap.SimpleEntry<>(name, checkBox);
    }

    private static Map.Entry<List<String>, List<Object[]>> result(Object[] row) {
        return new AbstractMap.SimpleEntry<>(List.of("col"), List.<Object[]>of(row));
    }

    private static Connection connection(String... failingMethods) {
        TestConnection testConnection = new TestConnection(List.of(failingMethods));
        return (Connection) Proxy.newProxyInstance(
                SqlExecutorTest.class.getClassLoader(),
                new Class[]{Connection.class},
                testConnection
        );
    }

    private static List<String> catalogs(Connection connection) {
        return ((TestConnection) Proxy.getInvocationHandler(connection)).catalogs;
    }

    private static List<String> schemas(Connection connection) {
        return ((TestConnection) Proxy.getInvocationHandler(connection)).schemas;
    }

    private static RecordingSqlErrorPrompt prompt(int response) {
        return new RecordingSqlErrorPrompt(response);
    }

    private static Dimension dialogSize(Dimension screenSize) {
        return SqlExecutor.calcularTamanoDialogoError(() -> screenSize);
    }

    @Test
    void executesMysqlAndPostgresSchemasAndAddsNonEmptyResults() {
        RecordingDatabaseExecutor databaseExecutor = new RecordingDatabaseExecutor();
        databaseExecutor.results.add(result(new Object[]{"mysql-value"}));
        databaseExecutor.results.add(result(new Object[]{"postgres-value"}));
        RecordingScriptPanel scriptPanel = new RecordingScriptPanel();
        Connection mysqlConnection = connection();
        Connection postgresConnection = connection();
        ServerItem mysql = serverItem("mysql", TipoServidor.MYSQL, mysqlConnection, schema("db", true));
        ServerItem postgres = serverItem("postgres", TipoServidor.POSTGRESQL, postgresConnection, schema("public", true));
        postgres.getServidor().setExecutaAsRol(true);
        postgres.getServidor().setRol("app_role");
        SqlExecutor executor = executor(scriptPanel, databaseExecutor, new Component[]{new JLabel("ignored"), mysql, postgres});

        executor.doInBackground();

        assertEquals(List.of("db"), catalogs(mysqlConnection));
        assertEquals(List.of("public"), schemas(postgresConnection));
        assertEquals(List.of("SET ROLE app_role"), databaseExecutor.executedSql);
        assertEquals(List.of("select mysql", "select postgres"), databaseExecutor.executedAnySql);
        assertEquals(2, scriptPanel.results.size());
        assertEquals("mysql:db:select mysql", scriptPanel.results.get(0));
        assertEquals("postgres:public:select postgres", scriptPanel.results.get(1));
    }

    @Test
    void skipsUnselectedNullConnectionAndEmptyResults() {
        RecordingDatabaseExecutor databaseExecutor = new RecordingDatabaseExecutor();
        databaseExecutor.results.add(null);
        databaseExecutor.results.add(result(new Object[]{null}));
        RecordingScriptPanel scriptPanel = new RecordingScriptPanel();
        ServerItem skipped = serverItem("skipped", TipoServidor.MYSQL, connection(), schema("db", false));
        ServerItem disconnected = serverItem("disconnected", TipoServidor.MYSQL, null, schema("db", true));
        ServerItem emptyResult = serverItem("empty", TipoServidor.MYSQL, connection(), schema("db1", true), schema("db2", true));
        SqlExecutor executor = executor(scriptPanel, databaseExecutor, new Component[]{skipped, disconnected, emptyResult});

        executor.doInBackground();

        assertEquals(List.of("select mysql", "select mysql"), databaseExecutor.executedAnySql);
        assertTrue(scriptPanel.results.isEmpty());
        assertTrue(scriptPanel.errors.isEmpty());
    }

    @Test
    void stopsAllExecutionAfterConnectionError() {
        RecordingDatabaseExecutor databaseExecutor = new RecordingDatabaseExecutor();
        RecordingConnectionErrorNotifier connectionErrorNotifier = new RecordingConnectionErrorNotifier();
        ServerItem broken = serverItem("broken", TipoServidor.MYSQL, connection("setCatalog"), schema("db", true));
        ServerItem next = serverItem("next", TipoServidor.MYSQL, connection(), schema("next_db", true));
        SqlExecutor executor = executor(new RecordingScriptPanel(), databaseExecutor, connectionErrorNotifier,
                prompt(JOptionPane.YES_OPTION), new Component[]{broken, next});

        executor.doInBackground();

        assertEquals("broken", connectionErrorNotifier.servidor.getName());
        assertEquals("setCatalog failed", connectionErrorNotifier.error.getMessage());
        assertTrue(databaseExecutor.executedAnySql.isEmpty());
        assertTrue(catalogs(next.getDatabaseConnection()).isEmpty());
    }

    @Test
    void sqlErrorPromptResultControlsStatementLoop() {
        RecordingDatabaseExecutor databaseExecutor = new RecordingDatabaseExecutor();
        databaseExecutor.errors.add(new SQLException("bad sql"));
        databaseExecutor.results.add(result(new Object[]{"next schema"}));
        RecordingScriptPanel scriptPanel = new RecordingScriptPanel();
        RecordingSqlErrorPrompt sqlErrorPrompt = prompt(JOptionPane.NO_OPTION);
        ServerItem serverItem = serverItem("mysql", TipoServidor.MYSQL, connection(), schema("db1", true), schema("db2", true));
        SqlExecutor executor = executor(scriptPanel, databaseExecutor, new RecordingConnectionErrorNotifier(),
                sqlErrorPrompt, new Component[]{serverItem});

        executor.doInBackground();

        assertEquals(List.of("mysql:db1:select mysql:bad sql"), scriptPanel.errors);
        assertEquals("select mysql", sqlErrorPrompt.sentencia);
        assertEquals("bad sql", sqlErrorPrompt.mensajeError);
        assertEquals(2, databaseExecutor.executedAnySql.size());
        assertEquals(List.of("db1", "db2"), catalogs(serverItem.getDatabaseConnection()));
        assertEquals(List.of("mysql:db2:select mysql"), scriptPanel.results);
    }

    @Test
    void cancelBeforeRunSkipsServerIteration() {
        RecordingDatabaseExecutor databaseExecutor = new RecordingDatabaseExecutor();
        ServerItem serverItem = serverItem("mysql", TipoServidor.MYSQL, connection(), schema("db", true));
        SqlExecutor executor = executor(new RecordingScriptPanel(), databaseExecutor, new Component[]{serverItem});
        executor.cancel(true);

        executor.doInBackground();

        assertTrue(databaseExecutor.executedAnySql.isEmpty());
        assertTrue(catalogs(serverItem.getDatabaseConnection()).isEmpty());
    }

    @Test
    void executesMariaDbAndPostgresWithoutRoleWhenRoleIsDisabledOrBlank() {
        RecordingDatabaseExecutor databaseExecutor = new RecordingDatabaseExecutor();
        databaseExecutor.results.add(result(new Object[]{"mariadb"}));
        databaseExecutor.results.add(result(new Object[]{"postgres without role"}));
        databaseExecutor.results.add(result(new Object[]{"postgres blank role"}));
        ServerItem mariaDb = serverItem("mariadb", TipoServidor.MARIADB, connection(), schema("db", true));
        ServerItem postgresWithoutRole = serverItem("postgres-off", TipoServidor.POSTGRESQL, connection(), schema("public", true));
        postgresWithoutRole.getServidor().setExecutaAsRol(false);
        postgresWithoutRole.getServidor().setRol("app_role");
        ServerItem postgresBlankRole = serverItem("postgres-blank", TipoServidor.POSTGRESQL, connection(), schema("custom", true));
        postgresBlankRole.getServidor().setExecutaAsRol(true);
        postgresBlankRole.getServidor().setRol("");
        SqlExecutor executor = new SqlExecutor(new RecordingScriptPanel(),
                List.of("select mysql"), 1, List.of("select postgres"), 0,
                () -> new Component[]{mariaDb, postgresWithoutRole, postgresBlankRole},
                databaseExecutor,
                new RecordingConnectionErrorNotifier(),
                prompt(JOptionPane.YES_OPTION),
                () -> {
                },
                () -> {
                }
        );

        executor.doInBackground();

        assertEquals(List.of("db"), catalogs(mariaDb.getDatabaseConnection()));
        assertEquals(List.of("public"), schemas(postgresWithoutRole.getDatabaseConnection()));
        assertEquals(List.of("custom"), schemas(postgresBlankRole.getDatabaseConnection()));
        assertTrue(databaseExecutor.executedSql.isEmpty());
    }

    @Test
    void cancellationDuringStatementLoopStopsRemainingStatementsAndSchemas() {
        RecordingDatabaseExecutor databaseExecutor = new RecordingDatabaseExecutor();
        databaseExecutor.results.add(null);
        AtomicReference<SqlExecutor> executorRef = new AtomicReference<>();
        AtomicInteger executions = new AtomicInteger();
        databaseExecutor.onExecuteAny = () -> {
            if (executions.incrementAndGet() == 1) {
                executorRef.get().cancel(true);
            }
        };
        ServerItem serverItem = serverItem("mysql", TipoServidor.MYSQL, connection(), schema("db1", true), schema("db2", true));
        SqlExecutor executor = new SqlExecutor(new RecordingScriptPanel(),
                List.of("first", "second"), 2, List.of(), 0,
                () -> new Component[]{serverItem},
                databaseExecutor,
                new RecordingConnectionErrorNotifier(),
                prompt(JOptionPane.YES_OPTION),
                () -> {
                },
                () -> {
                }
        );
        executorRef.set(executor);

        executor.doInBackground();

        assertEquals(List.of("first"), databaseExecutor.executedAnySql);
        assertEquals(List.of("db1"), catalogs(serverItem.getDatabaseConnection()));
    }

    @Test
    void doneUnlocksAndNotifiesCompletion() {
        AtomicBoolean unlocked = new AtomicBoolean(false);
        AtomicBoolean notified = new AtomicBoolean(false);
        SqlExecutor executor = new SqlExecutor(new RecordingScriptPanel(), List.of(), 0, List.of(), 0,
                () -> new Component[0],
                new RecordingDatabaseExecutor(),
                new RecordingConnectionErrorNotifier(),
                prompt(JOptionPane.YES_OPTION),
                () -> unlocked.set(true),
                () -> notified.set(true)
        );

        executor.done();

        assertTrue(unlocked.get());
        assertTrue(notified.get());
    }

    @Test
    void errorDialogSizeUsesMinimumsAndMaximums() {
        assertEquals(new Dimension(500, 220), dialogSize(new Dimension(400, 200)));
        assertEquals(new Dimension(900, 420), dialogSize(new Dimension(2000, 2000)));
    }

    private static final class RecordingScriptPanel extends ScriptPanel {
        private final List<String> results = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        private RecordingScriptPanel() {
            super(null);
        }

        @Override
        public void addResultadoQuery(Servidor servidor,
                                      String esquema,
                                      String sentencia,
                                      Map.Entry<List<String>, List<Object[]>> resultado) {
            results.add(servidor.getName() + ":" + esquema + ":" + sentencia);
        }

        @Override
        public void addError(String server, String esquema, String sentencia, String error) {
            errors.add(server + ":" + esquema + ":" + sentencia + ":" + error);
        }
    }

    private static final class TestConnection implements java.lang.reflect.InvocationHandler {
        private final List<String> failures;
        private final List<String> catalogs = new ArrayList<>();
        private final List<String> schemas = new ArrayList<>();

        private TestConnection(List<String> failures) {
            this.failures = failures;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (failures.contains(methodName)) {
                throw new SQLException(methodName + " failed");
            }
            switch (methodName) {
                case "setCatalog" -> {
                    catalogs.add((String) args[0]);
                    return null;
                }
                case "setSchema" -> {
                    schemas.add((String) args[0]);
                    return null;
                }
                case "getCatalog" -> {
                    return catalogs.isEmpty() ? null : catalogs.getLast();
                }
                case "getSchema" -> {
                    return schemas.isEmpty() ? null : schemas.getLast();
                }
                case "toString" -> {
                    return "TestConnection";
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
            }
            throw new UnsupportedOperationException(methodName);
        }
    }

    private static final class RecordingDatabaseExecutor implements SqlExecutor.DatabaseExecutor {
        private final List<String> executedSql = new ArrayList<>();
        private final List<String> executedAnySql = new ArrayList<>();
        private final List<Map.Entry<List<String>, List<Object[]>>> results = new ArrayList<>();
        private final List<SQLException> errors = new ArrayList<>();
        private Runnable onExecuteAny = () -> {
        };

        @Override
        public void execute(Connection connection, String sql) {
            executedSql.add(sql);
        }

        @Override
        public Map.Entry<List<String>, List<Object[]>> executeAny(Connection connection, String sentencia) throws SQLException {
            executedAnySql.add(sentencia);
            onExecuteAny.run();
            if (!errors.isEmpty()) {
                throw errors.removeFirst();
            }
            if (results.isEmpty()) {
                return null;
            }
            return results.removeFirst();
        }
    }

    private static final class RecordingConnectionErrorNotifier implements SqlExecutor.ConnectionErrorNotifier {
        private Servidor servidor;
        private SQLException error;

        @Override
        public void showConnectionError(Servidor servidor, SQLException error) {
            this.servidor = servidor;
            this.error = error;
        }
    }

    private static final class RecordingSqlErrorPrompt implements SqlExecutor.SqlErrorPrompt {
        private final int response;
        private String sentencia;
        private String mensajeError;

        private RecordingSqlErrorPrompt(int response) {
            this.response = response;
        }

        @Override
        public int showSqlError(Component parent,
                                String sentencia,
                                String mensajeError,
                                String title,
                                Object[] options,
                                Object initialValue) {
            this.sentencia = sentencia;
            this.mensajeError = mensajeError;
            return response;
        }
    }
}
