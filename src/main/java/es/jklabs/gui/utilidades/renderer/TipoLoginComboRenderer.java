package es.jklabs.gui.utilidades.renderer;

import es.jklabs.json.configuracion.TipoLogin;

import javax.swing.*;
import java.awt.*;

public class TipoLoginComboRenderer extends JLabel implements javax.swing.ListCellRenderer<TipoLogin> {
    @Override
    public Component getListCellRendererComponent(JList<? extends TipoLogin> jList, TipoLogin tipoLogin, int i, boolean b, boolean b1) {
        setText(tipoLogin.getDescripcion());
        return this;
    }
}
