package es.jklabs.storage;

import es.jklabs.json.configuracion.Configuracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemProjectStoreTest {

    @TempDir
    Path tempDir;

    private static List<Path> listFiles(Path dir, String prefix) throws Exception {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .collect(Collectors.toList());
        }
    }

    @Test
    void loadBacksUpCorruptConnectionsJson() throws Exception {
        FileSystemProjectStore store = new FileSystemProjectStore(tempDir);
        Path connections = tempDir.resolve("connections.json");
        Files.writeString(connections, "{ invalid json", StandardCharsets.UTF_8);

        Configuracion loaded = store.load();

        assertNotNull(loaded);
        FileSystemProjectStore.StoreError error = store.consumeLastError();
        assertNotNull(error);
        assertEquals("configuracion.corrupta", error.key());
        List<Path> backups = listFiles(tempDir, "connections.json.corrupt-");
        assertTrue(!backups.isEmpty(), "Expected backup file to be created.");
    }

    @Test
    void importProjectReportsCorruptJsonWithoutBackup() throws Exception {
        FileSystemProjectStore store = new FileSystemProjectStore(tempDir);
        File corruptFile = tempDir.resolve("import.json").toFile();
        Files.writeString(corruptFile.toPath(), "{ invalid json", StandardCharsets.UTF_8);
        Configuracion existing = new Configuracion();

        Configuracion result = store.importProject(corruptFile, existing);

        assertEquals(existing, result);
        FileSystemProjectStore.StoreError error = store.consumeLastError();
        assertNotNull(error);
        assertEquals("configuracion.corrupta", error.key());
        List<Path> backups = listFiles(tempDir, "import.json.corrupt-");
        assertTrue(backups.isEmpty(), "Import should not create backup.");
        assertTrue(Files.exists(corruptFile.toPath()), "Import file should remain intact.");
    }
}
