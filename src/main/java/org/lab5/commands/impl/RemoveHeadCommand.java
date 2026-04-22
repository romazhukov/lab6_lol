package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.models.Organization;

public class RemoveHeadCommand extends AbstractCommand {

    public RemoveHeadCommand() {
        super("remove_head", "remove and print the first element");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        Organization organization = ctx.getCollectionManager().removeHead();

        if (organization == null) {
            ctx.getConsole().println("(collection is empty)");
            return;
        }

        ctx.getConsole().println("Removed:");
        ctx.getConsole().println(organization.toString());
    }
}