package es.jklabs.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.security.SecureMetadata;
import es.jklabs.security.SecureVaultEntry;
import es.jklabs.security.SecureVaultFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectStoreZipTest {

    @Test
    void exportImportRoundTrip() throws Exception {
        Path baseDir = Files.createTempDirectory("bse-project-");
        FileSystemProjectStore store = new FileSystemProjectStore(baseDir);
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

        Path secureDir = baseDir.resolve(".secure");
        Files.createDirectories(secureDir);
        SecureVaultFile vault = new SecureVaultFile();
        vault.setVaultVersion(2);
        vault.setEntries(Map.of("cred:test", new SecureVaultEntry("nonce", "cipher")));
        SecureMetadata meta = new SecureMetadata();
        meta.setSchemaVersion(1);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(secureDir.resolve("credentials-config.json"), gson.toJson(vault));
        Files.writeString(secureDir.resolve("secure-meta.json"), gson.toJson(meta));

        File zip = baseDir.resolve("export.zip").toFile();
        store.exportProject(zip);

        Path baseDir2 = Files.createTempDirectory("bse-project-import-");
        FileSystemProjectStore store2 = new FileSystemProjectStore(baseDir2);
        Configuracion merged = store2.importProject(zip, new Configuracion());
        assertEquals(1, merged.getServers().size());
        assertTrue(Files.exists(baseDir2.resolve(".secure/credentials-config.json")));
        assertTrue(Files.exists(baseDir2.resolve(".secure/secure-meta.json")));
    }
}
