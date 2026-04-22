package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.Command;
import org.lab5.commands.CommandContext;

public class HelpCommand extends AbstractCommand {

    public HelpCommand() {
        super("help", "print help for available commands");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        ctx.getConsole().println("Available commands:");
        for (Command command : ctx.getCommandManager().getCommands().values()) {
            ctx.getConsole().println(" - " + command.getName() + " : " + command.getDescription());
        }
    }
}