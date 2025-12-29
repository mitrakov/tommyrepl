package com.mitrakoff.self.repl;

import org.beryx.textio.TextIO;
import org.beryx.textio.web.WebTextTerminal;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class WebTabHandler {
    private final TextIO textIO;
    private final WebTextTerminal term;
    private final ExecutorService slave = Executors.newSingleThreadExecutor();
    private final StringBuffer buffer = new StringBuffer();

    private Future<?> task;
    private Process process;
    private Path curDir = Paths.get(System.getProperty("user.home"));

    public WebTabHandler(TextIO textIO) {
        this.textIO = textIO;
        term = (WebTextTerminal) textIO.getTextTerminal();
        term.setBookmark("clear");
        term.getProperties().setPromptColor(Color.WHITE);
        term.registerUserInterruptHandler(t -> {
            System.out.println("CTRL+C");
            if (task != null)
                task.cancel(true);
            if (process != null)
                process.destroy();
            printError("Task cancelled.");
        }, true);
    }

    public void run() {
        final String password = textIO.newStringInputReader().withInputMasking(true).read("password: ");
        if (!password.equals(System.getenv("WEB_PASSWORD"))) {
            printError("invalid password");
            term.getProperties().setInputColor(Color.BLACK);
            return;
        } else {
            term.resetToBookmark("clear");
            printLineCyan("Welcome to Tommy REPL!\n");
            task = slave.submit(() -> {
                try {
                    runBash("date", curDir);
                    runBash("uname -a", curDir);
                    runBash("whoami", curDir);
                } catch (Exception e) {
                    printError(e.getMessage());
                }
            });
        }

        while (true) {
            // here we replace "\u00A0" (&nbsp;) with a just space
            final String cmd = textIO.newStringInputReader().withMinLength(0).read().replace("\u00A0", " ").trim();
            if (task != null) {
                if (task.isDone()) {
                    task = null;
                    buffer.setLength(0);   // TODO here possibly bug on non-empty buffer; try "ls -la"
                } else {
                    printLine(buffer.toString());
                    buffer.setLength(0);
                    continue;
                }
            }
            if (cmd.isEmpty()) continue;
            if (cmd.equals("exit") || cmd.equals("quit")) break;
            else if (cmd.equals("shutdown")) {
                slave.shutdown();
                textIO.dispose("Server killed. Forever.");
            } else if (cmd.equals("cd")) {
                curDir = Paths.get(System.getProperty("user.home"));
                printLineCyan(curDir.toString());
            }
            else if (cmd.startsWith("cd")) {
                final String newStr = cmd.substring(3).trim();
                final Path newPath = curDir.resolve(newStr).normalize().toAbsolutePath();
                if (newPath.toFile().exists()) {
                    curDir = newPath;
                    printLineCyan(curDir.toString());
                } else printError("cd: no such file or directory: " + newStr);
            }
            else {
                task = slave.submit(() -> {
                    try {
                        runBash(cmd, curDir);
                    } catch (Exception e) {
                        printError(e.getMessage());
                    }
                });
            }
        }

        printLineCyan("Your session is over. Good bye...");
        term.getProperties().setInputColor(Color.BLACK);
    }

    private void runBash(String command, Path pwd) throws Exception {
        // process builder setup
        final ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(true);   // redirect error stream to standard output stream for single stream reading
        if (pwd != null)
            pb.directory(pwd.toFile()); // set up working directory for "cd" commands

        // start process
        process = pb.start();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append(System.lineSeparator());
                term.postUserInput("");
            }
        }

        // wait for the process to complete gracefully
        final boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            process.destroy(); // force terminate if it times out
            throw new TimeoutException("command timed out");
        }

        // check the exit code value
        final int exitValue = process.exitValue();
        if (exitValue != 0) {
            buffer.append("Exit code: ").append(exitValue);
            term.postUserInput("");
        }
    }

    private synchronized void printLine(String s) {
        term.executeWithPropertiesConfigurator(
                p -> p.setPromptColor(Color.WHITE), (term) -> term.println(s));
    }

    private synchronized void printLineCyan(String s) {
        term.executeWithPropertiesConfigurator(p -> {
            p.setPromptColor(Color.CYAN);
            p.setPromptBold(false);
        }, (term) -> term.println(s));
    }

    private synchronized void printError(String s) {
        term.executeWithPropertiesConfigurator(p -> {
            p.setPromptColor(Color.RED);
            p.setPromptBold(true);
        }, (term) -> term.println(s));
    }
}
