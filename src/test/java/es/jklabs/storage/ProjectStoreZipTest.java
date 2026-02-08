package es.jklabs.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.security.MasterKeyProvider;
import es.jklabs.security.SecureMetadata;
import es.jklabs.security.SecureStorageManager;
import es.jklabs.utilidades.UtilidadesEncryptacion;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectStoreZipTest {

    private static void unzip(Path zipPath, Path destination) throws Exception {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipPath.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    Files.write(outPath, zis.readAllBytes());
                }
            }
        }
    }

    @Test
    void exportImportRoundTrip() throws Exception {
        Path baseDir = Files.createTempDirectory("bse-project-");
        FileSystemProjectStore store = new FileSystemProjectStore(baseDir,
                dir -> new SecureStorageManager(dir, java.util.List.of(new TestProvider())));
        SecureStorageManager manager = new SecureStorageManager(baseDir.resolve(".secure"),
                java.util.List.of(new TestProvider()));
        manager.load();
        Configuracion config = new Configuracion();
        Servidor servidor = new Servidor();
        servidor.setName("Test");
        servidor.setHost("localhost");
        servidor.setPort("5432");
        servidor.setTipoServidor(TipoServidor.POSTGRESQL);
        servidor.setTipoLogin(TipoLogin.USUARIO_CONTRASENA);
        servidor.setUser("user");
        servidor.setCredentialRef("cred:test");
        config.getServers().add(servidor);
        store.save(config);
        manager.setPassword("cred:test", "secret", null);

        File zip = baseDir.resolve("export.zip").toFile();
        store.exportProject(zip);

        Path tempDir = Files.createTempDirectory("bse-export-inspect-");
        unzip(zip.toPath(), tempDir);
        Gson gson = new GsonBuilder().create();
        Configuracion exported = gson.fromJson(Files.readString(tempDir.resolve("connections.json")), Configuracion.class);
        assertEquals(1, exported.getServers().size());
        Servidor exportedServer = exported.getServers().get(0);
        assertNull(exportedServer.getCredentialRef());
        assertNotNull(exportedServer.getPass());
        assertEquals("secret", UtilidadesEncryptacion.decrypt(exportedServer.getPass()));
        assertFalse(Files.exists(tempDir.resolve(".secure/credentials-config.json")));

        Path baseDir2 = Files.createTempDirectory("bse-project-import-");
        FileSystemProjectStore store2 = new FileSystemProjectStore(baseDir2);
        Configuracion merged = store2.importProject(zip, new Configuracion());
        assertEquals(1, merged.getServers().size());
        Servidor importedServer = merged.getServers().get(0);
        assertNull(importedServer.getCredentialRef());
        assertNotNull(importedServer.getPass());
        assertEquals("secret", UtilidadesEncryptacion.decrypt(importedServer.getPass()));
        assertFalse(Files.exists(baseDir2.resolve(".secure/credentials-config.json")));
        assertFalse(Files.exists(baseDir2.resolve(".secure/secure-meta.json")));
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
        public byte[] getOrCreateMasterKey(SecureMetadata metadata, java.awt.Component parent, boolean allowCreate) {
            return "01234567890123456789012345678901".getBytes();
        }

        @Override
        public void reset(SecureMetadata metadata, java.awt.Component parent) {
        }
    }
}
