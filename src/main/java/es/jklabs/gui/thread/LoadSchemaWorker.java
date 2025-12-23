package es.jklabs.gui.thread;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.utilidades.UtilidadesBBDD;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LoadSchemaWorker extends SwingWorker<List<String>, Void> {
    private final ServerItem serverItem;
    private Runnable onDone;
    private Exception error;

    public LoadSchemaWorker(ServerItem serverItem) {
        this.serverItem = serverItem;
    }

    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    @Override
    protected List<String> doInBackground() {
        try {
            if (serverItem.getDatabaseConnection() == null) {
                serverItem.setDatabaseConnection(UtilidadesBBDD.getConexion(serverItem.getServidor()));
            }
            if (serverItem.getDatabaseConnection() == null) {
                return Collections.emptyList();
            }
            List<String> esquemasBBDD = UtilidadesBBDD.getEsquemas(serverItem.getDatabaseConnection());
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
                Growls.mostrarError(serverItem.getServidor().getName(), "leer.esquemas",
                        new String[]{UtilidadesBBDD.getURL(serverItem.getServidor())}, error);
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Growls.mostrarError(serverItem.getServidor().getName(), "leer.esquemas",
                    new String[]{UtilidadesBBDD.getURL(serverItem.getServidor())}, e);
        } catch (ExecutionException e) {
            Growls.mostrarError(serverItem.getServidor().getName(), "leer.esquemas",
                    new String[]{UtilidadesBBDD.getURL(serverItem.getServidor())}, e);
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
}
