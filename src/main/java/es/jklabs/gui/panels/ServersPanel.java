package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.dialogos.ConfigServer;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.LazadorHilos;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

public class ServersPanel extends JPanel {
    private MainUI mainUI;
    private JPanel panelServidores;
    private JButton btnAddServer;
    private ServerItem serverItemEditable;

    public ServersPanel(MainUI mainUI) {
        super();
        this.mainUI = mainUI;
        setLayout(new BorderLayout());
        cargarPanel();
    }

    private void cargarPanel() {
        panelServidores = new JPanel();
        panelServidores.setLayout(new BoxLayout(panelServidores, BoxLayout.Y_AXIS));
        mainUI.getConfiguracion().getServers().stream()
                .sorted(Comparator.comparing(Servidor::getName))
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
        ServerItem severItem;
        if (this.serverItemEditable != null) {
            severItem = this.serverItemEditable;
            severItem.update(servidor);
            this.serverItemEditable = null;
        } else {
            severItem = getServer(servidor);
            panelServidores.add(severItem);
        }
        severItem.loadEsquemas();
        SwingUtilities.updateComponentTreeUI(panelServidores);
    }

    public void eliminar(ServerItem servidor) {
        Optional<Component> op = Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem && Objects.equals(c, servidor))
                .findFirst();
        op.ifPresent(component -> panelServidores.remove(component));
        SwingUtilities.updateComponentTreeUI(panelServidores);
    }

    public void loadEsquemas() {
        List<Thread> hilos = new ArrayList<>();
        for (Component component : panelServidores.getComponents()) {
            if (component instanceof ServerItem) {
                hilos.add(((ServerItem) component).getHiloCarga());
            }
        }
        Thread hilo = new LazadorHilos(hilos, getMainUI());
        hilo.start();
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

    public void setEditable(ServerItem servidor) {
        this.serverItemEditable = servidor;
    }
}
