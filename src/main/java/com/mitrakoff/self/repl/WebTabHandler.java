package com.mitrakoff.self.repl;

import org.beryx.textio.*;
import org.beryx.textio.web.WebTextTerminal;
import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class WebTabHandler {
    public static final String NBSP = "\u00A0"; // browsers use non-breaking space (&nbsp;) instead of a usual space
    public static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    private final TextIO textIO;                                                     // ref to textIO
    private final WebTextTerminal term;                                              // ref to terminal instance
    private final ExecutorService slave = Executors.newSingleThreadExecutor();       // bash executor, runs async
    private final LinkedTransferQueue<String> buffer = new LinkedTransferQueue<>();  // stdout/stderr from slave
    private final Map<String, String> extraEnv = new HashMap<>(System.getenv());     // ENV for a new process

    private volatile Future<?> task;           // bash command task (to implement "CTRL+C")
    private volatile Process process;          // bash command internal OS process
    private volatile BufferedWriter bWriter;   // writer to be connected to "stdin" of REPLs like spark-shell, scala, psql, etc.
    private Path curDir = Paths.get(System.getProperty("user.home"));    // current working directory, for "cd"

    public WebTabHandler(TextIO textIO) {
        this.textIO = textIO;
        term = (WebTextTerminal) textIO.getTextTerminal();
        term.setBookmark("clear");
        term.getProperties().setPromptColor(Color.WHITE);
        term.registerHandler("ctrl L", t -> {
            t.resetToBookmark("clear");
            printCaret();
            return new ReadHandlerData(ReadInterruptionStrategy.Action.CONTINUE);
        });
        term.registerUserInterruptHandler(t -> {
            log("CTRL+C");
            interrupt();
            printError("Task cancelled.");
        }, true);
        extraEnv.put("TERM", "dumb"); // some REPLs (like old "spark-shell" v3.2.1) may try to control TTY and then hang forever
    }

    public void run() {
        final String password = textIO.newStringInputReader().withInputMasking(true).read("password: ");
        if (!password.equals(System.getenv("WEB_PASSWORD"))) {
            log("Invalid password detected");
            printError("Invalid password");
            term.getProperties().setInputColor(Color.BLACK);
            return;
        } else {
            log("Successful login");
            term.resetToBookmark("clear");
            printLine("Welcome to Tommy REPL (v1.0.3)", Color.GREEN);
            printLine(" - use CTRL+C to interrupt current command\n - use CTRL+L to clear console", Color.CYAN);
            printLine(" - run \"exit\" to close the session\n - run \"shutdown\" to stop the server", Color.CYAN);
            printLine(" - note that rich TTY features are disabled (e.g. --password); provide your passwords in ENV", Color.CYAN);
            task = slave.submit(() -> {
                try {
                    runBash(isWindows ? "date /t && ver && whoami" : "date && uname -a && whoami", curDir);
                } catch (Exception e) { printError(e.getMessage()); }
            });
        }

        while (true) try {
            // .read() is the only way in TextIO to handle "CTRL+C" (and other interrupt handlers), that's why all bash commands
            // must be run in a separate thread and push output to a shared buffer (instead of direct writing to terminal)
            final String cmd = textIO.newStringInputReader().withMinLength(0).read().replace(NBSP, " ").trim();
            // first, we have to print messages from the buffer filled by bash executor (again it is made only for "CTRL+C" feature)
            if (!buffer.isEmpty()) {
                printLine("");
                for (int i=0; i<128 && !buffer.isEmpty(); i++) {    // i<128 is a guard for infinite commands like "top"
                    String s;
                    if ((s = buffer.poll()) != null) {
                        if (s.equals("🜐")) printCaret();
                        else if (s.startsWith("Exit code: ")) printError(s);
                        else printLine(s);
                    }
                }
            }
            if (cmd.isEmpty()) continue;
            if (bWriter != null) {      // handle subprocess REPL, e.g. spark-shell, psql, redis-cli, scala, etc.
                bWriter.write(cmd + System.lineSeparator());
                bWriter.flush();
            }
            else if (cmd.equals("exit") || cmd.equals("quit")) break;
            else if (cmd.equals("shutdown")) {
                interrupt();
                slave.shutdown();
                printError("Web server shut down...");
                textIO.dispose("Web server shut down...");
            } else if (cmd.equals("clear") || cmd.equals("cls")) {
                term.resetToBookmark("clear");
                printCaret();
            } else if (cmd.equals("cd")) {
                curDir = Paths.get(System.getProperty("user.home"));
                printLine(curDir.toString(), Color.CYAN);
                printCaret();
            } else if (cmd.startsWith("cd ")) {
                final String newStr = cmd.substring(3).trim();
                final Path newPath = curDir.resolve(newStr).normalize().toAbsolutePath();
                if (newPath.toFile().exists()) {
                    curDir = newPath;
                    printLine(curDir.toString(), Color.CYAN);
                    printCaret();
                } else printError("cd: no such file or directory: " + newStr);
            } else if (cmd.equals("env")) {
                System.getenv().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach((e) ->
                        printLine(e.getKey() + "=" + (e.getKey().equals("WEB_PASSWORD") ? "••••••••••" : e.getValue())));
                printCaret();
            } else if (cmd.startsWith("export ")) {
                final String newStr = cmd.substring(7).trim();
                if (!newStr.contains("=")) {
                    printError("Format: export VAR=VALUE");
                    printCaret();
                } else {
                    final String[] arr = newStr.split("=");
                    final String key = arr[0];
                    final String value = arr[1];
                    extraEnv.put(key, value);
                    printLine("Variable set: " + key + "=" + value, Color.CYAN);
                    printCaret();
                }
            } else if (cmd.startsWith("unset ")) {
                final String key = cmd.substring(6).trim();
                extraEnv.remove(key);
                printLine("Variable unset: " + key, Color.CYAN);
                printCaret();
            } else {
                log(cmd);
                task = slave.submit(() -> {
                    try {
                        runBash(cmd, curDir); // usual Bash command
                    } catch (Exception e) { printError(e.getMessage()); }
                });
            }
        } catch (Exception e) { printError(e.getMessage()); }

        interrupt();
        slave.shutdown();
        printError("Your session is over. Good bye...");
        term.getProperties().setInputColor(Color.BLACK);
    }

    private void runBash(String command, Path pwd) throws Exception {
        // this method is being run asynchronously. DO NOT print to WebTerminal here, print to "buffer" instead.

        // process builder setup
        final ProcessBuilder pb = isWindows
            ? new ProcessBuilder("cmd.exe", "/c", command)
            : new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(true);   // redirect error stream to standard output stream for single stream reading
        pb.environment().putAll(extraEnv); // update ENV in case when a user adds "export X=Y" commands to active session
        if (pwd != null)
            pb.directory(pwd.toFile()); // set up working directory for "cd" commands

        // start process
        process = pb.start();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            bWriter = writer;
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.put(line);
                term.postUserInput(""); // signal to main UI thread to interrupt textIO.read()
            }
        } finally {
            bWriter = null;
        }

        // wait for the process to complete gracefully
        final boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished)
            process.destroy(); // force terminate if it times out

        // check the exit code value
        final int exitValue = process.exitValue();
        if (exitValue != 0) {
            buffer.put("Exit code: " + exitValue);
            term.postUserInput("");
        }

        // life-hack: print welcome message ">" into the end of the buffer (due to async nature of the app)
        buffer.put("🜐");
        term.postUserInput("");
    }

    private void interrupt() {
        if (process != null)
            process.destroy();
        if (task != null)
            task.cancel(true);
        if (bWriter != null) try {
            bWriter.close();
        } catch (IOException e) { printError(e.getMessage()); } finally {
            bWriter = null;
        }

        buffer.clear();
    }

    private void log(String s) {
        System.out.println(LocalDateTime.now() + ": " + s);
    }

    private synchronized void printCaret() {
        final String t = System.lineSeparator() + ">";
        term.executeWithPropertiesConfigurator(
                p -> p.setPromptColor(Color.YELLOW), (term) -> term.print(t));
    }

    private synchronized void printLine(String s) {
        printLine(s, Color.WHITE);
    }

    private synchronized void printLine(String s, Color colour) {
        if (s == null) return;
        final String t = s.replace(" ", NBSP);
        term.executeWithPropertiesConfigurator(p ->
                p.setPromptColor(colour), (term) -> term.println(t));
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
