package es.jklabs.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.security.SecureStorageException;
import es.jklabs.security.SecureStorageManager;
import es.jklabs.storage.FileSystemProjectStore;
import es.jklabs.utilidades.Constantes;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.UtilidadesEncryptacion;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FileSystemMigrationService implements MigrationService {
    private static final String LEGACY_CONFIG = "config.json";
    private static final String MIGRATION_FILE = "migration.json";

    private final FileSystemProjectStore projectStore;
    private final SecureStorageManager secureStorageManager;
    private final Gson gson;

    public FileSystemMigrationService(FileSystemProjectStore projectStore, SecureStorageManager secureStorageManager) {
        this.projectStore = projectStore;
        this.secureStorageManager = secureStorageManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void migrateIfNeeded() {
        try {
            Path baseDir = projectStore.getConnectionsPath().getParent();
            Path legacyPath = baseDir.resolve(LEGACY_CONFIG);
            Path migrationPath = projectStore.getSecureDir().resolve(MIGRATION_FILE);
            if (Files.exists(projectStore.getConnectionsPath())) {
                return;
            }
            if (!Files.exists(legacyPath)) {
                return;
            }
            if (Files.exists(migrationPath)) {
                return;
            }
            secureStorageManager.load();
            Configuracion legacy = gson.fromJson(Files.readString(legacyPath), Configuracion.class);
            if (legacy == null) {
                return;
            }
            if (legacy.getServers() != null) {
                for (Servidor servidor : legacy.getServers()) {
                    String pass = servidor.getPass();
                    if (StringUtils.isBlank(pass)) {
                        continue;
                    }
                    String plain = UtilidadesEncryptacion.decrypt(pass);
                    if (plain == null) {
                        continue;
                    }
                    String credentialRef = servidor.getCredentialRef();
                    if (credentialRef == null) {
                        credentialRef = "cred:" + UUID.randomUUID();
                        servidor.setCredentialRef(credentialRef);
                    }
                    try {
                        secureStorageManager.setPassword(credentialRef, plain, null);
                    } catch (SecureStorageException e) {
                        Logger.error(e);
                    }
                    servidor.setPass(null);
                }
            }
            projectStore.save(legacy);
            backupLegacy(legacyPath);
            writeMarker(migrationPath, "legacy", Constantes.VERSION, "success");
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private void backupLegacy(Path legacyPath) {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path backup = legacyPath.resolveSibling(legacyPath.getFileName() + ".bak-" + timestamp);
            Files.move(legacyPath, backup);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private void writeMarker(Path migrationPath, String fromVersion, String toVersion, String outcome) {
        try {
            Files.createDirectories(migrationPath.getParent());
            MigrationMarker marker = new MigrationMarker();
            marker.setFromVersion(fromVersion);
            marker.setToVersion(toVersion);
            marker.setDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            marker.setOutcome(outcome);
            Files.writeString(migrationPath, gson.toJson(marker));
        } catch (Exception e) {
            Logger.error(e);
        }
    }
}
