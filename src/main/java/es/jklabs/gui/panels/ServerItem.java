package es.jklabs.gui.panels;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.listener.ServidorListener;
import es.jklabs.json.configuracion.Servidor;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ServerItem extends JPanel {
    private final MainUI mainUI;
    private Servidor servidor;

    public ServerItem(MainUI mainUI, Servidor servidor) {
        super();
        setLayout(new BorderLayout());
        this.mainUI = mainUI;
        this.servidor = servidor;
        cargarInfo();
    }

    private void cargarInfo() {
        JLabel jLabel = new JLabel(servidor.getName());
        String icono = servidor.getTipoServidor().getIcono();
        jLabel.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource
                (icono))));
        jLabel.setVerticalTextPosition(SwingConstants.CENTER);
        jLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        jLabel.addMouseListener(new ServidorListener(mainUI, servidor));
        add(jLabel);
    }

    public Servidor getServidor() {
        return servidor;
    }

    public void setServidor(Servidor servidor) {
        this.servidor = servidor;
    }
}
