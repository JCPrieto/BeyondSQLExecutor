package es.jklabs.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
