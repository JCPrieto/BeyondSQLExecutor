package es.jklabs.gui.popup;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.dialogos.ConfigServer;
import es.jklabs.gui.panels.ServerItem;
import es.jklabs.gui.utilidades.IconUtils;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
public class ServerPopUp extends JPopupMenu {
    private final ServerItem servidor;
    private final MainUI mainUI;

    public ServerPopUp(MainUI mainUI, ServerItem servidor) {
        super();
        this.mainUI = mainUI;
        this.servidor = servidor;
        cargarElementos();
    }

    private void cargarElementos() {
        JMenuItem jmiEditar = new JMenuItem(Mensajes.getMensaje("editar"), IconUtils.loadIconScaled("edit.png", 24, 24));
        jmiEditar.addActionListener(l -> editar());
        add(jmiEditar);
        JMenuItem jmiEliminar = new JMenuItem(Mensajes.getMensaje("eliminar"), IconUtils.loadIconScaled("trash.png", 24, 24));
        jmiEliminar.addActionListener(l -> eliminar());
        add(jmiEliminar);
    }

    private void eliminar() {
        ImageIcon icon = IconUtils.loadIcon("trash.png");
        int input = JOptionPane.showConfirmDialog(mainUI, Mensajes.getMensaje("confirmacion.eliminar", new String[]{servidor.getName()}), Mensajes.getMensaje("eliminar"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon);
        if (input == JOptionPane.YES_OPTION) {
            mainUI.eliminar(servidor);
        }
    }

    private void editar() {
        mainUI.setEditable(servidor);
        ConfigServer configServer = new ConfigServer(mainUI, servidor.getServidor());
        configServer.setVisible(true);
    }
}
