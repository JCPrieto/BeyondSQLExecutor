package es.jklabs.security;

import javax.swing.*;
import java.awt.*;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class UiPromptProvider implements MasterKeyProvider {
    public static final String ID = "ui-prompt";
    private static final int DEFAULT_ITERATIONS = 120000;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Password Prompt";
    }

    @Override
    public int getDefaultPriority() {
        return 10;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public byte[] getOrCreateMasterKey(SecureMetadata metadata, Component parent, boolean allowCreate)
            throws SecureStorageException {
        UiKdfParams params = metadata.getUiKdfParams();
        char[] password = null;
        try {
            if (params == null) {
                if (!allowCreate) {
                    return null;
                }
                password = promptForPassword(parent, "Crear contraseña maestra");
                if (password == null || password.length == 0) {
                    throw new SecureStorageException("No se ha indicado la contraseña maestra.");
                }
                byte[] salt = CryptoUtils.randomBytes(16);
                byte[] key = CryptoUtils.deriveKeyPbkdf2(password, salt, DEFAULT_ITERATIONS);
                UiKdfParams newParams = new UiKdfParams();
                newParams.setSaltB64(Base64.getEncoder().encodeToString(salt));
                newParams.setIterations(DEFAULT_ITERATIONS);
                newParams.setAlgorithm(ALGORITHM);
                metadata.setUiKdfParams(newParams);
                return key;
            }
            password = promptForPassword(parent, "Contraseña maestra");
            if (password == null || password.length == 0) {
                throw new SecureStorageException("No se ha indicado la contraseña maestra.");
            }
            byte[] salt = Base64.getDecoder().decode(params.getSaltB64());
            return CryptoUtils.deriveKeyPbkdf2(password, salt, params.getIterations());
        } catch (GeneralSecurityException e) {
            throw new SecureStorageException("Error al derivar la clave maestra.", e);
        } finally {
            CryptoUtils.wipe(password);
        }
    }

    @Override
    public void reset(SecureMetadata metadata, Component parent) throws SecureStorageException {
        metadata.setUiKdfParams(null);
    }

    private char[] promptForPassword(Component parent, String title) {
        JPasswordField field = new JPasswordField(20);
        int result = JOptionPane.showConfirmDialog(parent, field, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return field.getPassword();
    }
}
