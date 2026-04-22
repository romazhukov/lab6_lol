package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;

public class ExitCommand extends AbstractCommand {

    public ExitCommand() {
        super("exit", "exit the program without saving");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        ctx.getRunning().set(false);
        ctx.getConsole().println("Goodbye.");
    }
}