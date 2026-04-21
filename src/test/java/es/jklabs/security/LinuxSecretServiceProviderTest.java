package es.jklabs.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinuxSecretServiceProviderTest {

    @TempDir
    Path tempDir;

    private static java.util.List<String> javaFixtureCommand(String osName) {
        return java.util.List.of(
                getJavaExecutable(),
                "-Dos.name=" + osName,
                "-cp",
                System.getProperty("java.class.path"),
                LinuxSecretServiceProviderAvailabilityFixture.class.getName()
        );
    }

    private static java.util.List<String> javaGetOrCreateFixtureCommand(String osName, boolean allowCreate) {
        return java.util.List.of(
                getJavaExecutable(),
                "-Dos.name=" + osName,
                "-cp",
                System.getProperty("java.class.path"),
                LinuxSecretServiceProviderGetOrCreateFixture.class.getName(),
                Boolean.toString(allowCreate)
        );
    }

    private static String getJavaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private static void createSecretTool(Path dir, String content) throws IOException {
        Path script = dir.resolve("secret-tool");
        Files.writeString(script, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(script, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
        ));
    }

    @Test
    void isAvailableReturnsFalseWhenOsIsNotLinux() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin"));
        createSecretTool(toolDir, """
                #!/bin/sh
                exit 0
                """);

        assertFixtureAvailability("Mac OS X", toolDir, false);
    }

    @Test
    void isAvailableReturnsFalseWhenSecretToolFailsWithoutUsageHint() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin-error"));
        createSecretTool(toolDir, """
                #!/bin/sh
                echo "unexpected failure" 1>&2
                exit 1
                """);

        assertFixtureAvailability("Linux", toolDir, false);
    }

    @Test
    void isAvailableReturnsTrueWhenSecretToolVersionSucceeds() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin"));
        createSecretTool(toolDir, """
                #!/bin/sh
                if [ "$1" = "--version" ]; then
                  echo "secret-tool 1.0"
                  exit 0
                fi
                exit 1
                """);

        assertFixtureAvailability("Linux", toolDir, true);
    }

    @Test
    void isAvailableReturnsTrueWhenSecretToolReportsUsage() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin"));
        createSecretTool(toolDir, """
                #!/bin/sh
                echo "usage: secret-tool [--help]" 1>&2
                exit 1
                """);

        assertFixtureAvailability("Linux", toolDir, true);
    }

    @Test
    void getOrCreateMasterKeyReturnsDecodedExistingKeyWhenLookupSucceeds() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin-lookup"));
        String encoded = Base64.getEncoder().encodeToString("existing-secret".getBytes(StandardCharsets.UTF_8));
        createSecretTool(toolDir, "#!/bin/sh\n"
                + "if [ \"$1\" = \"lookup\" ]; then\n"
                + "  printf '%s\\n' '" + encoded + "'\n"
                + "  exit 0\n"
                + "fi\n"
                + "exit 1\n");

        String output = runGetOrCreateFixture("Linux", toolDir, false);

        assertEquals("OK:" + encoded, output);
    }

    @Test
    void getOrCreateMasterKeyReturnsNullWhenLookupFailsAndCreationIsNotAllowed() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin-no-create"));
        createSecretTool(toolDir, """
                #!/bin/sh
                if [ "$1" = "lookup" ]; then
                  exit 1
                fi
                echo "store should not be called" 1>&2
                exit 9
                """);

        String output = runGetOrCreateFixture("Linux", toolDir, false);

        assertEquals("NULL", output);
    }

    @Test
    void getOrCreateMasterKeyCreatesAndStoresKeyWhenLookupFailsAndCreationIsAllowed() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin-create"));
        Path storeMarker = tempDir.resolve("store-marker.txt");
        createSecretTool(toolDir, "#!/bin/sh\n"
                + "if [ \"$1\" = \"lookup\" ]; then\n"
                + "  exit 1\n"
                + "fi\n"
                + "if [ \"$1\" = \"store\" ]; then\n"
                + "  printf 'store-called' > '" + storeMarker.toAbsolutePath() + "'\n"
                + "  cat >/dev/null\n"
                + "  exit 0\n"
                + "fi\n"
                + "exit 1\n");

        String output = runGetOrCreateFixture("Linux", toolDir, true);

        assertTrue(output.startsWith("OK:"));
        String encoded = output.substring(3);
        assertEquals("store-called", Files.readString(storeMarker, StandardCharsets.UTF_8));
        assertEquals(32, Base64.getDecoder().decode(encoded).length);
    }

    @Test
    void getOrCreateMasterKeyReturnsWrappedErrorWhenStoreFails() throws IOException, InterruptedException {
        Path toolDir = Files.createDirectory(tempDir.resolve("bin-store-error"));
        createSecretTool(toolDir, """
                #!/bin/sh
                if [ "$1" = "lookup" ]; then
                  exit 1
                fi
                if [ "$1" = "store" ]; then
                  echo "cannot persist secret" 1>&2
                  exit 7
                fi
                exit 1
                """);

        String output = runGetOrCreateFixture("Linux", toolDir, true);

        assertEquals("ERROR:SecureStorageException:No se pudo guardar la clave en Secret Service: cannot persist secret",
                output);
    }

    private void assertFixtureAvailability(String osName, Path pathDir, boolean expected)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(javaFixtureCommand(osName));
        builder.directory(tempDir.toFile());
        builder.redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.put("PATH", pathDir.toString());

        Process started = builder.start();
        String output = new String(started.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int exitCode = started.waitFor();

        assertEquals(0, exitCode);
        assertEquals(Boolean.toString(expected), output);
    }

    private String runGetOrCreateFixture(String osName, Path pathDir, boolean allowCreate)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(javaGetOrCreateFixtureCommand(osName, allowCreate));
        builder.directory(tempDir.toFile());
        builder.redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.put("PATH", pathDir.toString());

        Process started = builder.start();
        String output = new String(started.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int exitCode = started.waitFor();

        assertEquals(0, exitCode);
        return output;
    }
}
