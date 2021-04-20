package es.jklabs.gui.utilidades.listener;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.popup.ServerPopUp;
import es.jklabs.json.configuracion.Servidor;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ServidorListener implements MouseListener {

    private final MainUI mainUI;
    private final Servidor servidor;

    public ServidorListener(MainUI mainUI, Servidor servidor) {
        this.mainUI = mainUI;
        this.servidor = servidor;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
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
}
