package org.lab5.app;

import org.lab5.commands.CommandContext;
import org.lab5.commands.CommandManager;

public class ConsoleApplication implements Runnable {
    private final CommandContext context;
    private final CommandManager commandManager;

    public ConsoleApplication(CommandContext context, CommandManager commandManager) {
        this.context = context;
        this.commandManager = commandManager;
    }

    @Override
    public void run() {
        context.getConsole().println("Organization collection manager. Type \"help\" for commands.");
        context.getConsole().println("Data file: " + context.getFileManager().getDataFile());

        while (context.getRunning().get()) {
            context.getConsole().print("$ ");
            String line = context.getInputManager().readLine();

            if (line == null) {
                break;
            }

            commandManager.handle(line);
        }

        context.getScriptManager().closeAll();
    }
}