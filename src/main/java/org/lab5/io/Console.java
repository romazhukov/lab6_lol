package org.lab5.io;

public interface Console {
    void print(String text);

    void println(String text);

    void printError(String text);

    String readLineInteractive();
}