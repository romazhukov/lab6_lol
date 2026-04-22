package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;
import org.lab5.io.OrganizationBuilder;
import org.lab5.models.Organization;

import java.time.ZonedDateTime;

public class AddCommand extends AbstractCommand {

    public AddCommand() {
        super("add", "add a new element");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) throws CommandExecutionException, ValidationException {
        OrganizationBuilder builder = new OrganizationBuilder(ctx.getConsole(), ctx.getInputManager());
        Organization.Draft draft = builder.readOrganizationDraft();

        Organization organization = ctx.getCollectionManager().addNew(
                draft,
                ctx.getIdGenerator(),
                ZonedDateTime.now()
        );

        ctx.getConsole().println("Added organization id=" + organization.getId());
    }
}