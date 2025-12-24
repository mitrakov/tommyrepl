package com.mitrakoff.self.repl;

import org.beryx.textio.TextIO;
import org.beryx.textio.web.SparkTextIoApp;
import org.beryx.textio.web.WebTextTerminal;
import com.mitrakoff.self.repl.app.ShellIO;

public class TextIoDemo {
    public static void main(String[] args) {
        final WebTextTerminal t = new WebTextTerminal();
        t.init();
        new WebTextIoExecutor()
                .withPort(8080)
                .execute(new SparkTextIoApp(new ShellIO(), (WebTextTerminal)(new TextIO(t)).getTextTerminal()));
    }
}
