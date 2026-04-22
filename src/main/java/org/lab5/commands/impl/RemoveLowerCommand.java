package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;
import org.lab5.io.OrganizationBuilder;
import org.lab5.models.Organization;

public class RemoveLowerCommand extends AbstractCommand {

    public RemoveLowerCommand() {
        super("remove_lower", "remove all elements lower than given element");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) throws CommandExecutionException, ValidationException {
        OrganizationBuilder builder = new OrganizationBuilder(ctx.getConsole(), ctx.getInputManager());
        Organization.Draft draft = builder.readOrganizationDraft();

        int removed = ctx.getCollectionManager().removeLower(draft);
        ctx.getConsole().println("Removed elements: " + removed);
    }
}