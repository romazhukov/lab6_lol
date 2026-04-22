package org.lab5.managers;

import org.lab5.exceptions.ScriptRecursionException;
import org.lab5.io.ScriptScannerProvider;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class ScriptManager {
    private final Deque<Path> paths = new ArrayDeque<>();
    private final Deque<Scanner> scanners = new ArrayDeque<>();
    private final Set<Path> activePaths = new HashSet<>();

    public boolean isScriptMode() {
        return !scanners.isEmpty();
    }

    public void enterScript(Path path) throws ScriptRecursionException, FileNotFoundException {
        Path fullPath = path.toAbsolutePath().normalize();

        if (activePaths.contains(fullPath)) {
            throw new ScriptRecursionException("recursive execute_script detected: " + fullPath);
        }

        Scanner scanner = ScriptScannerProvider.open(fullPath);

        activePaths.add(fullPath);
        paths.push(fullPath);
        scanners.push(scanner);
    }

    public void leaveScript() {
        if (scanners.isEmpty()) {
            return;
        }

        Path path = paths.pop();
        Scanner scanner = scanners.pop();

        activePaths.remove(path);
        scanner.close();
    }

    public void closeAll() {
        while (!scanners.isEmpty()) {
            leaveScript();
        }
    }

    public String readLineFromTopScanner() {
        if (scanners.isEmpty()) {
            return null;
        }

        Scanner scanner = scanners.peek();
        if (!scanner.hasNextLine()) {
            return null;
        }

        return scanner.nextLine();
    }
}