package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.dialogos.ConfigServer;
import es.jklabs.gui.utilidades.listener.ServidorListener;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class ServersPanel extends JPanel {
    private final MainUI mainUI;

    public ServersPanel(MainUI mainUI) {
        super();
        this.mainUI = mainUI;
        setLayout(new BorderLayout());
        cargarPanel();
    }

    private void cargarPanel() {
        JPanel panelServidores = new JPanel();
        panelServidores.setLayout(new BoxLayout(panelServidores, BoxLayout.Y_AXIS));
        mainUI.getConfiguracion().getServers()
                .forEach(s -> panelServidores.add(getServer(s)));
        JButton btnAddServer = new JButton(Mensajes.getMensaje("anadir"));
        btnAddServer.addActionListener(this::addServer);
        super.add(panelServidores, BorderLayout.CENTER);
        super.add(btnAddServer, BorderLayout.SOUTH);
    }

    private void addServer(ActionEvent actionEvent) {
        ConfigServer configServer = new ConfigServer(mainUI);
        configServer.setVisible(true);
    }

    private JLabel getServer(Servidor servidor) {
        JLabel jLabel = new JLabel(servidor.getName());
        String icono = servidor.getTipoServidor().getIcono();
        jLabel.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource
                (icono))));
        jLabel.setVerticalTextPosition(SwingConstants.CENTER);
        jLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        jLabel.addMouseListener(new ServidorListener(this, jLabel, servidor));
        return jLabel;
    }

}
