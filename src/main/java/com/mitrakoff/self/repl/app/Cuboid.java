package com.mitrakoff.self.repl.app;

import org.beryx.textio.*;
import org.beryx.textio.web.RunnerData;

import java.util.function.BiConsumer;

/**
 * A simple application illustrating the use of TextIO.
 */
public class Cuboid implements BiConsumer<TextIO, RunnerData> {
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new Cuboid().accept(textIO, null);
    }

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        TextTerminal<?> terminal = textIO.getTextTerminal();
        String initData = (runnerData == null) ? null : runnerData.getInitData();
        AppUtil.printGsonMessage(terminal, initData);

        terminal.executeWithPropertiesPrefix("custom.title", t -> t.print("Cuboid dimensions: "));
        terminal.println();

        double length = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .withPropertiesPrefix("custom.length")
                .read("Length");

        double width = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .withPropertiesPrefix("custom.width")
                .read("Width");

        double height = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .withPropertiesPrefix("custom.height")
                .read("Height");


        terminal.executeWithPropertiesPrefix("custom.title",
                t -> t.print("The volume of your cuboid is: " + length * width * height));
        terminal.println();

        textIO.newStringInputReader()
                .withMinLength(0)
                .withPropertiesPrefix("custom.neutral")
                .read("\nPress enter to terminate...");
        textIO.dispose();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": computing the volume of a cuboid.\n" +
                "(Properties are dynamically changed at runtime using custom properties values.\n" +
                "Properties file: " + getClass().getSimpleName() + ".properties.)";
    }
}
