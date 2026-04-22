package org.lab5.commands;

import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;

public interface Command {
    String getName();

    String getDescription();

    void execute(CommandContext ctx, String[] args)
            throws CommandExecutionException, ValidationException;
}