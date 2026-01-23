package es.jklabs.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.security.MasterKeyProvider;
import es.jklabs.security.SecureMetadata;
import es.jklabs.security.SecureStorageException;
import es.jklabs.security.SecureStorageManager;
import es.jklabs.storage.FileSystemProjectStore;
import es.jklabs.utilidades.UtilidadesEncryptacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LegacyMigrationTest {

    @Test
    void migratesLegacyConfigToSecureVault() throws Exception {
        Path baseDir = Files.createTempDirectory("bse-legacy-");
        FileSystemProjectStore store = new FileSystemProjectStore(baseDir);
        SecureStorageManager manager = new SecureStorageManager(baseDir.resolve(".secure"),
                java.util.List.of(new TestProvider()));
        MigrationService migrationService = new FileSystemMigrationService(store, manager);

        Configuracion legacy = new Configuracion();
        Servidor servidor = new Servidor();
        servidor.setName("Legacy");
        servidor.setHost("localhost");
        servidor.setPort("3306");
        servidor.setTipoServidor(TipoServidor.MYSQL);
        servidor.setTipoLogin(TipoLogin.USUARIO_CONTRASENA);
        servidor.setUser("user");
        servidor.setPass(UtilidadesEncryptacion.encrypt("secret"));
        legacy.getServers().add(servidor);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(baseDir.resolve("config.json"), gson.toJson(legacy));

        migrationService.migrateIfNeeded();

        Path connections = baseDir.resolve("connections.json");
        assertTrue(Files.exists(connections));
        Configuracion migrated = gson.fromJson(Files.readString(connections), Configuracion.class);
        assertEquals(1, migrated.getServers().size());
        assertNotNull(migrated.getServers().get(0).getCredentialRef());
        assertNull(migrated.getServers().get(0).getPass());
        assertTrue(Files.exists(baseDir.resolve(".secure/credentials-config.json")));

        boolean backupFound = Files.list(baseDir)
                .anyMatch(p -> p.getFileName().toString().startsWith("config.json.bak-"));
        assertTrue(backupFound);
    }

    private static class TestProvider implements MasterKeyProvider {
        @Override
        public String getId() {
            return "test";
        }

        @Override
        public String getDisplayName() {
            return "test";
        }

        @Override
        public int getDefaultPriority() {
            return 100;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public byte[] getOrCreateMasterKey(SecureMetadata metadata, java.awt.Component parent, boolean allowCreate)
                throws SecureStorageException {
            return "01234567890123456789012345678901".getBytes();
        }

        @Override
        public void reset(SecureMetadata metadata, java.awt.Component parent) {
        }
    }
}
