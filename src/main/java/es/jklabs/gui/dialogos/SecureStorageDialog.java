package es.jklabs.gui.dialogos;

import es.jklabs.security.MasterKeyProvider;
import es.jklabs.security.ProviderConfig;
import es.jklabs.security.SecureStorageException;
import es.jklabs.security.SecureStorageManager;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesConfiguracion;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecureStorageDialog extends JDialog {
    private final transient SecureStorageManager manager;
    private ProviderTableModel providerTableModel;
    private JList<String> entryList;
    private JTextArea advancedArea;

    public SecureStorageDialog(Frame owner) {
        super(owner, Mensajes.getMensaje("almacenamiento.seguro"), true);
        this.manager = UtilidadesConfiguracion.getSecureStorageManager();
        manager.load();
        setLayout(new BorderLayout());
        setSize(600, 420);
        setLocationRelativeTo(owner);
        add(buildContent(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
    }

    private JComponent buildContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(Mensajes.getMensaje("almacenamiento.tab.password"), buildPasswordTab());
        tabs.addTab(Mensajes.getMensaje("almacenamiento.tab.contenido"), buildContentsTab());
        tabs.addTab(Mensajes.getMensaje("almacenamiento.tab.avanzado"), buildAdvancedTab());
        return tabs;
    }

    private JComponent buildPasswordTab() {
        JPanel panel = new JPanel(new BorderLayout());
        providerTableModel = new ProviderTableModel(manager);
        JTable table = new JTable(providerTableModel);
        table.setFillsViewportHeight(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearCache = new JButton(Mensajes.getMensaje("almacenamiento.limpiar.cache"));
        clearCache.addActionListener(e -> {
            manager.clearCachedMasterKey();
            JOptionPane.showMessageDialog(this, Mensajes.getMensaje("almacenamiento.cache.limpiada"));
        });
        JButton changePassword = new JButton(Mensajes.getMensaje("almacenamiento.cambiar.password"));
        changePassword.addActionListener(e -> changePassword());
        JButton recover = new JButton(Mensajes.getMensaje("almacenamiento.recuperar.password"));
        recover.addActionListener(e -> recoverPassword());
        buttons.add(clearCache);
        buttons.add(changePassword);
        buttons.add(recover);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildContentsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        entryList = new JList<>(manager.getVault().getEntries().keySet().toArray(new String[0]));
        panel.add(new JScrollPane(entryList), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildAdvancedTab() {
        JPanel panel = new JPanel(new BorderLayout());
        advancedArea = new JTextArea(10, 40);
        advancedArea.setEditable(false);
        advancedArea.setText(buildAdvancedText());
        panel.add(new JScrollPane(advancedArea), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton(Mensajes.getMensaje("cerrar"));
        close.addActionListener(e -> dispose());
        panel.add(close);
        return panel;
    }

    private void changePassword() {
        String providerId = promptProviderSelection();
        if (providerId == null) {
            return;
        }
        try {
            manager.changeMasterProvider(providerId, this);
            refresh();
            JOptionPane.showMessageDialog(this, Mensajes.getMensaje("almacenamiento.password.cambiada"));
        } catch (SecureStorageException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), Mensajes.getMensaje("error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recoverPassword() {
        String providerId = promptProviderSelection();
        if (providerId == null) {
            return;
        }
        if (Objects.equals(providerId, es.jklabs.security.UiPromptProvider.ID)) {
            try {
                manager.recoverUiProvider(this);
                refresh();
            } catch (SecureStorageException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), Mensajes.getMensaje("error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            manager.clearCachedMasterKey();
            JOptionPane.showMessageDialog(this, Mensajes.getMensaje("almacenamiento.recuperar.os"));
        }
    }

    private String promptProviderSelection() {
        List<MasterKeyProvider> available = manager.getProviders().stream()
                .filter(MasterKeyProvider::isAvailable)
                .filter(p -> manager.ensureProviderConfig(manager.getMetadata(), p).isEnabled())
                .toList();
        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this, Mensajes.getMensaje("almacenamiento.proveedor.none"));
            return null;
        }
        String[] options = available.stream().map(MasterKeyProvider::getDisplayName).toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(this,
                Mensajes.getMensaje("almacenamiento.seleccionar.proveedor"),
                Mensajes.getMensaje("almacenamiento.seguro"),
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (selected == null) {
            return null;
        }
        for (MasterKeyProvider provider : available) {
            if (Objects.equals(provider.getDisplayName(), selected)) {
                return provider.getId();
            }
        }
        return null;
    }

    private void refresh() {
        providerTableModel.refresh();
        entryList.setListData(manager.getVault().getEntries().keySet().toArray(new String[0]));
        advancedArea.setText(buildAdvancedText());
    }

    private String buildAdvancedText() {
        return "Provider hint: " + manager.getMetadata().getProviderHint() + "\n" +
                "Schema: " + manager.getMetadata().getSchemaVersion() + "\n" +
                "Vault: " + manager.getVault().getVaultVersion() + "\n";
    }

    private static class ProviderRow {
        private final String id;
        private final String name;
        private boolean enabled;
        private int priority;

        private ProviderRow(String id, String name, boolean enabled, int priority) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.priority = priority;
        }
    }

    private static class ProviderTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Activo", "Proveedor", "Prioridad"};
        private final SecureStorageManager manager;
        private List<ProviderRow> rows;

        private ProviderTableModel(SecureStorageManager manager) {
            this.manager = manager;
            refresh();
        }

        public void refresh() {
            rows = new ArrayList<>();
            for (MasterKeyProvider provider : manager.getProviders()) {
                if (!provider.isAvailable()) {
                    continue;
                }
                ProviderConfig config = manager.ensureProviderConfig(manager.getMetadata(), provider);
                rows.add(new ProviderRow(provider.getId(), provider.getDisplayName(),
                        config.isEnabled(), config.getPriority()));
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            }
            if (columnIndex == 2) {
                return Integer.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ProviderRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.enabled;
                case 1 -> row.name;
                case 2 -> row.priority;
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            ProviderRow row = rows.get(rowIndex);
            if (columnIndex == 0) {
                row.enabled = Boolean.TRUE.equals(aValue);
            } else if (columnIndex == 2 && aValue instanceof Integer) {
                row.priority = (Integer) aValue;
            } else if (columnIndex == 2) {
                try {
                    row.priority = Integer.parseInt(String.valueOf(aValue));
                } catch (NumberFormatException ignored) {
                    return;
                }
            }
            manager.updateProviderConfig(row.id, row.enabled, row.priority);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }
}
