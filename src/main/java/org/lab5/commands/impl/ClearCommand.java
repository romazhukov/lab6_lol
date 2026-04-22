package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;

public class ClearCommand extends AbstractCommand {

    public ClearCommand() {
        super("clear", "clear the collection");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        ctx.getCollectionManager().clear();
        ctx.getConsole().println("Collection cleared.");
    }
}