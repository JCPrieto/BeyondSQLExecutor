package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.dialogos.ConfigServer;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class ServersPanel extends JPanel {
    private final MainUI mainUI;
    private JPanel panelServidores;

    public ServersPanel(MainUI mainUI) {
        super();
        this.mainUI = mainUI;
        setLayout(new BorderLayout());
        cargarPanel();
    }

    private void cargarPanel() {
        panelServidores = new JPanel();
        panelServidores.setLayout(new BoxLayout(panelServidores, BoxLayout.Y_AXIS));
        mainUI.getConfiguracion().getServers()
                .forEach(s -> panelServidores.add(getServer(s)));
        JButton btnAddServer = new JButton(Mensajes.getMensaje("anadir"));
        btnAddServer.addActionListener(this::addServer);
        JScrollPane jScrollPane = new JScrollPane(panelServidores);
        super.add(jScrollPane, BorderLayout.CENTER);
        super.add(btnAddServer, BorderLayout.SOUTH);
    }

    private void addServer(ActionEvent actionEvent) {
        ConfigServer configServer = new ConfigServer(mainUI);
        configServer.setVisible(true);
    }

    private ServerItem getServer(Servidor servidor) {
        return new ServerItem(mainUI, servidor);
    }

    public void actualizarServidor(Servidor servidor) {
        eliminar(servidor);
        panelServidores.add(getServer(servidor));
        SwingUtilities.updateComponentTreeUI(panelServidores);
    }

    public void eliminar(Servidor servidor) {
        Optional<Component> op = Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem && Objects.equals(((ServerItem) c).getServidor(), servidor))
                .findFirst();
        op.ifPresent(component -> panelServidores.remove(component));
        SwingUtilities.updateComponentTreeUI(panelServidores);
    }
}
