package es.jklabs.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

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

    private static LinuxSecretServiceProvider provider(TrackingCommandExecutor executor) {
        return new LinuxSecretServiceProvider(executor, () -> "secret-tool");
    }

    @Test
    void getOrCreateMasterKeyReturnsDecodedExistingKeyWhenLookupSucceeds() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("existing-secret".getBytes(StandardCharsets.UTF_8));
        TrackingCommandExecutor executor = new TrackingCommandExecutor(
                new CommandRunner.CommandResult(0, encoded + "\n", "")
        );
        LinuxSecretServiceProvider provider = provider(executor);

        byte[] key = provider.getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertArrayEquals("existing-secret".getBytes(StandardCharsets.UTF_8), key);
        assertEquals(1, executor.calls.size());
        assertEquals("lookup", executor.calls.getFirst().command().get(1));
    }

    @Test
    void getOrCreateMasterKeyReturnsNullWhenLookupFailsAndCreationIsNotAllowed() throws Exception {
        TrackingCommandExecutor executor = new TrackingCommandExecutor(
                new CommandRunner.CommandResult(1, "", "")
        );
        LinuxSecretServiceProvider provider = provider(executor);

        byte[] key = provider.getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertNull(key);
        assertEquals(1, executor.calls.size());
        assertEquals("lookup", executor.calls.getFirst().command().get(1));
    }

    @Test
    void getOrCreateMasterKeyCreatesAndStoresKeyWhenLookupFailsAndCreationIsAllowed() throws Exception {
        TrackingCommandExecutor executor = new TrackingCommandExecutor(
                new CommandRunner.CommandResult(1, "", ""),
                new CommandRunner.CommandResult(0, "", "")
        );
        LinuxSecretServiceProvider provider = provider(executor);

        byte[] key = provider.getOrCreateMasterKey(new SecureMetadata(), null, true);

        assertEquals(32, key.length);
        assertEquals(2, executor.calls.size());
        assertEquals("lookup", executor.calls.get(0).command().get(1));
        assertEquals("store", executor.calls.get(1).command().get(1));
        String storedKey = new String(executor.calls.get(1).stdin(), StandardCharsets.UTF_8).trim();
        assertArrayEquals(key, Base64.getDecoder().decode(storedKey));
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

    @Test
    void getOrCreateMasterKeyReturnsWrappedErrorWhenStoreFails() {
        TrackingCommandExecutor executor = new TrackingCommandExecutor(
                new CommandRunner.CommandResult(1, "", ""),
                new CommandRunner.CommandResult(7, "", "cannot persist secret")
        );
        LinuxSecretServiceProvider provider = provider(executor);

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider.getOrCreateMasterKey(new SecureMetadata(), null, true));

        assertEquals("No se pudo guardar la clave en Secret Service: cannot persist secret", exception.getMessage());
    }

    private static final class TrackingCommandExecutor implements LinuxSecretServiceProvider.CommandExecutor {
        private final List<CommandRunner.CommandResult> results;
        private final List<CommandCall> calls = new ArrayList<>();
        private int index;

        private TrackingCommandExecutor(CommandRunner.CommandResult... results) {
            this.results = List.of(results);
        }

        @Override
        public CommandRunner.CommandResult run(List<String> command, byte[] stdin) {
            calls.add(new CommandCall(command, stdin));
            return results.get(index++);
        }
    }

    private record CommandCall(List<String> command, byte[] stdin) {
    }
}
