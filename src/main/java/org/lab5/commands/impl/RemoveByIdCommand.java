package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;

public class RemoveByIdCommand extends AbstractCommand {

    public RemoveByIdCommand() {
        super("remove_by_id", "remove element by id");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        if (args.length != 1) {
            ctx.getConsole().printError("usage: remove_by_id <id>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            ctx.getConsole().printError("id must be an integer");
            return;
        }

        boolean removed = ctx.getCollectionManager().removeById(id);

        if (!removed) {
            ctx.getConsole().printError("organization with id=" + id + " not found");
            return;
        }

        ctx.getConsole().println("Removed organization id=" + id);
    }
}