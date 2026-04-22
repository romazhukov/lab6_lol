package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.managers.CollectionManager;
import org.lab5.models.Address;
import org.lab5.models.Organization;

import java.util.ArrayList;
import java.util.List;

public class PrintFieldDescendingPostalAddressCommand extends AbstractCommand {

    public PrintFieldDescendingPostalAddressCommand() {
        super("print_field_descending_postal_address", "print postalAddress field descending");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) {
        List<Address> addresses = new ArrayList<>();

        for (Organization organization : ctx.getCollectionManager().snapshotAll()) {
            addresses.add(organization.getPostalAddress());
        }

        addresses.sort(CollectionManager.postalAddressDescendingNullsLast());

        for (Address address : addresses) {
            ctx.getConsole().println(String.valueOf(address));
        }
    }
}