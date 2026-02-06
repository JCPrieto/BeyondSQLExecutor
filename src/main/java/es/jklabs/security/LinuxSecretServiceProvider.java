package es.jklabs.security;

import es.jklabs.utilidades.UtilidadesSeguridad;

import java.awt.*;
import java.io.File;
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
            String secretTool = resolveSecretToolCommand();
            if (secretTool == null) {
                return false;
            }
            CommandRunner.CommandResult result = CommandRunner.run(List.of(secretTool, "--version"), null);
            if (result.exitCode() == 0) {
                return true;
            }
            String combined = (result.stdout() + "\n" + result.stderr()).toLowerCase();
            return combined.contains("usage: secret-tool");
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
            String secretTool = resolveSecretToolCommand();
            if (secretTool == null) {
                return null;
            }
            CommandRunner.CommandResult lookup = CommandRunner.run(List.of(secretTool, "lookup",
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
            CommandRunner.CommandResult store = CommandRunner.run(List.of(secretTool, "store",
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
        return UtilidadesSeguridad.getOsProviderConfig(metadata);
    }

    private String resolveSecretToolCommand() {
        String path = System.getenv("PATH");
        if (path != null && !path.isEmpty()) {
            String[] parts = path.split(File.pathSeparator);
            for (String part : parts) {
                File candidate = new File(part, "secret-tool");
                if (candidate.exists() && candidate.canExecute()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        String[] defaults = {"/usr/bin/secret-tool", "/usr/local/bin/secret-tool", "/bin/secret-tool"};
        for (String candidate : defaults) {
            File file = new File(candidate);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}
