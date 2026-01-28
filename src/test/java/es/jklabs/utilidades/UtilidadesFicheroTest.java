package es.jklabs.utilidades;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilidadesFicheroTest {

    private static String getenvOrNull(String key) {
        try {
            return System.getenv(key);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void restoreProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    @Test
    void getLogDirUsesOverrideWhenProvided() {
        String originalOverride = System.getProperty("bse.logs.dir");
        String originalOsName = System.getProperty("os.name");
        try {
            Path override = Path.of(System.getProperty("java.io.tmpdir"), "bse-logs-override");
            System.setProperty("bse.logs.dir", override.toString());
            System.setProperty("os.name", "Linux");

            assertEquals(override, UtilidadesFichero.getLogDir());
        } finally {
            restoreProperty("bse.logs.dir", originalOverride);
            restoreProperty("os.name", originalOsName);
        }
    }

    @Test
    void getLogDirUsesLinuxDefault() {
        String originalOsName = System.getProperty("os.name");
        String originalOverride = System.getProperty("bse.logs.dir");
        try {
            System.setProperty("os.name", "Linux");
            System.clearProperty("bse.logs.dir");

            Path expected = Path.of(UtilidadesFichero.HOME, ".local", "share", "BeyondSQLExecutor", "logs");
            assertEquals(expected, UtilidadesFichero.getLogDir());
        } finally {
            restoreProperty("os.name", originalOsName);
            restoreProperty("bse.logs.dir", originalOverride);
        }
    }

    @Test
    void getLogDirUsesMacDefault() {
        String originalOsName = System.getProperty("os.name");
        String originalOverride = System.getProperty("bse.logs.dir");
        try {
            System.setProperty("os.name", "Mac OS X");
            System.clearProperty("bse.logs.dir");

            Path expected = Path.of(UtilidadesFichero.HOME, "Library", "Application Support", "BeyondSQLExecutor", "logs");
            assertEquals(expected, UtilidadesFichero.getLogDir());
        } finally {
            restoreProperty("os.name", originalOsName);
            restoreProperty("bse.logs.dir", originalOverride);
        }
    }

    @Test
    void getLogDirUsesWindowsDefault() {
        String originalOsName = System.getProperty("os.name");
        String originalOverride = System.getProperty("bse.logs.dir");
        try {
            System.setProperty("os.name", "Windows 10");
            System.clearProperty("bse.logs.dir");

            String base = getenvOrNull("LOCALAPPDATA");
            if (isBlank(base)) {
                base = getenvOrNull("APPDATA");
            }
            if (isBlank(base)) {
                base = UtilidadesFichero.HOME;
            }

            Path expected = Path.of(base, "BeyondSQLExecutor", "logs");
            Path actual = UtilidadesFichero.getLogDir();
            assertEquals(expected, actual);
            assertTrue(actual.endsWith(Path.of("BeyondSQLExecutor", "logs")));
        } finally {
            restoreProperty("os.name", originalOsName);
            restoreProperty("bse.logs.dir", originalOverride);
        }
    }
}
