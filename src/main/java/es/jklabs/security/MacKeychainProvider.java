package es.jklabs.security;

import es.jklabs.utilidades.UtilidadesSeguridad;

import java.awt.*;
import java.util.Base64;
import java.util.List;

public class MacKeychainProvider implements MasterKeyProvider {
    public static final String ID = "os-keychain";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "macOS Keychain";
    }

    @Override
    public int getDefaultPriority() {
        return 100;
    }

    @Override
    public boolean isAvailable() {
        if (!OsUtils.isMac()) {
            return false;
        }
        try {
            CommandRunner.CommandResult result = CommandRunner.run(List.of("security", "-h"), null);
            return result.exitCode() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public byte[] getOrCreateMasterKey(SecureMetadata metadata, Component parent, boolean allowCreate)
            throws SecureStorageException {
        OsProviderConfig config = ensureConfig(metadata);
        String service = config.getServiceName();
        String account = config.getAccountName();
        try {
            CommandRunner.CommandResult lookup = CommandRunner.run(List.of("security", "find-generic-password",
                    "-s", service, "-a", account, "-w"), null);
            if (lookup.exitCode() == 0 && !lookup.stdout().isBlank()) {
                return Base64.getDecoder().decode(lookup.stdout().trim());
            }
            if (!allowCreate) {
                return null;
            }
            byte[] key = CryptoUtils.randomBytes(32);
            String encoded = Base64.getEncoder().encodeToString(key);
            CommandRunner.CommandResult store = CommandRunner.run(List.of("security", "add-generic-password",
                    "-s", service, "-a", account, "-w", encoded, "-U"), null);
            if (store.exitCode() != 0) {
                throw new SecureStorageException("No se pudo guardar la clave en Keychain: " + store.stderr());
            }
            return key;
        } catch (Exception e) {
            throw new SecureStorageException("No se pudo acceder a Keychain.", e);
        }
    }

    @Override
    public void reset(SecureMetadata metadata, Component parent) {
        // Best-effort; a new key will be created on next unlock.
    }

    private OsProviderConfig ensureConfig(SecureMetadata metadata) {
        return UtilidadesSeguridad.getOsProviderConfig(metadata);
    }
}
