package es.jklabs.gui.utilidades.renderer;

import es.jklabs.json.configuracion.TipoServidor;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TipoServidorComboRenderer extends JLabel implements javax.swing.ListCellRenderer<TipoServidor> {

    @Override
    public Component getListCellRendererComponent(JList<? extends TipoServidor> jList, TipoServidor tipoServidor, int i, boolean b, boolean b1) {
        setIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource
                (tipoServidor.getIcono()))));
        setText(tipoServidor.getNombre());
        return this;
    }
}
