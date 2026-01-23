package es.jklabs.security;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class LinuxSecretServiceProvider implements MasterKeyProvider {
    public static final String ID = "os-secret-service";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Secret Service";
    }

    @Override
    public int getDefaultPriority() {
        return 100;
    }

    @Override
    public boolean isAvailable() {
        if (!OsUtils.isLinux()) {
            return false;
        }
        try {
            CommandRunner.CommandResult result = CommandRunner.run(List.of("secret-tool", "--version"), null);
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
            CommandRunner.CommandResult lookup = CommandRunner.run(List.of("secret-tool", "lookup",
                    "service", service, "account", account), null);
            if (lookup.exitCode() == 0 && !lookup.stdout().isBlank()) {
                return Base64.getDecoder().decode(lookup.stdout().trim());
            }
            if (!allowCreate) {
                return null;
            }
            byte[] key = CryptoUtils.randomBytes(32);
            String encoded = Base64.getEncoder().encodeToString(key);
            String label = service + " master key";
            CommandRunner.CommandResult store = CommandRunner.run(List.of("secret-tool", "store",
                            "--label", label, "service", service, "account", account),
                    (encoded + "\n").getBytes(StandardCharsets.UTF_8));
            if (store.exitCode() != 0) {
                throw new SecureStorageException("No se pudo guardar la clave en Secret Service: " + store.stderr());
            }
            return key;
        } catch (Exception e) {
            throw new SecureStorageException("No se pudo acceder a Secret Service.", e);
        }
    }

    @Override
    public void reset(SecureMetadata metadata, Component parent) {
        // Key removal is best-effort; a new key will be created on next unlock.
    }

    private OsProviderConfig ensureConfig(SecureMetadata metadata) {
        OsProviderConfig config = metadata.getOsProvider();
        if (config == null) {
            config = new OsProviderConfig();
            config.setServiceName("BeyondSQLExecutor");
            config.setAccountName("master-key");
            metadata.setOsProvider(config);
        }
        return config;
    }
}
