package es.jklabs.gui.dialogos;

import es.jklabs.json.configuracion.TipoLogin;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import sun.misc.Unsafe;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServerTest {

    private static <T> T allocateInstance(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        return type.cast(unsafe.allocateInstance(type));
    }

    private static Object invokePrivateMethod(Object target, String name, Class<?>[] parameterTypes, Object[] args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object invokePrivateStaticMethod(Class<?> type, String name) throws Exception {
        Method method = type.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(null);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void getGridBagConstraintsReturnsExpectedDefaults() throws Exception {
        GridBagConstraints constraints = (GridBagConstraints) invokePrivateStaticMethod(ConfigServer.class, "getGridBagConstraints");

        assertEquals(1, constraints.gridwidth);
        assertEquals(1, constraints.gridheight);
        assertEquals(new Insets(5, 5, 5, 5), constraints.insets);
        assertEquals(GridBagConstraints.LINE_START, constraints.anchor);
    }

    @Test
    void seleccionarLoginPasswordRemovesAwsFieldsAndAddsPasswordFields() throws Exception {
        ConfigServer dialog = allocateInstance(ConfigServer.class);
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
    }

    @Test
    void seleccionarLoginAWSRemovesPasswordFieldAndAddsAwsFields() throws Exception {
        ConfigServer dialog = allocateInstance(ConfigServer.class);
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
    }

    @Test
    void setRolEditableTogglesEditableAndClearsTextWhenDisabled() throws Exception {
        ConfigServer dialog = allocateInstance(ConfigServer.class);
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
    void validarFormularioReturnsTrueForUserPasswordLoginWithRequiredFields() throws Exception {
        ConfigServer dialog = allocateInstance(ConfigServer.class);

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
        ConfigServer dialog = allocateInstance(ConfigServer.class);

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
}
