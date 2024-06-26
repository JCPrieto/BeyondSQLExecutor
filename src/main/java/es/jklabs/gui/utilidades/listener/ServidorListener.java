package es.jklabs.gui.utilidades.listener;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.popup.ServerPopUp;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ServidorListener implements MouseListener {

    private final MainUI mainUI;
    private final ServerItem servidor;
    private boolean enable;

    public ServidorListener(MainUI mainUI, ServerItem servidor) {
        this.mainUI = mainUI;
        this.servidor = servidor;
        this.enable = true;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (enable && SwingUtilities.isRightMouseButton(e)) {
            ServerPopUp serverPopUp = new ServerPopUp(mainUI, servidor);
            serverPopUp.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    public boolean getEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
