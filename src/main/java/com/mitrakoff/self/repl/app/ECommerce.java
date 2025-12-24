package com.mitrakoff.self.repl.app;

import org.beryx.textio.TerminalProperties;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.web.RunnerData;

import java.util.function.BiConsumer;

/**
 * A simple application illustrating the use of TextIO.
 */
public class ECommerce implements BiConsumer<TextIO, RunnerData> {
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new ECommerce().accept(textIO, null);
    }

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        TextTerminal<?> terminal = textIO.getTextTerminal();
        String initData = (runnerData == null) ? null : runnerData.getInitData();
        AppUtil.printGsonMessage(terminal, initData);

        TerminalProperties<?> props = terminal.getProperties();

        props.setPromptBold(true);
        props.setPromptUnderline(true);
        props.setPromptColor("cyan");
        terminal.println("Order details");

        props.setPromptUnderline(false);
        props.setPromptBold(false);
        props.setInputColor("blue");
        props.setInputItalic(true);
        String product = textIO.newStringInputReader().read("Product name");

        int quantity = textIO.newIntInputReader()
                .withMinVal(1)
                .withMaxVal(10)
                .read("Quantity");

        props.setPromptBold(true);
        props.setPromptUnderline(true);
        props.setPromptColor("green");
        terminal.println("\nShipping Information");

        props.setPromptBold(false);
        props.setPromptUnderline(false);
        props.setInputColor("yellow");
        String city = textIO.newStringInputReader().read("City");
        String street = textIO.newStringInputReader().read("Street Address");
        String shippingOptions = textIO.newStringInputReader()
                .withNumberedPossibleValues("Standard Shipping", "Two-Day Shipping", "One-Day Shipping")
                .read("Shipping Options");


        props.setPromptBold(true);
        props.setPromptUnderline(true);
        props.setPromptColor("white");
        terminal.println("\nPayment Details");

        props.setPromptBold(false);
        props.setPromptUnderline(false);
        props.setInputColor("magenta");
        String paymentType = textIO.newStringInputReader()
                .withNumberedPossibleValues("PayPal", "MasterCard", "VISA")
                .read("Payment Type");
        String owner = textIO.newStringInputReader().read("Account Owner");


        props.setPromptBold(true);
        props.setPromptUnderline(true);
        props.setPromptColor("red");
        terminal.println("\nOrder Overview");

        props.setPromptBold(false);
        props.setPromptUnderline(false);
        props.setPromptColor("yellow");
        terminal.printf("Product: %s\nQuantity: %d\n", product, quantity);

        terminal.printf("\n%s to %s, %s\n", shippingOptions, street, city);

        terminal.printf("%s is paying with %s.\n", owner, paymentType);

        props.setPromptColor("green");
        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
        textIO.dispose("Payment receipt sent to " + owner + ".");
    }

    @Override
    public String toString() {
        return "E-Commerce: placing an online order.\n" +
                "(Properties are dynamically changed at runtime using hard-coded values.\n" +
                "Properties file: " + getClass().getSimpleName() + ".properties.)";
    }
}
