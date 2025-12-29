package com.mitrakoff.self.repl;

import org.beryx.textio.web.SparkTextIoApp;
import org.beryx.textio.web.TextIoApp;
import org.beryx.textio.web.WebTextTerminal;
import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Main {
    public static int port = 8020;

    public static void main(String[] args) {
        if (!System.getenv().containsKey("WEB_PASSWORD")) {
            System.err.println("Provide WEB_PASSWORD env variable");
            System.exit(1);
        }
        final WebTextTerminal term = new WebTextTerminal();
        term.init();
        runWebApp(new SparkTextIoApp((t, d) -> new WebTabHandler(t).run(), term));
    }

    public static void runWebApp(TextIoApp<?> app) {
        final Consumer<String> exit = sessionId -> Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> System.exit(0), 1, TimeUnit.SECONDS);

        app.withOnDispose(exit)
                .withOnAbort(exit)
                .withPort(port)
                .withMaxInactiveSeconds(600)
                .withStaticFilesLocation("public-html")
                .init();

        final String url = "http://localhost:" + app.getPort();
        boolean browserStarted = false;
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
                browserStarted = true;
            } catch (Exception e) { e.printStackTrace(); }
        }
        if (!browserStarted)
            System.out.println("Please open the following link in your browser: " + url);
    }
}
