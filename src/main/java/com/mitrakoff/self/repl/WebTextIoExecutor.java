package com.mitrakoff.self.repl;

import org.beryx.textio.web.TextIoApp;

import java.awt.*;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Allows executing code in a {@link org.beryx.textio.web.WebTextTerminal}
 * by configuring and initializing a {@link TextIoApp}.
 */
public class WebTextIoExecutor {
    private Integer port;

    public WebTextIoExecutor withPort(int port) {
        this.port = port;
        return this;
    }

    public void execute(TextIoApp<?> app) {
        Consumer<String> stopServer = sessionId -> Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            System.exit(0);
        }, 2, TimeUnit.SECONDS);

        app.withOnDispose(stopServer)
            .withOnAbort(stopServer)
            .withPort(port)
            .withMaxInactiveSeconds(600)
            .withStaticFilesLocation("public-html")
            .init();

        String url = "http://localhost:" + app.getPort() + "/web-demo.html";
        boolean browserStarted = false;
        if(Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
                browserStarted = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(!browserStarted) {
            System.out.println("Please open the following link in your browser: " + url);
        }
    }
}
