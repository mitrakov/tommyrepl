package com.mitrakoff.self.repl.app;

import org.beryx.textio.TextTerminal;

public class AppUtil {
    public static void printGsonMessage(TextTerminal<?> terminal, String initData) {
        if(initData != null && !initData.isEmpty()) {
            String message = initData.trim();
            if(message != null && !message.isEmpty()) {
                terminal.println(message);
            }
        }
    }
}
