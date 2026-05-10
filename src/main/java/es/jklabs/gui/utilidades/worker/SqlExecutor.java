package es.jklabs.gui.utilidades.worker;

import es.jklabs.gui.panels.ScriptPanel;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesBBDD;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class SqlExecutor extends SwingWorker<Void, Void> implements Serializable {
    public static final String EJECUCION_SQL = "ejecucion.sql";
    @Serial
    private static final long serialVersionUID = -6379570340235113885L;
    private final List<String> sentenciasMysql;
    private final int totalMysql;
    private final ScriptPanel scriptPanel;
    private final List<String> sentenciasPostgres;
    private final int totalPostreSQL;
    private final Supplier<Component[]> serverComponents;
    private final DatabaseExecutor databaseExecutor;
    private final ConnectionErrorNotifier connectionErrorNotifier;
    private final SqlErrorPrompt sqlErrorPrompt;
    private final Runnable unlockScreen;
    private final Runnable completionNotifier;
    private int count;

    public SqlExecutor(ScriptPanel scriptPanel, ServersPanel serverPanel, List<String> sentenciasMysql, int totalMysql, List<String> sentenciasPostgres, int totalPostreSQL) {
        this(scriptPanel,
                sentenciasMysql,
                totalMysql,
                sentenciasPostgres,
                totalPostreSQL,
                () -> serverPanel.getPanelServidores().getComponents(),
                new DefaultDatabaseExecutor(),
                (servidor, error) -> Growls.mostrarError(servidor.getName(), "conexion.bbdd",
                        new String[]{UtilidadesBBDD.getURL(servidor)}, error),
                new DefaultSqlErrorPrompt(() -> Toolkit.getDefaultToolkit().getScreenSize()),
                () -> serverPanel.getMainUI().desbloquearPantalla(),
                () -> Growls.mostrarInfo("ejecucion.completada")
        );
    }

    SqlExecutor(ScriptPanel scriptPanel,
                List<String> sentenciasMysql,
                int totalMysql,
                List<String> sentenciasPostgres,
                int totalPostreSQL,
                Supplier<Component[]> serverComponents,
                DatabaseExecutor databaseExecutor,
                ConnectionErrorNotifier connectionErrorNotifier,
                SqlErrorPrompt sqlErrorPrompt,
                Runnable unlockScreen,
                Runnable completionNotifier) {
        this.scriptPanel = scriptPanel;
        this.sentenciasMysql = sentenciasMysql;
        this.totalMysql = totalMysql;
        this.sentenciasPostgres = sentenciasPostgres;
        this.totalPostreSQL = totalPostreSQL;
        this.serverComponents = serverComponents;
        this.databaseExecutor = databaseExecutor;
        this.connectionErrorNotifier = connectionErrorNotifier;
        this.sqlErrorPrompt = sqlErrorPrompt;
        this.unlockScreen = unlockScreen;
        this.completionNotifier = completionNotifier;
        this.count = 0;
    }

    private static JPanel crearPanelErrorSql(String sentencia, String mensajeError, Supplier<Dimension> screenSize) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JTextArea areaError = new JTextArea(Mensajes.getError("ejecucion.sql.detalle", new String[]{sentencia, mensajeError}));
        areaError.setEditable(false);
        areaError.setLineWrap(false);
        areaError.setWrapStyleWord(false);
        areaError.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(areaError);
        scrollPane.setBorder(BorderFactory.createTitledBorder(Mensajes.getMensaje("detalle.error.sql")));
        scrollPane.setPreferredSize(calcularTamanoDialogoError(screenSize));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(new JLabel(Mensajes.getMensaje("ejecucion.sql.pregunta")), BorderLayout.SOUTH);
        return panel;
    }

    static Dimension calcularTamanoDialogoError(Supplier<Dimension> screenSize) {
        Dimension currentScreenSize = screenSize.get();
        int maxWidth = Math.max(500, (int) (currentScreenSize.width * 0.65));
        int maxHeight = Math.max(220, (int) (currentScreenSize.height * 0.35));
        return new Dimension(Math.min(900, maxWidth), Math.min(420, maxHeight));
    }

    private int ejecutarSQL(ServerItem serverItem, List<String> sentencias) {
        int retorno = 0;
        Iterator<Map.Entry<String, JCheckBox>> it = serverItem.getEsquemas().entrySet().iterator();
        while (retorno < 2 && it.hasNext() && !isCancelled()) {
            Map.Entry<String, JCheckBox> entry = it.next();
            retorno = ejecutarSQL(serverItem, sentencias, entry, retorno);
        }
        return retorno;
    }

    @Override
    protected Void doInBackground() {
        int retorno = 0;
        Iterator<Component> it = Arrays.stream(serverComponents.get()).iterator();
        while (retorno < 2 && it.hasNext() && !isCancelled()) {
            Component component = it.next();
            if (component instanceof ServerItem) {
                if (!Objects.equals(((ServerItem) component).getServidor().getTipoServidor(), TipoServidor.POSTGRESQL)) {
                    retorno = ejecutarSQL((ServerItem) component, sentenciasMysql);
                } else {
                    retorno = ejecutarSQL((ServerItem) component, sentenciasPostgres);
                }
            }
        }
        return null;
    }

    private int ejecutarSQL(Connection connection, Servidor servidor, String esquema, List<String> sentencias) {
        int retorno = 0;
        Iterator<String> it = sentencias.iterator();
        while (retorno == 0 && it.hasNext() && !isCancelled()) {
            retorno = ejecutarSQL(connection, servidor, esquema, it.next());
        }
        return retorno;
    }

    private void establecerEsquema(ServerItem serverItem,
                                   Map.Entry<String, JCheckBox> entry) throws SQLException {
        if (Objects.equals(serverItem.getServidor().getTipoServidor(), TipoServidor.MYSQL) ||
                Objects.equals(serverItem.getServidor().getTipoServidor(), TipoServidor.MARIADB)) {
            serverItem.getDatabaseConnection().setCatalog(entry.getKey());
        } else {
            serverItem.getDatabaseConnection().setSchema(entry.getKey());
            if (Objects.equals(serverItem.getServidor().getExecutaAsRol(), Boolean.TRUE) &&
                    StringUtils.isNotEmpty(serverItem.getServidor().getRol())) {
                databaseExecutor.execute(serverItem.getDatabaseConnection(), "SET ROLE " + serverItem.getServidor().getRol());
            }
        }
    }

    private int ejecutarSQL(ServerItem serverItem, List<String> sentencias, Map.Entry<String, JCheckBox> entry, int retorno) {
        try {
            if (entry.getValue().isSelected() && serverItem.getDatabaseConnection() != null) {
                establecerEsquema(serverItem, entry);
                retorno = ejecutarSQL(serverItem.getDatabaseConnection(), serverItem.getServidor(), entry.getKey(), sentencias);
            }
        } catch (SQLException e) {
            connectionErrorNotifier.showConnectionError(serverItem.getServidor(), e);
            retorno = 2;
        }
        return retorno;
    }

    private int ejecutarSQL(Connection connection, Servidor servidor, String esquema, String sentencia) {
        int retorno = 0;
        try {
            Map.Entry<List<String>, List<Object[]>> resultado = databaseExecutor.executeAny(connection, sentencia);
            if (resultado != null &&
                    resultado.getValue().stream().anyMatch(r -> Arrays.stream(r).anyMatch(Objects::nonNull))) {
                scriptPanel.addResultadoQuery(servidor, esquema, sentencia, resultado);
            }
        } catch (SQLException e) {
            scriptPanel.addError(servidor.getName(), esquema, sentencia, e.getMessage());
            retorno = sqlErrorPrompt.showSqlError(
                    scriptPanel,
                    sentencia,
                    e.getMessage(),
                    Mensajes.getError(EJECUCION_SQL, new String[]{servidor.getName(), esquema}),
                    new Object[]{Mensajes.getMensaje("continuar"), Mensajes.getMensaje("saltar"), Mensajes.getMensaje("cancelar")},
                    Mensajes.getMensaje("continuar"));
            Logger.info(EJECUCION_SQL, new String[]{servidor.getName(), esquema}, e);
        }
        int progreso;
        if (count++ == 0) {
            progreso = 0;
        } else {
            progreso = Math.toIntExact(Math.round(((double) count / ((double) totalMysql + (double) totalPostreSQL)) * 100));
            if (progreso > 100) {
                progreso = 100;
            }
        }
        setProgress(progreso);
        return retorno;
    }

    @Override
    public void done() {
        unlockScreen.run();
        completionNotifier.run();
    }

    interface DatabaseExecutor {
        void execute(Connection connection, String sql) throws SQLException;

        Map.Entry<List<String>, List<Object[]>> executeAny(Connection connection, String sentencia) throws SQLException;
    }

    interface ConnectionErrorNotifier {
        void showConnectionError(Servidor servidor, SQLException error);
    }

    interface SqlErrorPrompt {
        int showSqlError(Component parent,
                         String sentencia,
                         String mensajeError,
                         String title,
                         Object[] options,
                         Object initialValue);
    }

    private static final class DefaultDatabaseExecutor implements DatabaseExecutor {
        @Override
        public void execute(Connection connection, String sql) throws SQLException {
            UtilidadesBBDD.execute(connection, sql);
        }

        @Override
        public Map.Entry<List<String>, List<Object[]>> executeAny(Connection connection, String sentencia) throws SQLException {
            return UtilidadesBBDD.executeAny(connection, sentencia);
        }
    }

    private record DefaultSqlErrorPrompt(Supplier<Dimension> screenSize) implements SqlErrorPrompt {

        @Override
        public int showSqlError(Component parent,
                                String sentencia,
                                String mensajeError,
                                String title,
                                Object[] options,
                                Object initialValue) {
            return JOptionPane.showOptionDialog(
                    parent,
                    crearPanelErrorSql(sentencia, mensajeError, screenSize),
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    options,
                    initialValue);
        }
    }

}
