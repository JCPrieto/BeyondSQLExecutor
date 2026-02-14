package es.jklabs.gui.dialogos;

import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AcercaDeTest {

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

    @Test
    void getJLabelMyMailReturnsConfiguredLabelWithMouseBehavior() throws Exception {
        Method method = AcercaDe.class.getDeclaredMethod("getJLabelMyMail");
        method.setAccessible(true);
        JLabel mailLabel = (JLabel) method.invoke(null);

        assertEquals("JuanC.Prieto.Silos@gmail.com", mailLabel.getText());
        assertTrue(mailLabel.getMouseListeners().length > 0, "Expected at least one mouse listener.");

        MouseListener listener = mailLabel.getMouseListeners()[0];
        listener.mouseEntered(new MouseEvent(mailLabel, MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0, 0, false));
        assertNotNull(mailLabel.getCursor(), "Expected cursor to be set on mouse enter.");
        assertEquals(Cursor.HAND_CURSOR, mailLabel.getCursor().getType());

        listener.mouseExited(new MouseEvent(mailLabel, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false));
        assertNotNull(mailLabel.getCursor(), "Expected cursor to fall back to default on mouse exit.");
        assertEquals(Cursor.DEFAULT_CURSOR, mailLabel.getCursor().getType());
    }

    @Test
    void addPoweredWithUrlAddsTitleAndUrlLabels() throws Exception {
        AcercaDe acercaDe = allocateInstance(AcercaDe.class);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        invokePrivateMethod(
                acercaDe,
                "addPowered",
                new Class[]{JPanel.class, GridBagConstraints.class, int.class, String.class, String.class},
                new Object[]{panel, constraints, 4, "GSon", "https://github.com/google/gson"});

        assertEquals(2, panel.getComponentCount(), "Expected title and URL labels.");

        JLabel titleLabel = (JLabel) panel.getComponent(0);
        JLabel urlLabel = (JLabel) panel.getComponent(1);
        assertTrue(titleLabel.getText().contains("GSon"));
        assertEquals("https://github.com/google/gson", urlLabel.getText());
        assertTrue(titleLabel.getMouseListeners().length > 0, "Expected title label to be clickable.");
        assertTrue(urlLabel.getMouseListeners().length > 0, "Expected URL label to be clickable.");
    }

    @Test
    void addPoweredWithoutUrlAddsOnlyTitleLabel() throws Exception {
        AcercaDe acercaDe = allocateInstance(AcercaDe.class);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        invokePrivateMethod(
                acercaDe,
                "addPowered",
                new Class[]{JPanel.class, GridBagConstraints.class, int.class, String.class, String.class},
                new Object[]{panel, constraints, 2, "JNA", null});

        assertEquals(1, panel.getComponentCount(), "Expected only title label when URL is null.");

        JLabel titleLabel = (JLabel) panel.getComponent(0);
        assertTrue(titleLabel.getText().contains("JNA"));
        assertEquals(0, titleLabel.getMouseListeners().length, "Expected title label to be non-clickable.");
    }
}
