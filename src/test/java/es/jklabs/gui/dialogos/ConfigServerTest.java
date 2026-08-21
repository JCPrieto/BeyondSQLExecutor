package es.jklabs.gui.dialogos;

import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import sun.misc.Unsafe;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServerTest {

    private static <T> T allocateInstance(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        return type.cast(unsafe.allocateInstance(type));
    }

    private static ConfigServer allocateInstance() throws Exception {
        return allocateInstance(ConfigServer.class);
    }

    private static ConfigServer allocateHeadlessDialog() throws Exception {
        return allocateInstance(HeadlessConfigServer.class);
    }

    private static Object invokePrivateMethod(Object target, String name, Class<?>[] parameterTypes, Object[] args)
            throws Exception {
        Method method = ConfigServer.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = ConfigServer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object invokePrivateStaticMethod() throws Exception {
        Method method = ConfigServer.class.getDeclaredMethod("getGridBagConstraints");
        method.setAccessible(true);
        return method.invoke(null);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = ConfigServer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static ConfigServer createPopulatedHeadlessDialog(Servidor servidor) throws Exception {
        ConfigServer dialog = allocateHeadlessDialog();
        setField(dialog, "servidor", servidor);
        setField(dialog, "panelFormularioServidor", new JPanel(new GridBagLayout()));
        setField(dialog, "cbTipo", new JComboBox<>(TipoServidor.values()));
        setField(dialog, "txNombre", new JTextField());
        setField(dialog, "txIp", new JTextField());
        setField(dialog, "txPuerto", new JTextField());
        setField(dialog, "cbTipoLogin", new JComboBox<>(TipoLogin.values()));
        setField(dialog, "txBbddUser", new JTextField());
        setField(dialog, "txDataBase", new JTextField());
        setField(dialog, "txExclusion", new JTextField());
        return dialog;
    }

    private static ConfigServer createValidationDialog(TipoLogin tipoLogin) throws Exception {
        ConfigServer dialog = allocateInstance();
        setField(dialog, "txNombre", new JTextField());
        setField(dialog, "txIp", new JTextField());
        setField(dialog, "txPuerto", new JTextField());
        setField(dialog, "txBbddUser", new JTextField());
        setField(dialog, "txBbddPasword", new JPasswordField());
        setField(dialog, "txAwsProfile", new JTextField());
        JComboBox<TipoLogin> cbTipoLogin = new JComboBox<>(TipoLogin.values());
        cbTipoLogin.setSelectedItem(tipoLogin);
        setField(dialog, "cbTipoLogin", cbTipoLogin);
        return dialog;
    }

    @Test
    void loadExecuteWithRolAddsReusesAndRemovesPostgresqlFields() throws Exception {
        ConfigServer dialog = allocateHeadlessDialog();
        JPanel panel = new JPanel(new GridBagLayout());
        JComboBox<TipoServidor> cbTipo = new JComboBox<>(TipoServidor.values());
        cbTipo.setSelectedItem(TipoServidor.POSTGRESQL);
        setField(dialog, "panelFormularioServidor", panel);
        setField(dialog, "cbTipo", cbTipo);

        invokePrivateMethod(dialog, "loadExecuteWithRol", new Class[0], new Object[0]);

        JCheckBox checkRol = (JCheckBox) getField(dialog, "checkRol");
        JLabel lbRol = (JLabel) getField(dialog, "lbRol");
        JTextField txPostgresRol = (JTextField) getField(dialog, "txPostgresRol");
        assertNotNull(checkRol);
        assertNotNull(lbRol);
        assertNotNull(txPostgresRol);
        assertFalse(txPostgresRol.isEditable());

        invokePrivateMethod(dialog, "loadExecuteWithRol", new Class[0], new Object[0]);

        assertEquals(1, countOccurrences(panel, checkRol));
        assertEquals(1, countOccurrences(panel, lbRol));
        assertEquals(1, countOccurrences(panel, txPostgresRol));

        checkRol.setSelected(true);
        txPostgresRol.setText("app_role");
        cbTipo.setSelectedItem(TipoServidor.MYSQL);
        invokePrivateMethod(dialog, "loadExecuteWithRol", new Class[0], new Object[0]);

        assertFalse(checkRol.isSelected());
        assertEquals("", txPostgresRol.getText());
        assertFalse(Arrays.asList(panel.getComponents()).contains(checkRol));
        assertFalse(Arrays.asList(panel.getComponents()).contains(lbRol));
        assertFalse(Arrays.asList(panel.getComponents()).contains(txPostgresRol));
    }

    private static long countOccurrences(JPanel panel, Component component) {
        return Arrays.stream(panel.getComponents()).filter(component::equals).count();
    }

    @Test
    void getGridBagConstraintsReturnsExpectedDefaults() throws Exception {
        GridBagConstraints constraints = (GridBagConstraints) invokePrivateStaticMethod();

        assertEquals(1, constraints.gridwidth);
        assertEquals(1, constraints.gridheight);
        assertEquals(new Insets(5, 5, 5, 5), constraints.insets);
        assertEquals(GridBagConstraints.LINE_START, constraints.anchor);
    }

    @Test
    void seleccionarLoginPasswordRemovesAwsFieldsAndAddsPasswordFields() throws Exception {
        ConfigServer dialog = allocateInstance();
        JPanel panel = new JPanel(new GridBagLayout());

        JLabel lbRegion = new JLabel("region");
        JComboBox<Region> cbRegion = new JComboBox<>(new Region[]{Region.EU_WEST_1});
        cbRegion.setSelectedItem(Region.EU_WEST_1);
        JLabel lbAwsProfile = new JLabel("aws profile");
        JTextField txAwsProfile = new JTextField();
        txAwsProfile.setText("dev-profile");
        panel.add(lbRegion);
        panel.add(cbRegion);
        panel.add(lbAwsProfile);
        panel.add(txAwsProfile);

        setField(dialog, "panelFormularioServidor", panel);
        setField(dialog, "lbRegion", lbRegion);
        setField(dialog, "cbRegion", cbRegion);
        setField(dialog, "lbAwsProfile", lbAwsProfile);
        setField(dialog, "txAwsProfile", txAwsProfile);

        invokePrivateMethod(dialog, "seleccionarLoginPassword", new Class[]{GridBagConstraints.class},
                new Object[]{new GridBagConstraints()});

        JLabel lbBbddPassword = (JLabel) getField(dialog, "lbBbddPassword");
        JPasswordField txBbddPasword = (JPasswordField) getField(dialog, "txBbddPasword");

        assertNotNull(lbBbddPassword);
        assertNotNull(txBbddPasword);
        assertNull(cbRegion.getSelectedItem(), "Expected region selection to be cleared.");
        assertEquals("", txAwsProfile.getText(), "Expected AWS profile text to be cleared.");
        assertTrue(Arrays.asList(panel.getComponents()).contains(lbBbddPassword), "Password label should be added.");
        assertTrue(Arrays.asList(panel.getComponents()).contains(txBbddPasword), "Password field should be added.");
        assertFalse(Arrays.asList(panel.getComponents()).contains(lbRegion), "Region label should be removed.");
        assertFalse(Arrays.asList(panel.getComponents()).contains(cbRegion), "Region combo should be removed.");
        assertFalse(Arrays.asList(panel.getComponents()).contains(lbAwsProfile), "AWS profile label should be removed.");
        assertFalse(Arrays.asList(panel.getComponents()).contains(txAwsProfile), "AWS profile field should be removed.");

        invokePrivateMethod(dialog, "seleccionarLoginPassword", new Class[]{GridBagConstraints.class},
                new Object[]{new GridBagConstraints()});

        assertEquals(1, countOccurrences(panel, lbBbddPassword));
        assertEquals(1, countOccurrences(panel, txBbddPasword));
    }

    @Test
    void seleccionarLoginAWSRemovesPasswordFieldAndAddsAwsFields() throws Exception {
        ConfigServer dialog = allocateInstance();
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel lbBbddPassword = new JLabel("password");
        JPasswordField txBbddPasword = new JPasswordField();
        txBbddPasword.setText("secret");
        panel.add(lbBbddPassword);
        panel.add(txBbddPasword);

        setField(dialog, "panelFormularioServidor", panel);
        setField(dialog, "lbBbddPassword", lbBbddPassword);
        setField(dialog, "txBbddPasword", txBbddPasword);

        invokePrivateMethod(dialog, "seleccionarLoginAWS", new Class[]{GridBagConstraints.class},
                new Object[]{new GridBagConstraints()});

        JLabel lbRegion = (JLabel) getField(dialog, "lbRegion");
        JComboBox<?> cbRegion = (JComboBox<?>) getField(dialog, "cbRegion");
        JLabel lbAwsProfile = (JLabel) getField(dialog, "lbAwsProfile");
        JTextField txAwsProfile = (JTextField) getField(dialog, "txAwsProfile");

        assertNotNull(lbRegion);
        assertNotNull(cbRegion);
        assertNotNull(lbAwsProfile);
        assertNotNull(txAwsProfile);
        assertEquals(0, txBbddPasword.getPassword().length, "Password should be wiped from the field.");
        assertFalse(Arrays.asList(panel.getComponents()).contains(lbBbddPassword), "Password label should be removed.");
        assertFalse(Arrays.asList(panel.getComponents()).contains(txBbddPasword), "Password field should be removed.");
        assertTrue(Arrays.asList(panel.getComponents()).contains(lbRegion), "Region label should be added.");
        assertTrue(Arrays.asList(panel.getComponents()).contains(cbRegion), "Region combo should be added.");
        assertTrue(Arrays.asList(panel.getComponents()).contains(lbAwsProfile), "AWS profile label should be added.");
        assertTrue(Arrays.asList(panel.getComponents()).contains(txAwsProfile), "AWS profile field should be added.");
        assertTrue(cbRegion.getItemCount() > 0, "Expected AWS regions to be loaded.");

        invokePrivateMethod(dialog, "seleccionarLoginAWS", new Class[]{GridBagConstraints.class},
                new Object[]{new GridBagConstraints()});

        assertEquals(1, countOccurrences(panel, lbRegion));
        assertEquals(1, countOccurrences(panel, cbRegion));
        assertEquals(1, countOccurrences(panel, lbAwsProfile));
        assertEquals(1, countOccurrences(panel, txAwsProfile));
    }

    @Test
    void seleccionarLoginAWSHandlesMissingPasswordComponents() throws Exception {
        ConfigServer dialog = allocateInstance();
        JPanel panel = new JPanel(new GridBagLayout());
        setField(dialog, "panelFormularioServidor", panel);

        invokePrivateMethod(dialog, "seleccionarLoginAWS", new Class[]{GridBagConstraints.class},
                new Object[]{new GridBagConstraints()});

        assertNotNull(getField(dialog, "lbRegion"));
        assertNotNull(getField(dialog, "cbRegion"));
        assertNotNull(getField(dialog, "lbAwsProfile"));
        assertNotNull(getField(dialog, "txAwsProfile"));
    }

    @Test
    void seleccionarLoginPasswordHandlesMissingAwsComponents() throws Exception {
        ConfigServer dialog = allocateInstance();
        JPanel panel = new JPanel(new GridBagLayout());
        setField(dialog, "panelFormularioServidor", panel);

        invokePrivateMethod(dialog, "seleccionarLoginPassword", new Class[]{GridBagConstraints.class},
                new Object[]{new GridBagConstraints()});

        assertNotNull(getField(dialog, "lbBbddPassword"));
        assertNotNull(getField(dialog, "txBbddPasword"));
    }

    @Test
    void setRolEditableTogglesEditableAndClearsTextWhenDisabled() throws Exception {
        ConfigServer dialog = allocateInstance();
        JCheckBox checkRol = new JCheckBox();
        JTextField txPostgresRol = new JTextField();

        setField(dialog, "checkRol", checkRol);
        setField(dialog, "txPostgresRol", txPostgresRol);

        checkRol.setSelected(true);
        invokePrivateMethod(dialog, "setRolEditable", new Class[0], new Object[0]);
        assertTrue(txPostgresRol.isEditable(), "Role field should be editable when checkbox is selected.");

        txPostgresRol.setText("app_role");
        checkRol.setSelected(false);
        invokePrivateMethod(dialog, "setRolEditable", new Class[0], new Object[0]);
        assertFalse(txPostgresRol.isEditable(), "Role field should be read-only when checkbox is not selected.");
        assertEquals("", txPostgresRol.getText(), "Role field should be cleared when checkbox is not selected.");
    }

    @Test
    void loadExecuteWithRolHandlesNonPostgresqlWithoutExistingFields() throws Exception {
        ConfigServer dialog = allocateHeadlessDialog();
        JComboBox<TipoServidor> cbTipo = new JComboBox<>(TipoServidor.values());
        cbTipo.setSelectedItem(TipoServidor.MARIADB);
        setField(dialog, "panelFormularioServidor", new JPanel(new GridBagLayout()));
        setField(dialog, "cbTipo", cbTipo);

        invokePrivateMethod(dialog, "loadExecuteWithRol", new Class[0], new Object[0]);

        assertNull(getField(dialog, "checkRol"));
        assertNull(getField(dialog, "lbRol"));
        assertNull(getField(dialog, "txPostgresRol"));
    }

    @Test
    void seleccionarTipoLoginCoversPasswordAwsAndNoSelection() throws Exception {
        ConfigServer dialog = allocateHeadlessDialog();
        JPanel panel = new JPanel(new GridBagLayout());
        JComboBox<TipoLogin> cbTipoLogin = new JComboBox<>(TipoLogin.values());
        setField(dialog, "panelFormularioServidor", panel);
        setField(dialog, "cbTipoLogin", cbTipoLogin);

        cbTipoLogin.setSelectedItem(TipoLogin.USUARIO_CONTRASENA);
        invokePrivateMethod(dialog, "seleccionarTipoLogin", new Class[0], new Object[0]);
        assertNotNull(getField(dialog, "txBbddPasword"));

        cbTipoLogin.setSelectedItem(TipoLogin.AWS_PROFILE);
        invokePrivateMethod(dialog, "seleccionarTipoLogin", new Class[0], new Object[0]);
        assertNotNull(getField(dialog, "txAwsProfile"));

        cbTipoLogin.setSelectedItem(null);
        invokePrivateMethod(dialog, "seleccionarTipoLogin", new Class[0], new Object[0]);
        assertNull(cbTipoLogin.getSelectedItem());
    }

    @Test
    void establecerValoresFormularioLoadsLegacyPasswordServerWithoutRole() throws Exception {
        Servidor servidor = new Servidor();
        servidor.setTipoServidor(TipoServidor.MYSQL);
        servidor.setName("Legacy server");
        servidor.setHost("localhost");
        servidor.setPort("3306");
        servidor.setUser("root");
        servidor.setDataBase("inventory");
        servidor.setEsquemasExcluidos(List.of("sys", "audit"));
        ConfigServer dialog = createPopulatedHeadlessDialog(servidor);

        invokePrivateMethod(dialog, "establecerValoresFormulario", new Class[0], new Object[0]);

        assertEquals(TipoLogin.USUARIO_CONTRASENA,
                ((JComboBox<?>) getField(dialog, "cbTipoLogin")).getSelectedItem());
        assertEquals("Legacy server", ((JTextField) getField(dialog, "txNombre")).getText());
        assertEquals("sys,audit", ((JTextField) getField(dialog, "txExclusion")).getText());
        assertNull(getField(dialog, "checkRol"));
    }

    @Test
    void establecerValoresFormularioLoadsAwsPostgresqlServerWithRole() throws Exception {
        Servidor servidor = new Servidor();
        servidor.setTipoServidor(TipoServidor.POSTGRESQL);
        servidor.setTipoLogin(TipoLogin.AWS_PROFILE);
        servidor.setName("AWS server");
        servidor.setHost("database.example.com");
        servidor.setPort("5432");
        servidor.setUser("app");
        servidor.setDataBase("orders");
        servidor.setAwsProfile("production");
        servidor.setAwsRegion(Region.EU_WEST_1);
        servidor.setExecutaAsRol(true);
        servidor.setRol("reporting");
        ConfigServer dialog = createPopulatedHeadlessDialog(servidor);

        invokePrivateMethod(dialog, "establecerValoresFormulario", new Class[0], new Object[0]);

        assertEquals("production", ((JTextField) getField(dialog, "txAwsProfile")).getText());
        assertEquals(Region.EU_WEST_1, ((JComboBox<?>) getField(dialog, "cbRegion")).getSelectedItem());
        assertTrue(((JCheckBox) getField(dialog, "checkRol")).isSelected());
        assertEquals("reporting", ((JTextField) getField(dialog, "txPostgresRol")).getText());
        assertTrue(((JTextField) getField(dialog, "txPostgresRol")).isEditable());
    }

    @Test
    void establecerValoresFormularioLoadsPostgresqlServerWithoutRole() throws Exception {
        Servidor servidor = new Servidor();
        servidor.setTipoServidor(TipoServidor.POSTGRESQL);
        servidor.setTipoLogin(TipoLogin.USUARIO_CONTRASENA);
        servidor.setName("PostgreSQL server");
        servidor.setHost("localhost");
        servidor.setPort("5432");
        servidor.setUser("postgres");
        servidor.setDataBase("postgres");
        servidor.setExecutaAsRol(false);
        ConfigServer dialog = createPopulatedHeadlessDialog(servidor);

        invokePrivateMethod(dialog, "establecerValoresFormulario", new Class[0], new Object[0]);

        assertFalse(((JCheckBox) getField(dialog, "checkRol")).isSelected());
        assertEquals("", ((JTextField) getField(dialog, "txPostgresRol")).getText());
        assertFalse(((JTextField) getField(dialog, "txPostgresRol")).isEditable());
    }

    private static final class HeadlessConfigServer extends ConfigServer {
        private HeadlessConfigServer() {
            super(null);
        }

        @Override
        public void pack() {
            // Avoid creating native UI resources in these headless unit tests.
        }
    }

    @Test
    void validarFormularioReturnsTrueForUserPasswordLoginWithRequiredFields() throws Exception {
        ConfigServer dialog = allocateInstance();

        setField(dialog, "txNombre", new JTextField("Server 1"));
        setField(dialog, "txIp", new JTextField("localhost"));
        setField(dialog, "txPuerto", new JTextField("5432"));
        setField(dialog, "txBbddUser", new JTextField("postgres"));
        setField(dialog, "txBbddPasword", new JPasswordField("secret"));
        setField(dialog, "txAwsProfile", new JTextField());
        JComboBox<TipoLogin> cbTipoLogin = new JComboBox<>(TipoLogin.values());
        cbTipoLogin.setSelectedItem(TipoLogin.USUARIO_CONTRASENA);
        setField(dialog, "cbTipoLogin", cbTipoLogin);

        boolean valido = (boolean) invokePrivateMethod(dialog, "validarFormulario", new Class[0], new Object[0]);

        assertTrue(valido);
    }

    @Test
    void validarFormularioReturnsTrueForAwsProfileLoginWithRequiredFields() throws Exception {
        ConfigServer dialog = allocateInstance();

        setField(dialog, "txNombre", new JTextField("Server AWS"));
        setField(dialog, "txIp", new JTextField("db.example.com"));
        setField(dialog, "txPuerto", new JTextField("3306"));
        setField(dialog, "txBbddUser", new JTextField("admin"));
        setField(dialog, "txBbddPasword", new JPasswordField());
        setField(dialog, "txAwsProfile", new JTextField("default"));
        JComboBox<TipoLogin> cbTipoLogin = new JComboBox<>(TipoLogin.values());
        cbTipoLogin.setSelectedItem(TipoLogin.AWS_PROFILE);
        setField(dialog, "cbTipoLogin", cbTipoLogin);

        boolean valido = (boolean) invokePrivateMethod(dialog, "validarFormulario", new Class[0], new Object[0]);

        assertTrue(valido);
    }

    @Test
    void validarFormularioReportsAllEmptyPasswordLoginFields() throws Exception {
        ConfigServer dialog = createValidationDialog(TipoLogin.USUARIO_CONTRASENA);
        List<String> warnings = new ArrayList<>();

        boolean valido = dialog.validarFormulario((title, body) -> warnings.add(title + ":" + body));

        assertFalse(valido);
        assertEquals(List.of(
                "anadir.servidor:nombre.servidor.vacio",
                "anadir.servidor:ip.servidor.vacio",
                "anadir.servidor:puerto.servidor.vacio",
                "anadir.servidor:usuario.vacio",
                "anadir.servidor:password.vacio"
        ), warnings);
    }

    @Test
    void validarFormularioReportsEmptyAwsProfile() throws Exception {
        ConfigServer dialog = createValidationDialog(TipoLogin.AWS_PROFILE);
        setField(dialog, "txNombre", new JTextField("Server AWS"));
        setField(dialog, "txIp", new JTextField("db.example.com"));
        setField(dialog, "txPuerto", new JTextField("5432"));
        setField(dialog, "txBbddUser", new JTextField("admin"));
        List<String> warnings = new ArrayList<>();

        boolean valido = dialog.validarFormulario((title, body) -> warnings.add(title + ":" + body));

        assertFalse(valido);
        assertEquals(List.of("anadir.servidor:perfil.aws.vacio"), warnings);
    }
}
