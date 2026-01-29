package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.dialogos.ConfigServer;
import es.jklabs.gui.thread.LoadSchemaWorker;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ServersPanel extends JPanel {
    private final MainUI mainUI;
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
        JScrollPane jScrollPane = new JScrollPane(
                panelServidores,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        jScrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        jScrollPane.setPreferredSize(new Dimension(400, 0));
        super.add(jScrollPane, BorderLayout.CENTER);
        super.add(btnAddServer, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(calcularAnchoMinimoPanel(jScrollPane), 0));
    }

    private int calcularAnchoMinimoPanel(JScrollPane scrollPane) {
        int hgap = new FlowLayout().getHgap();
        JButton btnAll = new JButton(Mensajes.getMensaje("seleccionar.todos"));
        JButton btnNone = new JButton(Mensajes.getMensaje("deseleccionar.todos"));
        int botonesWidth = btnAll.getPreferredSize().width + btnNone.getPreferredSize().width + (hgap * 3);
        int borderWidth = 0;
        if (scrollPane.getBorder() != null) {
            Insets insets = scrollPane.getBorder().getBorderInsets(scrollPane);
            borderWidth = insets.left + insets.right;
        }
        int scrollBarWidth = UIManager.getInt("ScrollBar.width");
        if (scrollBarWidth <= 0) {
            scrollBarWidth = 16;
        }
        int minWidth = botonesWidth + borderWidth + scrollBarWidth;
        if (btnAddServer != null) {
            minWidth = Math.max(minWidth, btnAddServer.getPreferredSize().width);
        }
        return minWidth;
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
        servidor.closeConnection();
        Optional<Component> op = Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem && Objects.equals(c, servidor))
                .findFirst();
        op.ifPresent(component -> panelServidores.remove(component));
        SwingUtilities.updateComponentTreeUI(panelServidores);
    }

    public void loadEsquemas() {
        List<LoadSchemaWorker> workers = new ArrayList<>();
        for (Component component : panelServidores.getComponents()) {
            if (component instanceof ServerItem) {
                workers.add(((ServerItem) component).getLoadSchemaWorker());
            }
        }
        if (workers.isEmpty()) {
            mainUI.refresSplit();
            return;
        }
        AtomicInteger remaining = new AtomicInteger(workers.size());
        for (LoadSchemaWorker worker : workers) {
            worker.setOnDone(() -> {
                if (remaining.decrementAndGet() == 0) {
                    mainUI.refresSplit();
                }
            });
            worker.execute();
        }
    }

    public JPanel getPanelServidores() {
        return panelServidores;
    }

    public MainUI getMainUI() {
        return mainUI;
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

    public void closeAllConnections() {
        Arrays.stream(panelServidores.getComponents())
                .filter(c -> c instanceof ServerItem)
                .forEach(c -> ((ServerItem) c).closeConnection());
    }

    public void refrescar(Configuracion configuracion) {
        panelServidores.removeAll();
        configuracion.getServers().stream()
                .sorted(Comparator.comparing(Servidor::getName))
                .forEach(s -> panelServidores.add(getServer(s)));
        SwingUtilities.updateComponentTreeUI(panelServidores);
        loadEsquemas();
    }
}
