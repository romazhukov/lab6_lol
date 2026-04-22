package org.lab5.io;

import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;
import org.lab5.managers.ScriptManager;
import org.lab5.models.OrganizationType;
import org.lab5.util.ParserUtils;

public class InputManager {
    private final Console console;
    private final ScriptManager scriptManager;

    public InputManager(Console console, ScriptManager scriptManager) {
        this.console = console;
        this.scriptManager = scriptManager;
    }

    public String readLine() {
        if (scriptManager.isScriptMode()) {
            return scriptManager.readLineFromTopScanner();
        }
        return console.readLineInteractive();
    }

    public boolean isScriptMode() {
        return scriptManager.isScriptMode();
    }

    public String readValidatedLine(String prompt, StringChecker checker) throws CommandExecutionException {
        while (true) {
            console.print(prompt);
            String raw = readLine();

            if (raw == null) {
                throw new CommandExecutionException("unexpected end of input");
            }

            String error = checker.check(raw);
            if (error == null) {
                return raw;
            }

            console.printError(error);

            if (scriptManager.isScriptMode()) {
                throw new CommandExecutionException("script input error: " + error);
            }
        }
    }

    public int readInt(String prompt) throws CommandExecutionException {
        while (true) {
            console.print(prompt);
            String raw = readLine();

            if (raw == null) {
                throw new CommandExecutionException("unexpected end of input");
            }

            try {
                return ParserUtils.parseIntStrict(raw.trim());
            } catch (ValidationException e) {
                console.printError(e.getMessage());
                if (scriptManager.isScriptMode()) {
                    throw new CommandExecutionException("script input error: " + e.getMessage());
                }
            }
        }
    }

    public long readLong(String prompt) throws CommandExecutionException {
        while (true) {
            console.print(prompt);
            String raw = readLine();

            if (raw == null) {
                throw new CommandExecutionException("unexpected end of input");
            }

            try {
                return ParserUtils.parseLongStrict(raw.trim());
            } catch (ValidationException e) {
                console.printError(e.getMessage());
                if (scriptManager.isScriptMode()) {
                    throw new CommandExecutionException("script input error: " + e.getMessage());
                }
            }
        }
    }

    public double readDouble(String prompt) throws CommandExecutionException {
        while (true) {
            console.print(prompt);
            String raw = readLine();

            if (raw == null) {
                throw new CommandExecutionException("unexpected end of input");
            }

            try {
                return ParserUtils.parseDoubleStrict(raw.trim());
            } catch (ValidationException e) {
                console.printError(e.getMessage());
                if (scriptManager.isScriptMode()) {
                    throw new CommandExecutionException("script input error: " + e.getMessage());
                }
            }
        }
    }

    public float readFloat(String prompt) throws CommandExecutionException {
        while (true) {
            console.print(prompt);
            String raw = readLine();

            if (raw == null) {
                throw new CommandExecutionException("unexpected end of input");
            }

            try {
                return ParserUtils.parseFloatStrict(raw.trim());
            } catch (ValidationException e) {
                console.printError(e.getMessage());
                if (scriptManager.isScriptMode()) {
                    throw new CommandExecutionException("script input error: " + e.getMessage());
                }
            }
        }
    }

    public OrganizationType readOrganizationTypeNullable(String prompt) throws CommandExecutionException {
        while (true) {
            console.print(prompt + " [PUBLIC, TRUST, PRIVATE_LIMITED_COMPANY] (empty = null): ");
            String raw = readLine();

            if (raw == null) {
                throw new CommandExecutionException("unexpected end of input");
            }

            raw = raw.trim();
            if (raw.isEmpty()) {
                return null;
            }

            try {
                return ParserUtils.parseOrganizationType(raw);
            } catch (ValidationException e) {
                console.printError(e.getMessage());
                if (scriptManager.isScriptMode()) {
                    throw new CommandExecutionException("script input error: " + e.getMessage());
                }
            }
        }
    }

    @FunctionalInterface
    public interface StringChecker {
        String check(String raw);
    }
}