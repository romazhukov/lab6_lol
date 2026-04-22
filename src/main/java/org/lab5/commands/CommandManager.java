package org.lab5.commands;

import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommandManager {
    private final Map<String, Command> commands = new LinkedHashMap<>();
    private CommandContext context;

    public void setContext(CommandContext context) {
        this.context = context;
        context.attachCommandManager(this);
    }

    public void register(Command command) {
        commands.put(command.getName(), command);
    }

    public void handle(String line) {
        if (context == null) {
            throw new IllegalStateException("CommandContext is not set");
        }

        if (line == null) {
            return;
        }

        line = line.trim();
        if (line.isEmpty()) {
            return;
        }

        String name;
        String[] args;

        if (line.startsWith("execute_script")) {
            name = "execute_script";
            String rest = line.substring("execute_script".length()).trim();
            if (rest.isEmpty()) {
                args = new String[0];
            } else {
                args = new String[]{rest};
            }
        } else {
            String[] parts = line.split("\\s+");
            name = parts[0];
            args = new String[Math.max(0, parts.length - 1)];

            for (int i = 1; i < parts.length; i++) {
                args[i - 1] = parts[i];
            }
        }

        Command command = commands.get(name);

        if (command == null) {
            context.getConsole().printError("Unknown command: " + name);
            return;
        }

        try {
            command.execute(context, args);
        } catch (CommandExecutionException e) {
            context.getConsole().printError(e.getMessage());
        } catch (ValidationException e) {
            context.getConsole().printError("Validation error: " + e.getMessage());
        } catch (Exception e) {
            context.getConsole().printError("Unexpected error: " + e.getMessage());
        }
    }

    public Map<String, Command> getCommands() {
        return commands;
    }
}