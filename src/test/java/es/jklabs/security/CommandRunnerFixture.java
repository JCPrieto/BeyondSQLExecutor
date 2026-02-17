package es.jklabs.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CommandRunnerFixture {

    public static void main(String[] args) throws IOException {
        int exitCode = 0;
        String stdout = "";
        String stderr = "";
        boolean echoStdin = false;
        boolean reportStdinLength = false;

        for (String arg : args) {
            if (arg.startsWith("--exit=")) {
                exitCode = Integer.parseInt(arg.substring("--exit=".length()));
            } else if (arg.startsWith("--stdout=")) {
                stdout = arg.substring("--stdout=".length());
            } else if (arg.startsWith("--stderr=")) {
                stderr = arg.substring("--stderr=".length());
            } else if ("--echo-stdin".equals(arg)) {
                echoStdin = true;
            } else if ("--stdin-length".equals(arg)) {
                reportStdinLength = true;
            }
        }

        if (echoStdin) {
            byte[] input = System.in.readAllBytes();
            System.out.write(input);
        } else if (reportStdinLength) {
            int length = System.in.readAllBytes().length;
            System.out.write(Integer.toString(length).getBytes(StandardCharsets.UTF_8));
        } else if (!stdout.isEmpty()) {
            System.out.write(stdout.getBytes(StandardCharsets.UTF_8));
        }

        if (!stderr.isEmpty()) {
            System.err.write(stderr.getBytes(StandardCharsets.UTF_8));
        }

        System.exit(exitCode);
    }
}
