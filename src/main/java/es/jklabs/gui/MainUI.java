package es.jklabs.gui;

import es.jklabs.gui.dialogos.AcercaDe;
import es.jklabs.gui.panels.ScriptPanel;
import es.jklabs.gui.panels.ServersPanel;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.filter.JSonFilter;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesConfiguracion;
import es.jklabs.utilidades.UtilidadesFirebase;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MainUI extends JFrame {
    private Configuracion configuracion;
    private ServersPanel serverPanel;
    private JMenu jmArchivo;
    private ScriptPanel scriptPanel;
    private JMenu jmAyuda;

    public MainUI(Configuracion configuracion) {
        super(Constantes.NOMBRE_APP);
        this.configuracion = Objects.requireNonNullElseGet(configuracion, Configuracion::new);
        super.setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource
                ("img/icons/database.png"))).getImage());
        super.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        cargarMenu();
        cargarPantallaPrincipal();
    }

    private void cargarPantallaPrincipal() {
        super.setLayout(new BorderLayout(10, 10));
        serverPanel = new ServersPanel(this);
        super.add(serverPanel, BorderLayout.WEST);
        scriptPanel = new ScriptPanel(serverPanel);
        super.add(scriptPanel, BorderLayout.CENTER);
        serverPanel.loadEsquemas();
    }

    private void cargarMenu() {
        JMenuBar menu = new JMenuBar();
        jmArchivo = new JMenu(Mensajes.getMensaje("archivo"));
        jmArchivo.setMargin(new Insets(5, 5, 5, 5));
        JMenuItem jmiExportar = new JMenuItem(Mensajes.getMensaje("exportar.configuracion"), new ImageIcon(Objects
                .requireNonNull(getClass().getClassLoader().getResource("img/icons/download.png"))));
        jmiExportar.addActionListener(al -> exportarConfiguracion());
        JMenuItem jmiImportar = new JMenuItem(Mensajes.getMensaje("importar.configuracion"), new ImageIcon(Objects
                .requireNonNull(getClass().getClassLoader().getResource("img/icons/upload.png"))));
        jmiImportar.addActionListener(al -> importarConfiguracion());
        jmArchivo.add(jmiExportar);
        jmArchivo.add(jmiImportar);
        jmAyuda = new JMenu(Mensajes.getMensaje("ayuda"));
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
        } catch (InterruptedException e) {
            Growls.mostrarError("consultar.nueva.version", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Growls.mostrarError("consultar.nueva.version", e);
        }
        super.setJMenuBar(menu);
    }

    private void importarConfiguracion() {
        JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new JSonFilter());
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int retorno = fc.showOpenDialog(this);
        if (retorno == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                Configuracion nuevos = UtilidadesConfiguracion.loadConfig(file);
                nuevos.getServers().forEach(this::actualizarServidor);
            } catch (IOException e) {
                Growls.mostrarError(Mensajes.getError("importar.configuracion"), e);
            }
        }
    }

    private void exportarConfiguracion() {
        JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new JSonFilter());
        fc.setAcceptAllFileFilterUsed(false);
        int retorno = fc.showSaveDialog(this);
        if (retorno == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (!Objects.equals(FilenameUtils.getExtension(file.getName()), "json")) {
                file = new File(file + ".json");
            }
            try {
                UtilidadesConfiguracion.guardarConfiguracion(configuracion, file);
            } catch (IOException e) {
                Growls.mostrarError(Mensajes.getError("exportar.configuracion"), e);
            }
        }
    }

    private void descargarNuevaVersion() {
        try {
            UtilidadesFirebase.descargaNuevaVersion(this);
        } catch (InterruptedException e) {
            Growls.mostrarError("descargar.nueva.version", e);
            Thread.currentThread().interrupt();
        }
    }

    private void mostrarAcercaDe() {
        AcercaDe acercaDe = new AcercaDe(this);
        acercaDe.setVisible(true);
    }

    public Configuracion getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(Configuracion configuracion) {
        this.configuracion = configuracion;
    }

    public void actualizarServidor(Servidor servidor) {
        serverPanel.actualizarServidor(servidor);
    }

    public void eliminar(Servidor servidor) {
        try {
            configuracion.getServers().remove(servidor);
            UtilidadesConfiguracion.guardar(configuracion);
            serverPanel.eliminar(servidor);
        } catch (IOException e) {
            Growls.mostrarError("guardar.configuracion", e);
        }
    }

    public void desbloquearPantalla() {
        setCursor(null); //turn off the wait cursor
        jmArchivo.setEnabled(true);
        jmAyuda.setEnabled(true);
        serverPanel.desbloquearPantalla();
        scriptPanel.desbloquearPantalla();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void bloquearPantalla() {
        Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        setCursor(waitCursor);
        jmArchivo.setEnabled(false);
        jmAyuda.setEnabled(false);
        serverPanel.bloquearPantalla();
        scriptPanel.bloquearPantalla(waitCursor);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }
}
