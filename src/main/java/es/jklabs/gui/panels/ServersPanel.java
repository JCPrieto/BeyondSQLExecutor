package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.dialogos.ConfigServer;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class ServersPanel extends JPanel {
    private MainUI mainUI;
    private JPanel panelServidores;
    private JButton btnAddServer;

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
        btnAddServer = new JButton(Mensajes.getMensaje("anadir"));
        btnAddServer.addActionListener(this::addServer);
        JScrollPane jScrollPane = new JScrollPane(panelServidores);
        jScrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        jScrollPane.setPreferredSize(new Dimension(400, 0));
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
        ServerItem severItem = getServer(servidor);
        panelServidores.add(severItem);
        SwingUtilities.updateComponentTreeUI(panelServidores);
        severItem.loadEsquemas();
    }

    public void eliminar(Servidor servidor) {
        Optional<Component> op = Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem && Objects.equals(((ServerItem) c).getServidor(), servidor))
                .findFirst();
        op.ifPresent(component -> panelServidores.remove(component));
        SwingUtilities.updateComponentTreeUI(panelServidores);
    }

    public void loadEsquemas() {
        Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem)
                .forEach(c -> ((ServerItem) c).loadEsquemas());
    }

    public JPanel getPanelServidores() {
        return panelServidores;
    }

    public void setPanelServidores(JPanel panelServidores) {
        this.panelServidores = panelServidores;
    }

    public MainUI getMainUI() {
        return mainUI;
    }

    public void setMainUI(MainUI mainUI) {
        this.mainUI = mainUI;
    }

    public void desbloquearPantalla() {
        Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem)
                .forEach(c -> ((ServerItem) c).desbloquearPantalla());
        btnAddServer.setEnabled(true);
    }

    public void bloquearPantalla() {
        Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem)
                .forEach(c -> ((ServerItem) c).bloquearPantalla());
        btnAddServer.setEnabled(false);
    }
}
