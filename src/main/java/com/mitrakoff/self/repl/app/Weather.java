package com.mitrakoff.self.repl.app;

import org.beryx.textio.PropertiesConstants;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.web.RunnerData;

import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Illustrates some features introduced in version 3 of Text-IO: line reset, bookmarking etc.
 */
public class Weather implements BiConsumer<TextIO, RunnerData> {
    private boolean useCelsius;
    private boolean useKmh;
    private boolean useMbar;

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        TextTerminal<?> terminal = textIO.getTextTerminal();

        terminal.setBookmark("COUNTDOWN");
        terminal.println("Seconds to start:");
        for(int i=5; i>=0; i--) {
            terminal.resetLine();
            delay(500);
            terminal.print("      " + i);
            delay(500);
        }
        terminal.resetToBookmark("COUNTDOWN");

        terminal.println("################");
        terminal.println("# WEATHER INFO #");
        terminal.println("################");
        terminal.println();

        terminal.setBookmark("MAIN");
        while(true) {
            useCelsius = textIO.newBooleanInputReader().withDefaultValue(true).read("Display temperature in Celsius (Press 'N' for Fahrenheit)");
            useKmh = textIO.newBooleanInputReader().withDefaultValue(true).read("Display wind speed in km/h (Press 'N' for mph)");
            useMbar = textIO.newBooleanInputReader().withDefaultValue(true).read("Display atmospheric pressure in mbar (Press 'N' for kPa)");
            terminal.resetToBookmark("MAIN");

            terminal.getProperties().put(PropertiesConstants.PROP_PROMPT_STYLE_CLASS, "textterm-white-space-pre");
            terminal.executeWithPropertiesPrefix("pre", t -> {
                t.println("-------------------------------------------------------");
                t.println("  Temperature      Wind speed      Atmospheric pressure");
                t.println("-------------------------------------------------------");
                for(int i=0; i<20; i++) {
                    t.moveToLineStart();
                    delay(80);
                    t.print(getData());
                    delay(400);
                }
            });
            terminal.println();terminal.println();terminal.println();

            if(!textIO.newBooleanInputReader()
                    .withPropertiesPrefix("exit")
                    .withDefaultValue(true).read("Run again?")) break;
            terminal.resetToBookmark("MAIN");
        }

        textIO.dispose();
    }

    /**
     * Mocks a data retrieval operation.
     * @return the info text to be displayed
     */
    public String getData() {
        double temperature = 15 + 10 * Math.random();
        if(!useCelsius) {
            temperature = temperature * 9 / 5 + 32;
        }
        String sTemperature = String.format(Locale.US, (useCelsius ? "%4.1f °C" : "%4.1f °F"), temperature);

        double speed = 10 + 30 * Math.random();
        if(!useKmh) {
            speed /= 1.609344;
        }
        String sSpeed = String.format(Locale.US, (useKmh ? "%4.1f km/h" : "%4.1f mph"), speed);

        double pressure = 990 + 30 * Math.random();
        if(!useMbar) {
            pressure /= 10;
        }
        String sPressure = String.format(Locale.US, (useMbar ? "%6.1f mbar" : "%6.2f kPa"), pressure);

        return String.format(Locale.US, "%13s %15s %25s", sTemperature, sSpeed, sPressure);
    }

    public static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": displaying weather data.\n" +
                "(Illustrates line resetting, moving to line start, bookmarking and resetting to a bookmark.)";
    }
}
