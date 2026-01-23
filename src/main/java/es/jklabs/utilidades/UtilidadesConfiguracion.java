package es.jklabs.utilidades;

import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.migration.FileSystemMigrationService;
import es.jklabs.migration.MigrationService;
import es.jklabs.security.SecureStorageManager;
import es.jklabs.storage.FileSystemProjectStore;
import es.jklabs.storage.ProjectStore;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class UtilidadesConfiguracion {

    private static FileSystemProjectStore projectStore;
    private static SecureStorageManager secureStorageManager;
    private static MigrationService migrationService;

    private UtilidadesConfiguracion() {

    }

    public static Configuracion loadConfig() {
        try {
            ensureInitialized();
            migrationService.migrateIfNeeded();
            secureStorageManager.load();
            Configuracion configuracion = projectStore.load();
            upgradeAwsSDK(configuracion);
            return configuracion;
        } catch (Exception e) {
            Logger.error(e);
            return new Configuracion();
        }
    }

    public static void guardar(Configuracion configuracion) throws IOException {
        ensureInitialized();
        projectStore.save(configuracion);
    }

    public static void guardarConfiguracion(Configuracion configuracion, File file) throws IOException {
        ensureInitialized();
        projectStore.exportProject(file);
    }

    public static Configuracion loadConfig(File file) throws IOException {
        ensureInitialized();
        Configuracion base = projectStore.load();
        Configuracion merged = projectStore.importProject(file, base);
        upgradeAwsSDK(merged);
        migrateImportedCredentials(merged);
        projectStore.save(merged);
        secureStorageManager.load();
        return merged;
    }

    private static void upgradeAwsSDK(Configuracion configuracion) {
        configuracion.getServers().stream()
                .filter(servidor -> servidor.getRegion() != null &&
                        servidor.getAwsRegion() == null)
                .forEach(servidor -> servidor.setAwsRegion(Region.of(StringUtils.lowerCase(servidor.getRegion()).replace("_", "-"))));
    }

    private static void migrateImportedCredentials(Configuracion configuracion) {
        if (configuracion == null || configuracion.getServers() == null) {
            return;
        }
        for (es.jklabs.json.configuracion.Servidor servidor : configuracion.getServers()) {
            if (servidor.getCredentialRef() != null) {
                continue;
            }
            String pass = servidor.getPass();
            if (StringUtils.isBlank(pass)) {
                continue;
            }
            String plain = UtilidadesEncryptacion.decrypt(pass);
            if (plain == null) {
                continue;
            }
            String credentialRef = "cred:" + java.util.UUID.randomUUID();
            servidor.setCredentialRef(credentialRef);
            servidor.setPass(null);
            try {
                secureStorageManager.setPassword(credentialRef, plain, null);
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    public static SecureStorageManager getSecureStorageManager() {
        ensureInitialized();
        return secureStorageManager;
    }

    public static ProjectStore getProjectStore() {
        ensureInitialized();
        return projectStore;
    }

    private static void ensureInitialized() {
        if (projectStore != null) {
            return;
        }
        Path baseDir = Path.of(UtilidadesFichero.HOME, UtilidadesFichero.APP_FOLDER);
        projectStore = new FileSystemProjectStore(baseDir);
        secureStorageManager = new SecureStorageManager(baseDir.resolve(".secure"));
        migrationService = new FileSystemMigrationService(projectStore, secureStorageManager);
    }
}
