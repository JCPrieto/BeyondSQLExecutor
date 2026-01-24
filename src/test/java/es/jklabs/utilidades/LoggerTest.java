package es.jklabs.utilidades;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest {

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
    }

    @Test
    void eliminarLogsVaciosDeletesEmptyLogsExceptCurrent() throws Exception {
        String originalFolder = UtilidadesFichero.APP_FOLDER;
        String tempFolder = ".BeyondSQLExecutorTest-" + UUID.randomUUID();
        Path baseDir = Path.of(UtilidadesFichero.HOME, tempFolder);

        try {
            UtilidadesFichero.APP_FOLDER = tempFolder;
            Files.createDirectories(baseDir);

            Path emptyLog = baseDir.resolve("empty.log");
            Path nonEmptyLog = baseDir.resolve("nonempty.log");
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path currentLog = baseDir.resolve("log_" + date + ".log");

            Files.writeString(emptyLog, "", StandardCharsets.UTF_8);
            Files.writeString(nonEmptyLog, "line", StandardCharsets.UTF_8);
            Files.writeString(currentLog, "", StandardCharsets.UTF_8);

            Logger.eliminarLogsVacios();

            assertFalse(Files.exists(emptyLog), "Empty log should be deleted.");
            assertTrue(Files.exists(nonEmptyLog), "Non-empty log should remain.");
            assertTrue(Files.exists(currentLog), "Current log should not be deleted.");
        } finally {
            UtilidadesFichero.APP_FOLDER = originalFolder;
            deleteRecursively(baseDir);
        }
    }
}
