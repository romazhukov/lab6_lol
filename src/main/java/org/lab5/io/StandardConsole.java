package org.lab5.io;

import java.util.Scanner;

public class StandardConsole implements Console {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void print(String text) {
        System.out.print(text);
    }

    @Override
    public void println(String text) {
        System.out.println(text);
    }

    @Override
    public void printError(String text) {
        System.err.println(text);
    }

    @Override
    public String readLineInteractive() {
        return scanner.nextLine();
    }
}