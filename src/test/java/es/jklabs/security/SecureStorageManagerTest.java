package es.jklabs.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecureStorageManagerTest {

    @Test
    void loadReemplazaDocumentosJsonNullPorValoresIniciales(@TempDir Path secureDir) throws Exception {
        Path metadataPath = secureDir.resolve("secure-meta.json");
        Path vaultPath = secureDir.resolve("credentials-config.json");
        Files.writeString(metadataPath, "null", StandardCharsets.UTF_8);
        Files.writeString(vaultPath, "null", StandardCharsets.UTF_8);
        SecureStorageManager manager = new SecureStorageManager(secureDir, List.of());

        manager.load();

        assertNotNull(manager.getMetadata());
        assertEquals(1, manager.getMetadata().getSchemaVersion());
        assertNotNull(manager.getVault());
        assertEquals(2, manager.getVault().getVaultVersion());
        assertTrue(Files.readString(metadataPath, StandardCharsets.UTF_8).contains("\"schemaVersion\": 1"));
        assertTrue(Files.readString(vaultPath, StandardCharsets.UTF_8).contains("\"vaultVersion\": 2"));
    }
}
