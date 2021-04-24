package es.jklabs.gui.dialogos;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.filtro.PuertoDocumentoFilter;
import es.jklabs.gui.utilidades.renderer.TipoServidorComboRenderer;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesConfiguracion;
import es.jklabs.utilidades.UtilidadesEncryptacion;
import es.jklabs.utilidades.UtilidadesString;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigServer extends JDialog {

    private static final String ANADIR_SERVIDOR = "anadir.servidor";
    private final MainUI mainUI;
    private JTextField txNombre;
    private JTextField txIp;
    private JTextField txPuerto;
    private JTextField txBbddUser;
    private JPasswordField txBbddPasword;
    private JTextField txDataBase;
    private JComboBox<TipoServidor> cbTipo;
    private Servidor servidor;
    private JTextField txExclusion;

    public ConfigServer(MainUI mainUI) {
        this(mainUI, null);
    }

    public ConfigServer(MainUI mainUI, Servidor servidor) {
        super(mainUI, Mensajes.getMensaje("anadir.servidor"));
        this.mainUI = mainUI;
        this.servidor = servidor;
        cargarDatos();
        this.pack();
    }

    private void cargarDatos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(cargarFormulario(), BorderLayout.CENTER);
        if (servidor != null) {
            establecerValoresFormulario();
        }
        panel.add(cargarBotoneraFormulario(), BorderLayout.SOUTH);
        super.add(panel);
    }

    private void establecerValoresFormulario() {
        cbTipo.setSelectedItem(servidor.getTipoServidor());
        txNombre.setText(servidor.getName());
        txIp.setText(servidor.getHost());
        txPuerto.setText(servidor.getPort());
        txBbddUser.setText(servidor.getUser());
        txBbddPasword.setText(UtilidadesEncryptacion.decrypt(servidor.getPass()));
        txDataBase.setText(servidor.getDataBase());
        txExclusion.setText(StringUtils.join(servidor.getEsquemasExcluidos(), ","));
    }

    private JPanel cargarBotoneraFormulario() {
        JPanel panel = new JPanel();
        JButton btnAceptar = new JButton(Mensajes.getMensaje("aceptar"));
        btnAceptar.addActionListener(al -> guardarServidor());
        panel.add(btnAceptar);
        return panel;
    }

    private void guardarServidor() {
        if (validarFormulario()) {
            if (servidor == null) {
                servidor = new Servidor();
                mainUI.getConfiguracion().getServers().add(servidor);
            }
            servidor.setTipoServidor((TipoServidor) cbTipo.getSelectedItem());
            servidor.setName(txNombre.getText());
            servidor.setHost(txIp.getText());
            servidor.setPort(txPuerto.getText());
            servidor.setDataBase(txDataBase.getText());
            servidor.setUser(txBbddUser.getText());
            List<String> esquemas = new ArrayList<>();
            Arrays.asList(txExclusion.getText().split(",")).forEach(s -> esquemas.add(s.trim()));
            servidor.setEsquemasExcluidos(esquemas);
            try {
                servidor.setPass(UtilidadesEncryptacion.encrypt(String.valueOf(txBbddPasword.getPassword())));
                UtilidadesConfiguracion.guardar(mainUI.getConfiguracion());
                this.dispose();
                mainUI.actualizarServidor(servidor);
            } catch (Exception e) {
                Growls.mostrarError("guardar.configuracion", e);
            }
        }
    }

    private boolean validarFormulario() {
        boolean valido = true;
        if (UtilidadesString.isEmpty(txNombre)) {
            valido = false;
            Growls.mostrarAviso(ANADIR_SERVIDOR, "nombre.servidor.vacio");
        }
        if (UtilidadesString.isEmpty(txIp)) {
            valido = false;
            Growls.mostrarAviso(ANADIR_SERVIDOR, "ip.servidor.vacio");
        }
        if (UtilidadesString.isEmpty(txPuerto)) {
            valido = false;
            Growls.mostrarAviso(ANADIR_SERVIDOR, "puerto.servidor.vacio");
        }
        if (UtilidadesString.isEmpty(txBbddUser)) {
            valido = false;
            Growls.mostrarAviso(ANADIR_SERVIDOR, "usuario.vacio");
        }
        if (UtilidadesString.isEmpty(txBbddPasword)) {
            valido = false;
            Growls.mostrarAviso(ANADIR_SERVIDOR, "password.vacio");
        }
        return valido;
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
        cbTipo.setRenderer(new TipoServidorComboRenderer());
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
        JLabel lbExclusion = new JLabel(Mensajes.getMensaje("esquemas.excluidos"));
        c.gridx = 0;
        c.gridy = 8;
        panelFormularioServidor.add(lbExclusion, c);
        txExclusion = new JTextField();
        txExclusion.setColumns(100);
        c.gridx = 1;
        c.gridy = 8;
        panelFormularioServidor.add(txExclusion, c);
        return panelFormularioServidor;
    }
}
