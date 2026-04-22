package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.models.Organization;

import java.util.List;

public class ShowCommand extends AbstractCommand {

    public ShowCommand() {
        super("show", "print all elements in sorted order");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        List<Organization> organizations = ctx.getCollectionManager().getSortedView();

        if (organizations.isEmpty()) {
            ctx.getConsole().println("(empty)");
            return;
        }

        for (Organization organization : organizations) {
            ctx.getConsole().println(organization.toString());
        }
    }
}