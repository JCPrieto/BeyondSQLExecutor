package es.jklabs.storage;

import es.jklabs.json.configuracion.Configuracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    private static void writeZipEntry(Path zipPath, String entryName, String content) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
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

    @Test
    void unzipRejectsEntriesOutsideDestination() throws Exception {
        FileSystemProjectStore store = new FileSystemProjectStore(tempDir);
        Path zip = tempDir.resolve("malicious.zip");
        Path destination = tempDir.resolve("extract");
        Path outside = tempDir.resolve("outside.txt");
        writeZipEntry(zip, "../outside.txt", "malicious");

        assertThrows(IOException.class, () -> store.unzip(zip, destination));
        assertFalse(Files.exists(outside), "ZIP entry outside destination should not be written.");
    }

    @Test
    void createImportTempDirectoryUsesProjectDirectory() throws Exception {
        FileSystemProjectStore store = new FileSystemProjectStore(tempDir);
        Path importTempDir = store.createImportTempDirectory();

        try {
            assertTrue(importTempDir.startsWith(tempDir));
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                assertEquals(Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE
                ), Files.getPosixFilePermissions(importTempDir));
            }
        } finally {
            Files.deleteIfExists(importTempDir);
        }
    }
}
