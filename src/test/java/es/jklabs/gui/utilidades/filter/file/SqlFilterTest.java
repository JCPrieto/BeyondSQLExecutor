package es.jklabs.gui.utilidades.filter.file;

import es.jklabs.utilidades.Mensajes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqlFilterTest {

    private final SqlFilter filter = new SqlFilter();

    @Test
    void acceptReturnsTrueForDirectories(@TempDir Path tempDir) {
        assertTrue(filter.accept(tempDir.toFile()));
    }

    @Test
    void acceptReturnsTrueForSqlExtension() {
        File file = new File("script.sql");
        assertTrue(filter.accept(file));
    }

    @Test
    void acceptReturnsFalseForNonSqlExtension() {
        File file = new File("script.json");
        assertFalse(filter.accept(file));
    }

    @Test
    void getDescriptionReturnsLocalizedSqlLabel() {
        assertEquals(Mensajes.getMensaje("file.chooser.sql"), filter.getDescription());
    }
}
