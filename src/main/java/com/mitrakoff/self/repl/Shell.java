package com.mitrakoff.self.repl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Shell {
    public static String runBash(String command, Path pwd) throws Exception {
        // process builder setup
        final ProcessBuilder pb = System.getProperty("os.name").toLowerCase().contains("win")
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(true);   // redirect error stream to standard output stream for single stream reading
        if (pwd != null)
            pb.directory(pwd.toFile()); // set up working directory for "cd" commands


        // start process
        final Process process = pb.start();
        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null)
                output.append(line).append(System.lineSeparator());
        }

        // wait for the process to complete gracefully
        final boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroy(); // force terminate if it times out
            throw new TimeoutException("command timed out");
        }

        // check the exit code value
        final int exitValue = process.exitValue();
        if (exitValue != 0)
            throw new RuntimeException("exit code: " + exitValue + System.lineSeparator() + output);

        return output.toString();
    }
}
