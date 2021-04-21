package es.jklabs.gui.utilidades.renderer;

import es.jklabs.gui.utilidades.UtilidadesImagenes;
import es.jklabs.json.configuracion.TipoServidor;

import javax.swing.*;
import java.awt.*;

public class TipoServidorComboRenderer extends JLabel implements javax.swing.ListCellRenderer<TipoServidor> {

    @Override
    public Component getListCellRendererComponent(JList<? extends TipoServidor> jList, TipoServidor tipoServidor, int i, boolean b, boolean b1) {
        setIcon(UtilidadesImagenes.getIcono(tipoServidor.getIcono()));
        setText(tipoServidor.getNombre());
        return this;
    }
}
