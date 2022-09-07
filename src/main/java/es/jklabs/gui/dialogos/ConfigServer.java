package es.jklabs.gui.dialogos;

import com.amazonaws.regions.Regions;
import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.filter.document.PuertoDocumentoFilter;
import es.jklabs.gui.utilidades.renderer.TipoLoginComboRenderer;
import es.jklabs.gui.utilidades.renderer.TipoServidorComboRenderer;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
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
import java.util.Objects;

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
    private JComboBox<TipoLogin> cbTipoLogin;
    private JTextField txAwsProfile;
    private JPanel panelFormularioServidor;
    private JLabel lbAwsProfile;
    private JLabel lbBbddPassword;
    private JLabel lbRegion;
    private JComboBox<Regions> cbRegion;

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

    private static GridBagConstraints getGridBagConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        return c;
    }

    private JPanel cargarBotoneraFormulario() {
        JPanel panel = new JPanel();
        JButton btnAceptar = new JButton(Mensajes.getMensaje("aceptar"));
        btnAceptar.addActionListener(al -> guardarServidor());
        panel.add(btnAceptar);
        return panel;
    }

    private void establecerValoresFormulario() {
        cbTipo.setSelectedItem(servidor.getTipoServidor());
        txNombre.setText(servidor.getName());
        txIp.setText(servidor.getHost());
        txPuerto.setText(servidor.getPort());
        if (servidor.getTipoLogin() == null) {
            cbTipoLogin.setSelectedItem(TipoLogin.USUARIO_CONTRASENA);
        } else {
            cbTipoLogin.setSelectedItem(servidor.getTipoLogin());
        }
        txBbddUser.setText(servidor.getUser());
        seleccionarTipoLogin();
        if (Objects.equals(servidor.getTipoLogin(), TipoLogin.AWS_PROFILE)) {
            txAwsProfile.setText(servidor.getAwsProfile());
            cbRegion.setSelectedItem(servidor.getRegion());
        } else {
            txBbddPasword.setText(UtilidadesEncryptacion.decrypt(servidor.getPass()));
        }
        txDataBase.setText(servidor.getDataBase());
        txExclusion.setText(StringUtils.join(servidor.getEsquemasExcluidos(), ","));
    }

    private void guardarServidor() {
        try {
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
                servidor.setTipoLogin((TipoLogin) cbTipoLogin.getSelectedItem());
                servidor.setUser(txBbddUser.getText());
                if (Objects.equals(cbTipoLogin.getSelectedItem(), TipoLogin.USUARIO_CONTRASENA)) {
                    servidor.setPass(UtilidadesEncryptacion.encrypt(String.valueOf(txBbddPasword.getPassword())));
                }
                if (Objects.equals(cbTipoLogin.getSelectedItem(), TipoLogin.AWS_PROFILE)) {
                    servidor.setRegion((Regions) cbRegion.getSelectedItem());
                    servidor.setAwsProfile(txAwsProfile.getText());
                }
                List<String> esquemas = new ArrayList<>();
                Arrays.asList(txExclusion.getText().split(",")).forEach(s -> esquemas.add(s.trim()));
                servidor.setEsquemasExcluidos(esquemas);
                UtilidadesConfiguracion.guardar(mainUI.getConfiguracion());
                this.dispose();
                mainUI.actualizarServidor(servidor);
            }
        } catch (Exception e) {
            Growls.mostrarError("guardar.configuracion", e);
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
        if (Objects.equals(cbTipoLogin.getSelectedItem(), TipoLogin.USUARIO_CONTRASENA) &&
                UtilidadesString.isEmpty(txBbddPasword)) {
            valido = false;
            Growls.mostrarAviso(ANADIR_SERVIDOR, "password.vacio");
        }
        if (Objects.equals(cbTipoLogin.getSelectedItem(), TipoLogin.AWS_PROFILE) &&
                UtilidadesString.isEmpty(txAwsProfile)) {
            valido = false;
            Growls.mostrarAviso(ANADIR_SERVIDOR, "perfil.aws.vacio");
        }
        return valido;
    }

    private JPanel cargarFormulario() {
        panelFormularioServidor = new JPanel(new GridBagLayout());
        GridBagConstraints c = getGridBagConstraints();
        JLabel lbTipo = new JLabel(Mensajes.getMensaje("tipo"));
        c.gridx = 0;
        c.gridy = 0;
        panelFormularioServidor.add(lbTipo, c);
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
        JLabel lbTipoLogin = new JLabel(Mensajes.getMensaje("tipo.login"));
        c.gridx = 0;
        c.gridy = 5;
        panelFormularioServidor.add(lbTipoLogin, c);
        cbTipoLogin = new JComboBox<>(TipoLogin.values());
        cbTipoLogin.setRenderer(new TipoLoginComboRenderer());
        cbTipoLogin.addActionListener(l -> seleccionarTipoLogin());
        c.gridx = 1;
        c.gridy = 5;
        panelFormularioServidor.add(cbTipoLogin, c);
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
        JLabel lbExclusion = new JLabel(Mensajes.getMensaje("esquemas.excluidos"));
        c.gridx = 0;
        c.gridy = 9;
        panelFormularioServidor.add(lbExclusion, c);
        txExclusion = new JTextField();
        txExclusion.setColumns(100);
        c.gridx = 1;
        c.gridy = 9;
        panelFormularioServidor.add(txExclusion, c);
        return panelFormularioServidor;
    }

    private void seleccionarTipoLogin() {
        GridBagConstraints c = getGridBagConstraints();
        if (Objects.equals(cbTipoLogin.getSelectedItem(), TipoLogin.USUARIO_CONTRASENA)) {
            if (lbRegion != null) {
                panelFormularioServidor.remove(lbRegion);
            }
            if (cbRegion != null) {
                panelFormularioServidor.remove(cbRegion);
                cbRegion.setSelectedItem(null);
            }
            if (lbAwsProfile != null) {
                panelFormularioServidor.remove(lbAwsProfile);
            }
            if (txAwsProfile != null) {
                panelFormularioServidor.remove(txAwsProfile);
                txAwsProfile.setText(null);
            }
            if (lbBbddPassword == null) {
                lbBbddPassword = new JLabel(Mensajes.getMensaje("contrasena"));
            }
            c.gridx = 0;
            c.gridy = 7;
            panelFormularioServidor.add(lbBbddPassword, c);
            if (txBbddPasword == null) {
                txBbddPasword = new JPasswordField();
                txBbddPasword.setColumns(10);
            }
            c.gridx = 1;
            c.gridy = 7;
            panelFormularioServidor.add(txBbddPasword, c);
            servidor.setRegion(null);
            servidor.setAwsProfile(null);
        } else if (Objects.equals(cbTipoLogin.getSelectedItem(), TipoLogin.AWS_PROFILE)) {
            if (lbBbddPassword != null) {
                panelFormularioServidor.remove(lbBbddPassword);
            }
            if (txBbddPasword != null) {
                panelFormularioServidor.remove(txBbddPasword);
                txBbddPasword.setText(null);
            }
            if (lbRegion == null) {
                lbRegion = new JLabel(Mensajes.getMensaje("region"));
            }
            c.gridx = 0;
            c.gridy = 7;
            panelFormularioServidor.add(lbRegion, c);
            if (cbRegion == null) {
                cbRegion = new JComboBox<>(Regions.values());
            }
            c.gridx = 1;
            c.gridy = 7;
            panelFormularioServidor.add(cbRegion, c);
            if (lbAwsProfile == null) {
                lbAwsProfile = new JLabel(Mensajes.getMensaje("perfil"));
            }
            c.gridx = 0;
            c.gridy = 8;
            panelFormularioServidor.add(lbAwsProfile, c);
            if (txAwsProfile == null) {
                txAwsProfile = new JTextField();
                txAwsProfile.setColumns(10);
            }
            c.gridx = 1;
            c.gridy = 8;
            panelFormularioServidor.add(txAwsProfile, c);
            servidor.setPass(null);
        }
        this.pack();
    }
}
