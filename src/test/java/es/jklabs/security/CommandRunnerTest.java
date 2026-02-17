package es.jklabs.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CommandRunnerTest {

    private static List<String> javaFixtureCommand(String... args) {
        List<String> command = new ArrayList<>();
        command.add(getJavaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(CommandRunnerFixture.class.getName());
        command.addAll(List.of(args));
        return command;
    }

    private static String getJavaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    @Test
    void runCapturesStdoutStderrAndExitCode() throws IOException, InterruptedException {
        List<String> command = javaFixtureCommand(
                "--stdout=normal-output",
                "--stderr=error-output",
                "--exit=7"
        );

        CommandRunner.CommandResult result = CommandRunner.run(command, null);

        assertEquals(7, result.exitCode());
        assertEquals("normal-output", result.stdout());
        assertEquals("error-output", result.stderr());
    }

    @Test
    void runPassesStdinWhenProvided() throws IOException, InterruptedException {
        List<String> command = javaFixtureCommand("--echo-stdin");
        byte[] stdin = "input-from-stdin".getBytes(StandardCharsets.UTF_8);

        CommandRunner.CommandResult result = CommandRunner.run(command, stdin);

        assertEquals(0, result.exitCode());
        assertEquals("input-from-stdin", result.stdout());
        assertEquals("", result.stderr());
    }

    @Test
    void runClosesStdinWhenNotProvided() throws IOException, InterruptedException {
        List<String> command = javaFixtureCommand("--stdin-length");

        CommandRunner.CommandResult result = CommandRunner.run(command, null);

        assertEquals(0, result.exitCode());
        assertEquals("0", result.stdout());
        assertEquals("", result.stderr());
    }

    @Test
    void runThrowsWhenCommandDoesNotExist() {
        List<String> command = List.of("command-that-does-not-exist-1234567890");

        assertThrows(IOException.class, () -> CommandRunner.run(command, null));
    }
}
