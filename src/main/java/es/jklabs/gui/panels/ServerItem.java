package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.thread.LoadSchemaThread;
import es.jklabs.gui.utilidades.UtilidadesImagenes;
import es.jklabs.gui.utilidades.listener.ServidorListener;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.*;

public class ServerItem extends JPanel {
    private final MainUI mainUI;
    private final String id;
    private Connection databaseConnection;
    private Map<String, JCheckBox> esquemas;
    private Servidor servidor;
    private JPanel panelEsquemas;
    private JButton btnAll;
    private JButton btnNone;
    private JLabel jLabel;
    private JScrollPane scrollEsquemas;

    public ServerItem(MainUI mainUI, Servidor servidor) {
        super();
        this.id = String.valueOf(UUID.randomUUID());
        setLayout(new BorderLayout());
        this.mainUI = mainUI;
        this.servidor = servidor;
        this.esquemas = new HashMap<>();
        cargarInfo();
    }

    private void cargarInfo() {
        jLabel = new JLabel();
        jLabel.setVerticalTextPosition(SwingConstants.CENTER);
        jLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        jLabel.addMouseListener(new ServidorListener(mainUI, this));
        updateDescription(servidor);
        add(jLabel, BorderLayout.NORTH);
        panelEsquemas = new JPanel();
        panelEsquemas.setLayout(new BoxLayout(panelEsquemas, BoxLayout.Y_AXIS));
        scrollEsquemas = new JScrollPane(panelEsquemas);
        add(scrollEsquemas, BorderLayout.CENTER);
        JPanel jPanel1 = new JPanel();
        btnAll = new JButton(Mensajes.getMensaje("seleccionar.todos"));
        btnAll.addActionListener(l -> checkAll());
        jPanel1.add(btnAll);
        btnNone = new JButton(Mensajes.getMensaje("deseleccionar.todos"));
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

    public void desbloquearPantalla() {
        esquemas.forEach((key, value) -> value.setEnabled(true));
        btnAll.setEnabled(true);
        btnNone.setEnabled(true);
        Arrays.stream(jLabel.getMouseListeners())
                .filter(ServidorListener.class::isInstance)
                .forEach(m -> ((ServidorListener) m).setEnable(true));
    }

    public void bloquearPantalla() {
        esquemas.forEach((key, value) -> value.setEnabled(false));
        btnAll.setEnabled(false);
        btnNone.setEnabled(false);
        Arrays.stream(jLabel.getMouseListeners())
                .filter(ServidorListener.class::isInstance)
                .forEach(m -> ((ServidorListener) m).setEnable(false));
    }

    public JScrollPane getScrollEsquemas() {
        return scrollEsquemas;
    }

    public void setScrollEsquemas(JScrollPane scrollEsquemas) {
        this.scrollEsquemas = scrollEsquemas;
    }

    public Thread getHiloCarga() {
        return new LoadSchemaThread(this);
    }

    public Connection getDatabaseConnection() {
        return databaseConnection;
    }

    public void setDatabaseConnection(Connection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerItem that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public void update(Servidor servidor) {
        this.servidor = servidor;
        this.databaseConnection = null;
        this.esquemas.clear();
        List<Component> checks = Arrays.stream(this.panelEsquemas.getComponents())
                .filter(c -> c instanceof JCheckBox)
                .toList();
        checks.forEach(c -> panelEsquemas.remove(c));
        updateDescription(servidor);
    }

    private void updateDescription(Servidor servidor) {
        jLabel.setText(servidor.getName());
        String icono = servidor.getTipoServidor().getIcono();
        jLabel.setIcon(UtilidadesImagenes.getIcono(icono));
    }
}
