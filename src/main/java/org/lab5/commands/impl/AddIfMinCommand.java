package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;
import org.lab5.io.OrganizationBuilder;
import org.lab5.models.Organization;

import java.time.ZonedDateTime;

public class AddIfMinCommand extends AbstractCommand {

    public AddIfMinCommand() {
        super("add_if_min", "add element only if it is less than current minimum");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) throws CommandExecutionException, ValidationException {
        OrganizationBuilder builder = new OrganizationBuilder(ctx.getConsole(), ctx.getInputManager());
        Organization.Draft draft = builder.readOrganizationDraft();

        Organization organization = ctx.getCollectionManager().addIfMin(
                draft,
                ctx.getIdGenerator(),
                ZonedDateTime.now()
        );

        if (organization == null) {
            ctx.getConsole().println("Element was not added.");
        } else {
            ctx.getConsole().println("Added organization id=" + organization.getId());
        }
    }
}