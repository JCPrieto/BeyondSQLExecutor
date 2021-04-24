package es.jklabs.gui.thread;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.utilidades.UtilidadesBBDD;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LoadSchemaThread extends Thread {
    private final ServerItem serverItem;

    public LoadSchemaThread(ServerItem serverItem) {
        super(serverItem.getServidor().getName() + "_LoadSchema");
        this.serverItem = serverItem;
    }

    @Override
    public void run() {
        try {
            serverItem.getScrollEsquemas().setPreferredSize(new Dimension(200, 20));
            List<String> esquemasBBDD = UtilidadesBBDD.getEsquemas(serverItem.getServidor());
            if (!esquemasBBDD.isEmpty()) {
                for (String esquema : esquemasBBDD) {
                    if (!serverItem.getServidor().getEsquemasExcluidos().contains(esquema) &&
                            serverItem.getServidor().getEsquemasExcluidos().stream()
                                    .noneMatch(s -> s.endsWith("*") && esquema.startsWith(s.replace("*", StringUtils.EMPTY)))) {
                        JCheckBox jCheckBox = new JCheckBox(esquema);
                        serverItem.getEsquemas().put(esquema, jCheckBox);
                        serverItem.getPanelEsquemas().add(jCheckBox);
                    }
                }
                serverItem.getScrollEsquemas().setPreferredSize(getDimension(serverItem.getEsquemas().size()));
            }
        } catch (ClassNotFoundException | SQLException e) {
            Growls.mostrarError(serverItem.getServidor().getName(), "leer.esquemas", new String[]{UtilidadesBBDD.getURL(serverItem.getServidor())}, e);
        }
    }

    private Dimension getDimension(int size) {
        if (size < 9) {
            return new Dimension(200, (250 / 9) * size);
        } else {
            return new Dimension(200, 250);
        }
    }

}
