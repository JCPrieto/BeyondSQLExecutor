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
    private JPanel panelEsquemas;

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
        panelEsquemas = new JPanel();
        panelEsquemas.setLayout(new BoxLayout(panelEsquemas, BoxLayout.Y_AXIS));
        JScrollPane scrollEsquemas = new JScrollPane(panelEsquemas);
        scrollEsquemas.setPreferredSize(new Dimension(200, 200));
        add(scrollEsquemas, BorderLayout.CENTER);
        JPanel jPanel1 = new JPanel();
        JButton btnAll = new JButton("seleccionar.todos");
        btnAll.addActionListener(l -> checkAll());
        jPanel1.add(btnAll);
        JButton btnNone = new JButton("deseleccionar.todos");
        btnNone.addActionListener(l -> uncheckAll());
        jPanel1.add(btnNone);
        add(jPanel1, BorderLayout.SOUTH);
    }

    private void uncheckAll() {
        esquemas.forEach((key, value) -> value.setSelected(false));
    }

    private void checkAll() {
        esquemas.forEach((key, value) -> value.setSelected(true));
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

    public void loadEsquemas() {
        Thread hilo = new LoadSchemaThread(this);
        hilo.start();
    }

    public JPanel getPanelEsquemas() {
        return panelEsquemas;
    }

    public void setPanelEsquemas(JPanel panelEsquemas) {
        this.panelEsquemas = panelEsquemas;
    }

}
