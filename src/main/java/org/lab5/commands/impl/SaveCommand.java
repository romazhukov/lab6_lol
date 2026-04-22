package org.lab5.commands.impl;

import org.lab5.commands.AbstractCommand;
import org.lab5.commands.CommandContext;
import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.FileWriteException;

public class SaveCommand extends AbstractCommand {

    public SaveCommand() {
        super("save", "save collection to file");
    }

    @Override
    public void execute(CommandContext ctx, String[] args) throws CommandExecutionException {
        try {
            ctx.getFileManager().saveCollection(ctx.getCollectionManager().snapshotAll());
            ctx.getConsole().println("Saved to " + ctx.getFileManager().getDataFile());
        } catch (FileWriteException e) {
            throw new CommandExecutionException(e.getMessage());
        }
    }
}