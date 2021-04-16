package es.jklabs.gui.dialogos;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.filtro.PuertoDocumentoFilter;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesConfiguracion;
import es.jklabs.utilidades.UtilidadesEncryptacion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.PlainDocument;
import java.awt.*;

public class ConfigServer extends JDialog {
    private final Configuracion configuracion;
    private JTextField txNombre;
    private JTextField txIp;
    private JTextField txPuerto;
    private JTextField txBbddUser;
    private JPasswordField txBbddPasword;
    private JTextField txDataBase;
    private JComboBox<TipoServidor> cbTipo;
    private Servidor servidor;

    public ConfigServer(MainUI mainUI) {
        super(mainUI, Mensajes.getMensaje("add.server"));
        configuracion = mainUI.getConfiguracion();
        cargarDatos();
        this.pack();
    }

    private void cargarDatos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(cargarFormulario(), BorderLayout.CENTER);
        panel.add(cargarBotoneraFormulario(), BorderLayout.SOUTH);
        super.add(panel);
    }

    private JPanel cargarBotoneraFormulario() {
        JPanel panel = new JPanel();
        JButton btnAceptar = new JButton(Mensajes.getMensaje("aceptar"));
        btnAceptar.addActionListener(al -> guardarServidor());
        panel.add(btnAceptar);
        return panel;
    }

    private void guardarServidor() {
        if (servidor == null) {
            servidor = new Servidor();
            configuracion.getServers().add(servidor);
        }
        servidor.setTipoServidor((TipoServidor) cbTipo.getSelectedItem());
        servidor.setName(txNombre.getText());
        servidor.setHost(txIp.getText());
        servidor.setPort(txPuerto.getText());
        servidor.setDataBase(txDataBase.getText());
        servidor.setUser(txBbddUser.getText());
        try {
            servidor.setPass(UtilidadesEncryptacion.encrypt(String.valueOf(txBbddPasword.getPassword())));
            UtilidadesConfiguracion.guardar(configuracion);
        } catch (Exception e) {
            Growls.mostrarError("guardar.configuracion", e);
        }
    }

    private JPanel cargarFormulario() {
        JPanel panelFormularioServidor = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JLabel lbRegion = new JLabel(Mensajes.getMensaje("tipo"));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        panelFormularioServidor.add(lbRegion, c);
        cbTipo = new JComboBox<>(TipoServidor.values());
        c.gridx = 1;
        c.gridy = 0;
        panelFormularioServidor.add(cbTipo, c);
        JLabel lbNombre = new JLabel(Mensajes.getMensaje("nombre"));
        c.gridx = 0;
        c.gridy = 1;
        panelFormularioServidor.add(lbNombre, c);
        txNombre = new JTextField();
        txNombre.setColumns(10);
        c.gridx = 1;
        c.gridy = 1;
        panelFormularioServidor.add(txNombre, c);
        JLabel lbIp = new JLabel(Mensajes.getMensaje("host"));
        c.gridx = 0;
        c.gridy = 2;
        panelFormularioServidor.add(lbIp, c);
        txIp = new JTextField();
        txIp.setColumns(100);
        c.gridx = 1;
        c.gridy = 2;
        panelFormularioServidor.add(txIp, c);
        JLabel lbPuerto = new JLabel(Mensajes.getMensaje("puerto"));
        c.gridx = 0;
        c.gridy = 3;
        panelFormularioServidor.add(lbPuerto, c);
        txPuerto = new JTextField();
        ((PlainDocument) txPuerto.getDocument()).setDocumentFilter(new PuertoDocumentoFilter());
        txPuerto.setColumns(3);
        c.gridx = 1;
        c.gridy = 3;
        panelFormularioServidor.add(txPuerto, c);
        JLabel database = new JLabel(Mensajes.getMensaje("base.datos"));
        c.gridx = 0;
        c.gridy = 4;
        panelFormularioServidor.add(database, c);
        txDataBase = new JTextField();
        txDataBase.setColumns(10);
        c.gridx = 1;
        c.gridy = 4;
        panelFormularioServidor.add(txDataBase, c);
        JLabel lbBbddUser = new JLabel(Mensajes.getMensaje("usuario"));
        c.gridx = 0;
        c.gridy = 6;
        c.anchor = GridBagConstraints.LINE_START;
        panelFormularioServidor.add(lbBbddUser, c);
        txBbddUser = new JTextField();
        txBbddUser.setColumns(10);
        c.gridx = 1;
        c.gridy = 6;
        panelFormularioServidor.add(txBbddUser, c);
        JLabel lbBbddPassword = new JLabel(Mensajes.getMensaje("contrasena"));
        c.gridx = 0;
        c.gridy = 7;
        panelFormularioServidor.add(lbBbddPassword, c);
        txBbddPasword = new JPasswordField();
        txBbddPasword.setColumns(10);
        c.gridx = 1;
        c.gridy = 7;
        panelFormularioServidor.add(txBbddPasword, c);
        return panelFormularioServidor;
    }
}
