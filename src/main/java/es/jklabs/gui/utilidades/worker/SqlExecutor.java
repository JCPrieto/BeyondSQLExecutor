package es.jklabs.gui.utilidades.worker;

import es.jklabs.gui.panels.ScriptPanel;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.UtilidadesBBDD;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SqlExecutor extends SwingWorker<Void, Void> {
    private final ServersPanel serverPanel;
    private final List<String> sentencias;
    private final int total;
    private final ScriptPanel scriptPanel;
    private int count;

    public SqlExecutor(ScriptPanel scriptPanel, ServersPanel serverPanel, List<String> sentencias, int total) {
        this.scriptPanel = scriptPanel;
        this.serverPanel = serverPanel;
        this.sentencias = sentencias;
        this.total = total;
        this.count = 0;
    }

    @Override
    protected Void doInBackground() {
        Arrays.stream(serverPanel.getPanelServidores().getComponents())
                .filter(ServerItem.class::isInstance)
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
            if (sentencia.toLowerCase().startsWith("select")) {
                Map.Entry<List<String>, List<Object[]>> resultado = UtilidadesBBDD.executeSelect(servidor, esquema, sentencia);
                scriptPanel.addResultadoQuery(servidor, esquema, resultado);
            } else {
                UtilidadesBBDD.execute(servidor, esquema, sentencia);
            }
        } catch (ClassNotFoundException e) {
            Growls.mostrarError(servidor.getName(), "ejecucion.sql", new String[]{esquema}, e);
        } catch (SQLException e) {
            scriptPanel.addError(servidor.getName(), esquema, sentencia, e.getMessage());
            Logger.info("ejecucion.sql", e);
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

    @Override
    public void done() {
        serverPanel.getMainUI().desbloquearPantalla();
    }

}
