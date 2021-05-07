package es.jklabs.gui.dialogos;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.listener.UrlMouseListener;
import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Mensajes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class AcercaDe extends JDialog {

    public AcercaDe(MainUI mainUI) {
        super(mainUI, Mensajes.getMensaje("acerca.de"), true);
        cargarPantalla();
    }

    private void cargarPantalla() {
        final JPanel panel = new JPanel();
        int yPosition = 0;
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        final GridBagConstraints cns = new GridBagConstraints();
        final JLabel jLabelTitle = new JLabel(
                "<html><h1>" + Constantes.NOMBRE_APP + " " + Constantes.VERSION + "</h1></html>", new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource
                ("img/icons/database.png"))), SwingConstants.CENTER);
        cns.fill = GridBagConstraints.HORIZONTAL;
        cns.insets = new Insets(10, 10, 10, 10);
        cns.gridx = 0;
        cns.gridy = yPosition++;
        cns.gridwidth = 3;
        panel.add(jLabelTitle, cns);
        final JLabel jLabelCreadoPor = new JLabel(Mensajes.getMensaje("creado.por"), SwingConstants.LEFT);
        cns.insets = new Insets(10, 10, 3, 10);
        cns.gridy = yPosition++;
        cns.gridwidth = 1;
        panel.add(jLabelCreadoPor, cns);
        final JLabel jLabelMyName = new JLabel("<html><b>Juan Carlos Prieto Silos</b></html>", SwingConstants.LEFT);
        cns.insets = new Insets(3, 10, 3, 10);
        cns.gridy = yPosition++;
        panel.add(jLabelMyName, cns);
        final JLabel jLabelMyWeb = new JLabel("JCPrieto.cf", SwingConstants.LEFT);
        jLabelMyWeb.addMouseListener(new UrlMouseListener(jLabelMyWeb, "https://jcprieto.cf"));
        cns.gridx = 1;
        panel.add(jLabelMyWeb, cns);
        JLabel jLabelMyMail = new JLabel("JuanC.Prieto.Silos@gmail.com", SwingConstants.LEFT);
        jLabelMyMail.setAlignmentX(CENTER_ALIGNMENT);
        jLabelMyMail.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(
                            "mailto:JuanC.Prieto.Silos@gmail.com?subject=BeyondSQLExecutor"));
                } catch (IOException | URISyntaxException e1) {
                    Growls.mostrarError("acerca.de", "app.envio.correo", e1);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Empty
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Empty
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                jLabelMyMail.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jLabelMyMail.setCursor(null);
            }
        });
        cns.gridx = 2;
        panel.add(jLabelMyMail, cns);
        final JLabel jLabelPoweredBy = new JLabel(Mensajes.getMensaje("powered.by"), SwingConstants.LEFT);
        cns.insets = new Insets(10, 10, 3, 10);
        cns.gridx = 0;
        yPosition++;
        cns.gridy = yPosition++;
        panel.add(jLabelPoweredBy, cns);
        addPowered(panel, cns, yPosition++, "Papirus", "https://github.com/PapirusDevelopmentTeam/papirus-icon-theme");
        addPowered(panel, cns, yPosition++, "Apache Commons", "https://commons.apache.org");
        addPowered(panel, cns, yPosition++, "GSon", "https://github.com/google/gson");
        addPowered(panel, cns, yPosition++, "Firebase", "https://firebase.google.com");
        addPowered(panel, cns, yPosition++, "JAXB", "https://github.com/javaee/jaxb-v2");
        addPowered(panel, cns, yPosition++, "MySQL", "https://www.mysql.com/");
        addPowered(panel, cns, yPosition++, "MariaDB", "https://mariadb.org/");
        addPowered(panel, cns, yPosition++, "PostgreSQL", "https://www.postgresql.org/");
        JLabel jLabelLicense = new JLabel
                ("<html><i>Esta obra está bajo una licencia de Creative Commons " +
                        "Reconocimiento-NoComercial-CompartirIgual 4.0 Internacional</i><html>", new ImageIcon(Objects
                        .requireNonNull(getClass().getClassLoader().getResource
                                ("img/icons/creative_commons.png"))), SwingConstants.TRAILING);
        jLabelLicense.addMouseListener(new UrlMouseListener(jLabelLicense, "http://creativecommons.org/licenses/by-nc-sa/4.0/"));
        cns.insets = new Insets(10, 10, 10, 10);
        cns.gridx = 0;
        cns.gridy = yPosition++;
        cns.gridwidth = 3;
        panel.add(jLabelLicense, cns);
        JButton botonOk = new JButton(Mensajes.getMensaje("aceptar"));
        botonOk.setToolTipText(Mensajes.getMensaje("aceptar"));
        botonOk.addActionListener(al -> pressAceptar());
        cns.gridy = yPosition;
        panel.add(botonOk, cns);
        super.add(panel);
        super.pack();
    }

    private void pressAceptar() {
        this.dispose();
    }

    private void addPowered(JPanel panel, GridBagConstraints cns, int y, String titulo, String url) {
        JLabel jLabelTitulo = new JLabel("<html><b>" + titulo + "</b></html>", SwingConstants.LEFT);
        if (url != null) {
            jLabelTitulo.addMouseListener(new UrlMouseListener(jLabelTitulo, url));
        }
        cns.insets = new Insets(3, 10, 3, 10);
        cns.gridx = 0;
        cns.gridy = y;
        cns.gridwidth = 1;
        panel.add(jLabelTitulo, cns);
        if (url != null) {
            JLabel jLabelUrl = new JLabel(url, SwingConstants.LEFT);
            jLabelUrl.addMouseListener(new UrlMouseListener(jLabelUrl, url));
            cns.gridx = 1;
            cns.gridy = y;
            cns.gridwidth = 2;
            panel.add(jLabelUrl, cns);
        }
    }
}
