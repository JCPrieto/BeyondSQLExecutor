package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.thread.LoadSchemaThread;
import es.jklabs.gui.utilidades.UtilidadesImagenes;
import es.jklabs.gui.utilidades.listener.ServidorListener;
import es.jklabs.json.configuracion.Servidor;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ServerItem extends JPanel {
    private final MainUI mainUI;
    private Map<String, JCheckBox> esquemas;
    private Servidor servidor;

    public ServerItem(MainUI mainUI, Servidor servidor) {
        super();
        setLayout(new BorderLayout());
        this.mainUI = mainUI;
        this.servidor = servidor;
        this.esquemas = new HashMap<>();
        cargarInfo();
    }

    private void cargarInfo() {
        JLabel jLabel = new JLabel(servidor.getName());
        String icono = servidor.getTipoServidor().getIcono();
        jLabel.setIcon(UtilidadesImagenes.getIcono(icono));
        jLabel.setVerticalTextPosition(SwingConstants.CENTER);
        jLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        jLabel.addMouseListener(new ServidorListener(mainUI, servidor));
        add(jLabel, BorderLayout.NORTH);
        Runnable hilo = new LoadSchemaThread(this);
        hilo.run();
    }

    public Servidor getServidor() {
        return servidor;
    }

    public void setServidor(Servidor servidor) {
        this.servidor = servidor;
    }

    public Map<String, JCheckBox> getEsquemas() {
        return esquemas;
    }

    public void setEsquemas(Map<String, JCheckBox> esquemas) {
        this.esquemas = esquemas;
    }
}
