package org.lab5.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public final class ScriptScannerProvider {

    private ScriptScannerProvider() {
    }

    public static Scanner open(Path path) throws FileNotFoundException {
        return new Scanner(
                new InputStreamReader(
                        new FileInputStream(path.toFile()),
                        StandardCharsets.UTF_8
                )
        );
    }
}