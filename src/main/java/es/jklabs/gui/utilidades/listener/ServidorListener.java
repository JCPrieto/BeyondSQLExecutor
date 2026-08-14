package es.jklabs.gui.utilidades.listener;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.popup.ServerPopUp;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiFunction;

public class ServidorListener extends MouseAdapter {

    private final MainUI mainUI;
    private final ServerItem servidor;
    private final BiFunction<MainUI, ServerItem, JPopupMenu> serverPopUpFactory;
    private boolean enable;

    public ServidorListener(MainUI mainUI, ServerItem servidor) {
        this(mainUI, servidor, ServerPopUp::new);
    }

    ServidorListener(MainUI mainUI, ServerItem servidor,
                     BiFunction<MainUI, ServerItem, JPopupMenu> serverPopUpFactory) {
        this.mainUI = mainUI;
        this.servidor = servidor;
        this.serverPopUpFactory = serverPopUpFactory;
        this.enable = true;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (enable && SwingUtilities.isRightMouseButton(e)) {
            JPopupMenu serverPopUp = serverPopUpFactory.apply(mainUI, servidor);
            serverPopUp.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public boolean getEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
