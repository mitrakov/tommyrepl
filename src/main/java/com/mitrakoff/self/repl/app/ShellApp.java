package com.mitrakoff.self.repl.app;

import com.mitrakoff.self.repl.Shell;
import org.beryx.textio.TerminalProperties;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.web.RunnerData;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

public class ShellApp implements BiConsumer<TextIO, RunnerData> {
    private TextTerminal<?> terminal;

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        terminal = textIO.getTextTerminal();
        final TerminalProperties<?> props = terminal.getProperties();
        props.setPromptColor(Color.WHITE);

        Path curDir = Paths.get(System.getProperty("user.home"));

        while (true) {
            try {
                final String cmd = textIO.newStringInputReader().read("> ").trim();
                if (cmd.equals("exit") || cmd.equals("quit")) break;
                if (cmd.equals("cd")) {
                    curDir = Paths.get(System.getProperty("user.home"));
                    println(curDir.toString(), Color.CYAN, false, true);
                }
                else if (cmd.startsWith("cd")) {
                    final String newStr = cmd.substring(3).trim();
                    final Path newPath = curDir.resolve(newStr).normalize().toAbsolutePath();
                    if (newPath.toFile().exists()) {
                        curDir = newPath;
                        println(curDir.toString(), Color.CYAN, false, true);
                    } else throw new RuntimeException("cd: no such file or directory: " + newStr);
                }
                else println(Shell.runBash(cmd, curDir), Color.WHITE, false, false);
            } catch (Exception e) {
                println(e.getLocalizedMessage(), Color.RED, true, false);
            }
        }

        textIO.dispose("Good bye!");
    }

    private void println(String s, Color colour, boolean bold, boolean italic) {
        final TerminalProperties<?> props = terminal.getProperties();

        props.setPromptColor(colour);
        props.setPromptBold(bold);
        props.setPromptItalic(italic);
        terminal.println(s);
        props.setPromptColor(Color.WHITE);
        props.setPromptBold(false);
        props.setPromptItalic(false);
    }
}
