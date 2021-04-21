package es.jklabs.gui.utilidades.worker;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.UtilidadesBBDD;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class SqlExecutor extends SwingWorker<Void, Void> {
    private final ServersPanel serverPanel;
    private final List<String> sentencias;
    private final int total;
    private int count;

    public SqlExecutor(ServersPanel serverPanel, List<String> sentencias, int total) {
        this.serverPanel = serverPanel;
        this.sentencias = sentencias;
        this.total = total;
        this.count = 0;
    }

    @Override
    protected Void doInBackground() {
        Arrays.stream(serverPanel.getPanelServidores().getComponents())
                .filter(c -> c instanceof ServerItem)
                .forEach(c -> ejecutarSQL((ServerItem) c, sentencias));
        return null;
    }

    private void ejecutarSQL(ServerItem serverItem, List<String> sentencias) {
        serverItem.getEsquemas().entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .forEach(e -> ejecutarSQL(serverItem.getServidor(), e.getKey(), sentencias));
    }

    private void ejecutarSQL(Servidor servidor, String esquema, List<String> sentencias) {
        sentencias.forEach(sentencia -> ejecutarSQL(servidor, esquema, sentencia));
    }

    private void ejecutarSQL(Servidor servidor, String esquema, String sentencia) {
        try {
            UtilidadesBBDD.execute(servidor, esquema, sentencia);
        } catch (ClassNotFoundException e) {
            Growls.mostrarError(servidor.getName(), "ejecucion.sql", new String[]{esquema}, e);
        } catch (SQLException e) {
            Growls.mostrarError(servidor.getName() + " " + esquema, e.getMessage(), e);
        }
        int progreso;
        if (count++ == 0) {
            progreso = 0;
        } else {
            progreso = Math.toIntExact(Math.round(((double) count / (double) total) * 100));
            if (progreso > 100) {
                progreso = 100;
            }
        }
        setProgress(progreso);
    }

}