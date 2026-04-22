package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;
import org.lab5.io.OrganizationBuilder;
import org.lab5.models.Organization;

public class UpdateCommand extends AbstractCommand {

    public UpdateCommand() {
        super("update", "update element by id");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) throws CommandExecutionException, ValidationException {
        if (args.length != 1) {
            ctx.getConsole().printError("usage: update <id>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            ctx.getConsole().printError("id must be an integer");
            return;
        }

        Organization oldOrganization = ctx.getCollectionManager().findById(id);
        if (oldOrganization == null) {
            ctx.getConsole().printError("organization with id=" + id + " not found");
            return;
        }

        OrganizationBuilder builder = new OrganizationBuilder(ctx.getConsole(), ctx.getInputManager());
        Organization.Draft draft = builder.readOrganizationDraftForUpdate(oldOrganization);

        Organization newOrganization = ctx.getCollectionManager().update(id, draft);
        ctx.getConsole().println("Updated organization id=" + newOrganization.getId());
    }
}