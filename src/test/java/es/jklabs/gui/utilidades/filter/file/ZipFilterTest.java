package es.jklabs.gui.utilidades.filter.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipFilterTest {

    private final ZipFilter filter = new ZipFilter();

    @Test
    void acceptReturnsTrueForDirectories(@TempDir Path tempDir) {
        assertTrue(filter.accept(tempDir.toFile()));
    }

    @Test
    void acceptReturnsTrueForZipExtension() {
        File file = new File("project.zip");
        assertTrue(filter.accept(file));
    }

    @Test
    void acceptReturnsTrueForUppercaseZipExtension() {
        File file = new File("project.ZIP");
        assertTrue(filter.accept(file));
    }

    @Test
    void acceptReturnsFalseForNonZipExtension() {
        File file = new File("project.json");
        assertFalse(filter.accept(file));
    }

    @Test
    void getDescriptionReturnsZipLabel() {
        assertEquals("Archivos ZIP (*.zip)", filter.getDescription());
    }
}
