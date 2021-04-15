package es.jklabs.gui;

import es.jklabs.gui.dialogos.AcercaDe;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesFirebase;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
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
        JMenuBar menu = new JMenuBar();
        JMenu jmArchivo = new JMenu(Mensajes.getMensaje("archivo"));
        jmArchivo.setMargin(new Insets(5, 5, 5, 5));
        JMenuItem jmiConfiguracion = new JMenuItem(Mensajes.getMensaje("configuracion"), new ImageIcon(Objects
                .requireNonNull(getClass().getClassLoader().getResource("img/icons/settings.png"))));
        jmiConfiguracion.addActionListener(al -> abrirConfiguracion());
        jmArchivo.add(jmiConfiguracion);
        JMenu jmAyuda = new JMenu(Mensajes.getMensaje("ayuda"));
        jmAyuda.setMargin(new Insets(5, 5, 5, 5));
        JMenuItem jmiAcercaDe = new JMenuItem(Mensajes.getMensaje("acerca.de"), new ImageIcon(Objects
                .requireNonNull(getClass().getClassLoader().getResource("img/icons/info.png"))));
        jmiAcercaDe.addActionListener(al -> mostrarAcercaDe());
        jmAyuda.add(jmiAcercaDe);
        menu.add(jmArchivo);
        menu.add(jmAyuda);
        try {
            if (UtilidadesFirebase.existeNuevaVersion()) {
                menu.add(Box.createHorizontalGlue());
                JMenuItem jmActualizacion = new JMenuItem(Mensajes.getMensaje("existe.nueva.version"), new ImageIcon
                        (Objects.requireNonNull(getClass().getClassLoader().getResource("img/icons/update.png"))));
                jmActualizacion.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                jmActualizacion.setHorizontalTextPosition(SwingConstants.RIGHT);
                jmActualizacion.addActionListener(al -> descargarNuevaVersion());
                menu.add(jmActualizacion);
            }
        } catch (IOException e) {
            Logger.error("consultar.nueva.version", e);
        } catch (InterruptedException e) {
            Logger.error("consultar.nueva.version", e);
            Thread.currentThread().interrupt();
        }
        super.setJMenuBar(menu);
    }

    private void descargarNuevaVersion() {

    }

    private void mostrarAcercaDe() {
        AcercaDe acercaDe = new AcercaDe(this);
        acercaDe.setVisible(true);
    }

    private void abrirConfiguracion() {

    }
}
