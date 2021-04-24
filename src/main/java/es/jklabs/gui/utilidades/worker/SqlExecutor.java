package es.jklabs.gui.utilidades.worker;

import es.jklabs.gui.panels.ScriptPanel;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesBBDD;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SqlExecutor extends SwingWorker<Void, Void> implements Serializable {
    public static final String EJECUCION_SQL = "ejecucion.sql";
    @Serial
    private static final long serialVersionUID = -6379570340235113885L;
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
        int retorno = 0;
        Iterator<Component> it = Arrays.stream(serverPanel.getPanelServidores().getComponents()).iterator();
        while (retorno < 2 && it.hasNext() && !isCancelled()) {
            Component component = it.next();
            if (component instanceof ServerItem) {
                retorno = ejecutarSQL((ServerItem) component, sentencias);
            }
        }
        return null;
    }

    private int ejecutarSQL(ServerItem serverItem, List<String> sentencias) {
        int retorno = 0;
        Iterator<Map.Entry<String, JCheckBox>> it = serverItem.getEsquemas().entrySet().iterator();
        while (retorno < 2 && it.hasNext() && !isCancelled()) {
            Map.Entry<String, JCheckBox> entry = it.next();
            if (entry.getValue().isSelected()) {
                retorno = ejecutarSQL(serverItem.getServidor(), entry.getKey(), sentencias);
            }
        }
        return retorno;
    }

    private int ejecutarSQL(Servidor servidor, String esquema, List<String> sentencias) {
        int retorno = 0;
        Iterator<String> it = sentencias.iterator();
        while (retorno == 0 && it.hasNext() && !isCancelled()) {
            retorno = ejecutarSQL(servidor, esquema, it.next());
        }
        return retorno;
    }

    private int ejecutarSQL(Servidor servidor, String esquema, String sentencia) {
        int retorno = 0;
        try {
            if (sentencia.toLowerCase().startsWith("select")) {
                Map.Entry<List<String>, List<Object[]>> resultado = UtilidadesBBDD.executeSelect(servidor, esquema, sentencia);
                scriptPanel.addResultadoQuery(servidor, esquema, resultado);
            } else {
                UtilidadesBBDD.execute(servidor, esquema, sentencia);
            }
        } catch (ClassNotFoundException e) {
            Growls.mostrarError(servidor.getName(), EJECUCION_SQL, new String[]{servidor.getName(), esquema}, e);
        } catch (SQLException e) {
            scriptPanel.addError(servidor.getName(), esquema, sentencia, e.getMessage());
            retorno = JOptionPane.showOptionDialog(
                    scriptPanel,
                    Mensajes.getError("ejecucion.sql.detalle", new String[]{sentencia, e.getMessage()}),
                    Mensajes.getError(EJECUCION_SQL, new String[]{servidor.getName(), esquema}),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    new Object[]{Mensajes.getMensaje("continuar"), Mensajes.getMensaje("saltar"), Mensajes.getMensaje("cancelar")},
                    Mensajes.getMensaje("continuar"));
            Logger.info(EJECUCION_SQL, new String[]{servidor.getName(), esquema}, e);
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
        return retorno;
    }

    @Override
    public void done() {
        serverPanel.getMainUI().desbloquearPantalla();
    }

}
