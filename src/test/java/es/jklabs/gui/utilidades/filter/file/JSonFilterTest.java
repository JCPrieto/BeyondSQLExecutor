package es.jklabs.gui.utilidades.filter.file;

import es.jklabs.utilidades.Mensajes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JSonFilterTest {

    private final JSonFilter filter = new JSonFilter();

    @Test
    void acceptReturnsTrueForDirectories(@TempDir Path tempDir) {
        assertTrue(filter.accept(tempDir.toFile()));
    }

    @Test
    void acceptReturnsTrueForJsonExtension() {
        File file = new File("config.json");
        assertTrue(filter.accept(file));
    }

    @Test
    void acceptReturnsFalseForNonJsonExtension() {
        File file = new File("config.sql");
        assertFalse(filter.accept(file));
    }

    @Test
    void getDescriptionReturnsLocalizedJsonLabel() {
        assertEquals(Mensajes.getMensaje("file.chooser.json"), filter.getDescription());
    }
}
