package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.listener.ServidorListener;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.UtilidadesBBDD;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServerItem extends JPanel {
    private final MainUI mainUI;
    private final Map<String, JCheckBox> esquemas;
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
        jLabel.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource
                (icono))));
        jLabel.setVerticalTextPosition(SwingConstants.CENTER);
        jLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        jLabel.addMouseListener(new ServidorListener(mainUI, servidor));
        add(jLabel, BorderLayout.NORTH);
        try {
            List<String> esquemasBBDD = UtilidadesBBDD.getEsquemas(servidor);
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
            for (String esquema : esquemasBBDD) {
                JCheckBox jCheckBox = new JCheckBox(esquema);
                esquemas.put(esquema, jCheckBox);
                jPanel.add(jCheckBox);
            }
            JScrollPane jScrollPane = new JScrollPane(jPanel);
            jScrollPane.setPreferredSize(new Dimension(200, 200));
            add(jScrollPane, BorderLayout.CENTER);
            JPanel jPanel1 = new JPanel();
            JButton btnAll = new JButton("seleccionar.todos");
            btnAll.addActionListener(l -> checkAll());
            jPanel1.add(btnAll);
            JButton btnNone = new JButton("deseleccionar.todos");
            btnNone.addActionListener(l -> uncheckAll());
            jPanel1.add(btnNone);
            add(jPanel1, BorderLayout.SOUTH);
        } catch (ClassNotFoundException | SQLException e) {
            Growls.mostrarError("servidor.getName()", "leer.esquemas", new String[]{UtilidadesBBDD.getURL(servidor)}, e);
        }
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
}
