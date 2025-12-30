package com.mitrakoff.self.repl;

import org.beryx.textio.web.*;
import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Specify port number.\nExample: java -jar tommyrepl.jar 8080");
            System.exit(1);
        }
        if (!System.getenv().containsKey("WEB_PASSWORD")) {
            System.err.println("Provide WEB_PASSWORD env variable");
            System.exit(2);
        }
        final WebTextTerminal term = new WebTextTerminal();
        term.init();
        runWebApp(new SparkTextIoApp((t, d) -> new WebTabHandler(t).run(), term), Integer.parseInt(args[0]));
    }

    public static void runWebApp(TextIoApp<?> app, int port) {
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
            System.out.println("Web server started at: " + url);
    }
}
