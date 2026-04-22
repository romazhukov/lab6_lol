package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.exceptions.ScriptRecursionException;

import java.io.FileNotFoundException;
import java.nio.file.Path;

public class ExecuteScriptCommand extends AbstractCommand {

    public ExecuteScriptCommand() {
        super("execute_script", "execute commands from file");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            ctx.getConsole().printError("usage: execute_script <file_name>");
            return;
        }

        Path path = Path.of(args[0]);

        try {
            ctx.getScriptManager().enterScript(path);
        } catch (ScriptRecursionException e) {
            ctx.getConsole().printError(e.getMessage());
            return;
        } catch (FileNotFoundException e) {
            ctx.getConsole().printError("file not found: " + path);
            return;
        }

        try {
            while (true) {
                String line = ctx.getInputManager().readLine();

                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                ctx.getCommandManager().handle(line);
            }
        } finally {
            ctx.getScriptManager().leaveScript();
        }

        ctx.getConsole().println("Finished script: " + path);
    }
}