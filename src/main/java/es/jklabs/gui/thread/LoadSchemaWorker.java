package es.jklabs.gui.thread;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.UtilidadesBBDD;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LoadSchemaWorker extends SwingWorker<List<String>, Void> {
    private final ServerItem serverItem;
    private final SchemaLoader schemaLoader;
    private final ErrorNotifier errorNotifier;
    private Runnable onDone;
    private Exception error;

    public LoadSchemaWorker(ServerItem serverItem) {
        this(serverItem, new DefaultSchemaLoader(), new GrowlErrorNotifier());
    }

    LoadSchemaWorker(ServerItem serverItem, SchemaLoader schemaLoader, ErrorNotifier errorNotifier) {
        this.serverItem = serverItem;
        this.schemaLoader = schemaLoader;
        this.errorNotifier = errorNotifier;
    }

    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    @Override
    protected List<String> doInBackground() {
        try {
            if (serverItem.getDatabaseConnection() == null) {
                serverItem.setDatabaseConnection(schemaLoader.getConnection(serverItem.getServidor()));
            }
            if (serverItem.getDatabaseConnection() == null) {
                return Collections.emptyList();
            }
            List<String> esquemasBBDD = schemaLoader.getSchemas(serverItem.getDatabaseConnection());
            if (esquemasBBDD.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> filtrados = new ArrayList<>();
            for (String esquema : esquemasBBDD) {
                if (!isExcluded(esquema)) {
                    filtrados.add(esquema);
                }
            }
            return filtrados;
        } catch (SQLException | ClassNotFoundException e) {
            error = e;
            return Collections.emptyList();
        }
    }

    @Override
    protected void done() {
        try {
            if (error != null) {
                errorNotifier.showSchemaLoadError(serverItem.getServidor(), error);
                return;
            }
            List<String> esquemas = get();
            serverItem.getScrollEsquemas().setPreferredSize(new Dimension(200, 20));
            for (String esquema : esquemas) {
                JCheckBox jCheckBox = new JCheckBox(esquema);
                serverItem.getEsquemas().put(esquema, jCheckBox);
                serverItem.getPanelEsquemas().add(jCheckBox);
            }
            serverItem.getScrollEsquemas().setPreferredSize(getDimension(serverItem.getEsquemas().size()));
            serverItem.getPanelEsquemas().revalidate();
            serverItem.getPanelEsquemas().repaint();
            serverItem.revalidate();
            serverItem.repaint();
            Container parent = serverItem.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorNotifier.showSchemaLoadError(serverItem.getServidor(), e);
        } catch (ExecutionException e) {
            errorNotifier.showSchemaLoadError(serverItem.getServidor(), e);
        } finally {
            if (onDone != null) {
                onDone.run();
            }
        }
    }

    private boolean isExcluded(String esquema) {
        List<String> excluidos = serverItem.getServidor().getEsquemasExcluidos();
        if (excluidos.contains(esquema)) {
            return true;
        }
        for (String exclusion : excluidos) {
            if (exclusion.endsWith("*") && esquema.startsWith(exclusion.replace("*", StringUtils.EMPTY))) {
                return true;
            }
        }
        return false;
    }

    private Dimension getDimension(int size) {
        if (size < 9) {
            return new Dimension(200, (250 / 9) * size);
        }
        return new Dimension(200, 250);
    }

    interface SchemaLoader {
        Connection getConnection(Servidor servidor) throws SQLException, ClassNotFoundException;

        List<String> getSchemas(Connection connection) throws SQLException;
    }

    interface ErrorNotifier {
        void showSchemaLoadError(Servidor servidor, Exception error);
    }

    private static class DefaultSchemaLoader implements SchemaLoader {
        @Override
        public Connection getConnection(Servidor servidor) throws SQLException, ClassNotFoundException {
            return UtilidadesBBDD.getConexion(servidor);
        }

        @Override
        public List<String> getSchemas(Connection connection) throws SQLException {
            return UtilidadesBBDD.getEsquemas(connection);
        }
    }

    private static class GrowlErrorNotifier implements ErrorNotifier {
        @Override
        public void showSchemaLoadError(Servidor servidor, Exception error) {
            Growls.mostrarError(servidor.getName(), "leer.esquemas",
                    new String[]{UtilidadesBBDD.getURL(servidor)}, error);
        }
    }
}
