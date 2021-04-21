package es.jklabs.gui.utilidades;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class UtilidadesImagenes {

    private UtilidadesImagenes() {

    }

    public static ImageIcon getIcono(String nombre) {
        ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(UtilidadesImagenes.class.getClassLoader().getResource
                ("img/icons/" + nombre)));
        Image img = imageIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

}
