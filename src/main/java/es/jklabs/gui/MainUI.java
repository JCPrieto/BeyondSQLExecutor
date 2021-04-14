package es.jklabs.gui;

import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.utilidades.Constantes;

import javax.swing.*;
import java.util.Objects;

public class MainUI extends JFrame {
    private final Configuracion configuracion;

    public MainUI(Configuracion configuracion) {
        super(Constantes.NOMBRE_APP);
        this.configuracion = configuracion;
        super.setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource
                ("img/icons/database.png"))).getImage());
        super.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        cargarMenu();
        cargarPantallaPrincipal();
    }

    private void cargarPantallaPrincipal() {
        //ToDO
    }

    private void cargarMenu() {
        //ToDo
    }
}
