package es.jklabs.gui.thread;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.utilidades.UtilidadesBBDD;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LoadSchemaThread implements Runnable {
    private final ServerItem serverItem;

    public LoadSchemaThread(ServerItem serverItem) {
        this.serverItem = serverItem;
    }

    @Override
    public void run() {
        try {
            List<String> esquemasBBDD = UtilidadesBBDD.getEsquemas(serverItem.getServidor());
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
            for (String esquema : esquemasBBDD) {
                if (!serverItem.getServidor().getEsquemasExcluidos().contains(esquema) &&
                        serverItem.getServidor().getEsquemasExcluidos().stream()
                                .noneMatch(s -> s.endsWith("*") && esquema.startsWith(s.replace("*", StringUtils.EMPTY)))) {
                    JCheckBox jCheckBox = new JCheckBox(esquema);
                    serverItem.getEsquemas().put(esquema, jCheckBox);
                    jPanel.add(jCheckBox);
                }
            }
            JScrollPane jScrollPane = new JScrollPane(jPanel);
            jScrollPane.setPreferredSize(new Dimension(200, 200));
            serverItem.add(jScrollPane, BorderLayout.CENTER);
            JPanel jPanel1 = new JPanel();
            JButton btnAll = new JButton("seleccionar.todos");
            btnAll.addActionListener(l -> checkAll());
            jPanel1.add(btnAll);
            JButton btnNone = new JButton("deseleccionar.todos");
            btnNone.addActionListener(l -> uncheckAll());
            jPanel1.add(btnNone);
            serverItem.add(jPanel1, BorderLayout.SOUTH);
            SwingUtilities.updateComponentTreeUI(serverItem);
        } catch (ClassNotFoundException | SQLException e) {
            Growls.mostrarError("servidor.getName()", "leer.esquemas", new String[]{UtilidadesBBDD.getURL(serverItem.getServidor())}, e);
        }
    }

    private void uncheckAll() {
        serverItem.getEsquemas().forEach((key, value) -> value.setSelected(false));
    }

    private void checkAll() {
        serverItem.getEsquemas().forEach((key, value) -> value.setSelected(true));
    }
}
