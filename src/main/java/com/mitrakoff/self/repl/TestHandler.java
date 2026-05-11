package com.mitrakoff.self.repl;

import org.beryx.textio.TextIO;
import org.beryx.textio.web.WebTextTerminal;
import java.awt.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class TestHandler {
    public static final String NBSP = "\u00A0"; // browsers use non-breaking space (&nbsp;) instead of a usual space
    private final TextIO textIO;                                                     // ref to textIO
    private final WebTextTerminal term;                                              // ref to terminal instance

    public TestHandler(TextIO textIO) {
        this.textIO = textIO;
        term = (WebTextTerminal) textIO.getTextTerminal();
        term.setBookmark("clear");
        term.getProperties().setPromptColor(Color.WHITE);
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
            printLine("Welcome to REPL", Color.GREEN);
        }

        while (true) try {
            final String text = textIO.newStringInputReader().withMinLength(0).read().replace(NBSP, System.lineSeparator());
            Files.write(Paths.get(String.format("%d.txt", System.currentTimeMillis())), text.getBytes());
            printLine("OK", Color.CYAN);
        } catch (Exception e) { printError(e.getMessage()); }
    }

    private void log(String s) {
        System.out.println(LocalDateTime.now() + ": " + s);
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
