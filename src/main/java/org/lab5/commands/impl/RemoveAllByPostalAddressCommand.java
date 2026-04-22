package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.exceptions.CommandExecutionException;
import org.lab5.io.OrganizationBuilder;
import org.lab5.models.Address;

public class RemoveAllByPostalAddressCommand extends AbstractCommand {

    public RemoveAllByPostalAddressCommand() {
        super("remove_all_by_postal_address", "remove all elements with the same postal address");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) throws CommandExecutionException {
        OrganizationBuilder builder = new OrganizationBuilder(ctx.getConsole(), ctx.getInputManager());
        Address address = builder.readAddressFilter();

        int removed = ctx.getCollectionManager().removeAllByPostalAddress(address);
        ctx.getConsole().println("Removed elements: " + removed);
    }
}