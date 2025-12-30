package com.mitrakoff.self.repl;

import org.beryx.textio.*;
import org.beryx.textio.web.WebTextTerminal;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class WebTabHandler {
    public static final String NBSP = "\u00A0"; // browsers use non-breaking space (&nbsp;) instead of a usual space
    public static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    private final TextIO textIO;
    private final WebTextTerminal term;
    private final ExecutorService slave = Executors.newSingleThreadExecutor();
    private final LinkedTransferQueue<String> buffer = new LinkedTransferQueue<>();

    private Future<?> task;
    private Process process;
    private Path curDir = Paths.get(System.getProperty("user.home"));

    public WebTabHandler(TextIO textIO) {
        this.textIO = textIO;
        term = (WebTextTerminal) textIO.getTextTerminal();
        term.setBookmark("clear");
        term.getProperties().setPromptColor(Color.WHITE);
        term.registerHandler("ctrl L", t -> {
            t.resetToBookmark("clear");
            print(">");
            return new ReadHandlerData(ReadInterruptionStrategy.Action.CONTINUE);
        });
        term.registerUserInterruptHandler(t -> {
            System.out.println("CTRL+C");
            interrupt();
            printError("Task cancelled.");
        }, true);
    }

    public void run() {
        final String password = textIO.newStringInputReader().withInputMasking(true).read("password: ");
        if (!password.equals(System.getenv("WEB_PASSWORD"))) {
            printError("Invalid password");
            term.getProperties().setInputColor(Color.BLACK);
            return;
        } else {
            term.resetToBookmark("clear");
            printLineCyan("Welcome to Tommy REPL!\n - use CTRL+C to interrupt current command\n - use CTRL+L to clear console");
            printLineCyan(" - run \"exit\" to close the session\n - run \"shutdown\" to stop the server");
            task = slave.submit(() -> {
                try {
                    runBash(isWindows ? "date /t && ver && whoami" : "date && uname -a && whoami", curDir);
                } catch (Exception e) { printError(e.getMessage()); }
            });
        }

        while (true) {
            final String cmd = textIO.newStringInputReader().withMinLength(0).read().replace(NBSP, " ").trim();
            if (!buffer.isEmpty()) {
                printLine("");
                for (int i=0; i<128 && !buffer.isEmpty(); i++) {    // i<128 is a guard for infinite commands like "top"
                    String s;
                    if ((s = buffer.poll()) != null) {
                        if (s.equals("🜐")) print(">");
                        else if (s.startsWith("Exit code: ")) printError(s);
                        else printLine(s);
                    }
                }
            }
            if (cmd.isEmpty()) continue;
            if (cmd.equals("exit") || cmd.equals("quit")) break;
            else if (cmd.equals("shutdown")) {
                interrupt();
                slave.shutdown();
                textIO.dispose("Server killed. Forever.");
            } else if (cmd.equals("clear") || cmd.equals("cls")) {
                term.resetToBookmark("clear");
                print(">");
            } else if (cmd.equals("cd")) {
                curDir = Paths.get(System.getProperty("user.home"));
                printLineCyan(curDir.toString());
                print(">");
            } else if (cmd.startsWith("cd ")) {
                final String newStr = cmd.substring(3).trim();
                final Path newPath = curDir.resolve(newStr).normalize().toAbsolutePath();
                if (newPath.toFile().exists()) {
                    curDir = newPath;
                    printLineCyan(curDir.toString());
                    print(">");
                } else printError("cd: no such file or directory: " + newStr);
            } else { // usual Bash command
                task = slave.submit(() -> {
                    try {
                        runBash(cmd, curDir);
                    } catch (Exception e) { printError(e.getMessage()); }
                });
            }
        }

        interrupt();
        slave.shutdown();
        printError("Your session is over. Good bye...");
        term.getProperties().setInputColor(Color.BLACK);
    }

    private void runBash(String command, Path pwd) throws Exception {
        // process builder setup
        final ProcessBuilder pb = isWindows
            ? new ProcessBuilder("cmd.exe", "/c", command)
            : new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(true);   // redirect error stream to standard output stream for single stream reading
        if (pwd != null)
            pb.directory(pwd.toFile()); // set up working directory for "cd" commands

        // start process
        process = pb.start();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.put(line);
                term.postUserInput(""); // signal to main UI thread to interrupt textIO.read()
            }
        }

        // wait for the process to complete gracefully
        final boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            process.destroy(); // force terminate if it times out
            throw new TimeoutException("Command timed out");
        }

        // check the exit code value
        final int exitValue = process.exitValue();
        if (exitValue != 0) {
            buffer.put("Exit code: " + exitValue);
            term.postUserInput("");
        }

        // hack: print welcome ">"
        buffer.put("🜐");
        term.postUserInput("");
    }

    private void interrupt() {
        if (process != null)
            process.destroy();
        if (task != null)
            task.cancel(true);
        buffer.clear();
    }

    private synchronized void print(String s) {
        if (s == null) return;
        final String t = s.replace(" ", NBSP);
        term.executeWithPropertiesConfigurator(
                p -> p.setPromptColor(Color.YELLOW), (term) -> term.print(t));
    }

    private synchronized void printLine(String s) {
        if (s == null) return;
        final String t = s.replace(" ", NBSP);
        term.executeWithPropertiesConfigurator(
                p -> p.setPromptColor(Color.WHITE), (term) -> term.println(t));
    }

    private synchronized void printLineCyan(String s) {
        if (s == null) return;
        final String t = s.replace(" ", NBSP);
        term.executeWithPropertiesConfigurator(p -> {
            p.setPromptColor(Color.CYAN);
            p.setPromptBold(false);
        }, (term) -> term.println(t));
    }

    private synchronized void printError(String s) {
        if (s == null) return;
        final String t = s.replace(" ", NBSP);
        term.executeWithPropertiesConfigurator(p -> {
            p.setPromptColor(Color.RED);
            p.setPromptBold(true);
        }, (term) -> term.println(t));
    }
}
