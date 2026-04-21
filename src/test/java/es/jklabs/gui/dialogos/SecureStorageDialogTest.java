package es.jklabs.gui.dialogos;

import es.jklabs.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecureStorageDialogTest {

    private static <T> T allocateInstance(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        return type.cast(unsafe.allocateInstance(type));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object invokePrivateMethod(Object target, String name, Class<?>[] parameterTypes, Object[] args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static AbstractTableModel createProviderTableModel(SecureStorageManager manager) throws Exception {
        Class<?> modelClass = Class.forName("es.jklabs.gui.dialogos.SecureStorageDialog$ProviderTableModel");
        Constructor<?> constructor = modelClass.getDeclaredConstructor(SecureStorageManager.class);
        constructor.setAccessible(true);
        return (AbstractTableModel) constructor.newInstance(manager);
    }

    @Test
    void providerTableModelExposesOnlyAvailableProvidersAndColumnMetadata(@TempDir Path tempDir) throws Exception {
        FakeProvider available = new FakeProvider("ui", "UI Prompt", 5, true);
        FakeProvider unavailable = new FakeProvider("os", "OS Provider", 1, false);
        TrackingSecureStorageManager manager = new TrackingSecureStorageManager(tempDir, List.of(available, unavailable));
        manager.load();

        ProviderConfig config = manager.ensureProviderConfig(manager.getMetadata(), available);
        config.setEnabled(true);
        config.setPriority(7);

        AbstractTableModel model = createProviderTableModel(manager);

        assertEquals(1, model.getRowCount());
        assertEquals(3, model.getColumnCount());
        assertEquals("Activo", model.getColumnName(0));
        assertEquals(Boolean.class, model.getColumnClass(0));
        assertEquals(String.class, model.getColumnClass(1));
        assertEquals(Integer.class, model.getColumnClass(2));
        assertTrue(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(0, 1));
        assertEquals(Boolean.TRUE, model.getValueAt(0, 0));
        assertEquals("UI Prompt", model.getValueAt(0, 1));
        assertEquals(7, model.getValueAt(0, 2));
        assertNull(model.getValueAt(0, 99));
    }

    @Test
    void providerTableModelSetValueAtCoversBooleanIntegerStringAndInvalidPriority(@TempDir Path tempDir) throws Exception {
        FakeProvider provider = new FakeProvider("ui", "UI Prompt", 5, true);
        TrackingSecureStorageManager manager = new TrackingSecureStorageManager(tempDir, List.of(provider));
        manager.load();
        AbstractTableModel model = createProviderTableModel(manager);

        model.setValueAt(Boolean.FALSE, 0, 0);
        assertEquals(1, manager.updateCalls);
        assertEquals("ui", manager.lastProviderId);
        assertFalse(manager.lastEnabled);

        model.setValueAt(3, 0, 2);
        assertEquals(2, manager.updateCalls);
        assertEquals(3, manager.lastPriority);

        model.setValueAt("9", 0, 2);
        assertEquals(3, manager.updateCalls);
        assertEquals(9, manager.lastPriority);

        model.setValueAt("ignored", 0, 1);
        assertEquals(4, manager.updateCalls);
        assertFalse(manager.lastEnabled);
        assertEquals(9, manager.lastPriority);

        model.setValueAt("NaN", 0, 2);
        assertEquals(4, manager.updateCalls);

        ProviderConfig config = manager.ensureProviderConfig(manager.getMetadata(), provider);
        assertFalse(config.isEnabled());
        assertEquals(9, config.getPriority());
    }

    @Test
    void refreshUpdatesVisibleStateFromManagerMetadataAndVault(@TempDir Path tempDir) throws Exception {
        FakeProvider provider = new FakeProvider("ui", "UI Prompt", 5, true);
        TrackingSecureStorageManager manager = new TrackingSecureStorageManager(tempDir, List.of(provider));
        manager.load();
        manager.getMetadata().setProviderHint("ui");
        manager.getMetadata().setSchemaVersion(11);
        manager.getVault().setVaultVersion(22);
        manager.getVault().getEntries().put("cred-1", new SecureVaultEntry("nonce", "cipher"));

        SecureStorageDialog dialog = allocateInstance(SecureStorageDialog.class);
        AbstractTableModel model = createProviderTableModel(manager);
        JList<String> entryList = new JList<>(new String[]{"stale"});
        JTextArea advancedArea = new JTextArea("stale");

        setField(dialog, "manager", manager);
        setField(dialog, "providerTableModel", model);
        setField(dialog, "entryList", entryList);
        setField(dialog, "advancedArea", advancedArea);

        invokePrivateMethod(dialog, "refresh", new Class[0], new Object[0]);
        String advancedText = (String) invokePrivateMethod(dialog, "buildAdvancedText", new Class[0], new Object[0]);

        assertEquals(1, model.getRowCount());
        assertEquals(1, entryList.getModel().getSize());
        assertEquals("cred-1", entryList.getModel().getElementAt(0));
        assertEquals(advancedText, advancedArea.getText());
        assertTrue(advancedText.contains("Provider hint: ui"));
        assertTrue(advancedText.contains("Schema: 11"));
        assertTrue(advancedText.contains("Vault: 22"));
    }

    private static final class TrackingSecureStorageManager extends SecureStorageManager {
        private int updateCalls;
        private String lastProviderId;
        private boolean lastEnabled;
        private int lastPriority;

        private TrackingSecureStorageManager(Path secureDir, List<MasterKeyProvider> providers) {
            super(secureDir, providers);
        }

        @Override
        public void updateProviderConfig(String providerId, boolean enabled, int priority) {
            updateCalls++;
            lastProviderId = providerId;
            lastEnabled = enabled;
            lastPriority = priority;
            super.updateProviderConfig(providerId, enabled, priority);
        }
    }

    private static final class FakeProvider implements MasterKeyProvider {
        private final String id;
        private final String displayName;
        private final int defaultPriority;
        private final boolean available;

        private FakeProvider(String id, String displayName, int defaultPriority, boolean available) {
            this.id = id;
            this.displayName = displayName;
            this.defaultPriority = defaultPriority;
            this.available = available;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public int getDefaultPriority() {
            return defaultPriority;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public byte[] getOrCreateMasterKey(SecureMetadata metadata, Component parent, boolean allowCreate)
                throws SecureStorageException {
            return new byte[0];
        }

        @Override
        public void reset(SecureMetadata metadata, Component parent) throws SecureStorageException {
        }
    }
}
