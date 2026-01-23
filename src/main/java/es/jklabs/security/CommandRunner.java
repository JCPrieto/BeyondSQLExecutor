package es.jklabs.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommandRunner {

    private CommandRunner() {

    }

    public static CommandResult run(List<String> command, byte[] stdin) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        if (stdin != null) {
            process.getOutputStream().write(stdin);
            process.getOutputStream().flush();
            process.getOutputStream().close();
        } else {
            process.getOutputStream().close();
        }
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        readFully(process.getInputStream(), stdout);
        readFully(process.getErrorStream(), stderr);
        int exit = process.waitFor();
        return new CommandResult(exit, stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private static void readFully(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    public record CommandResult(int exitCode, String stdout, String stderr) {
    }
}
