package org.lab5.commands;

import org.lab5.io.Console;
import org.lab5.io.InputManager;
import org.lab5.managers.CollectionManager;
import org.lab5.managers.FileManager;
import org.lab5.managers.IdGenerator;
import org.lab5.managers.ScriptManager;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommandContext {
    private final CollectionManager collectionManager;
    private final FileManager fileManager;
    private final InputManager inputManager;
    private final ScriptManager scriptManager;
    private final Console console;
    private final IdGenerator idGenerator;
    private final AtomicBoolean running;
    private CommandManager commandManager;

    public CommandContext(
            CollectionManager collectionManager,
            FileManager fileManager,
            InputManager inputManager,
            ScriptManager scriptManager,
            Console console,
            IdGenerator idGenerator,
            AtomicBoolean running
    ) {
        this.collectionManager = collectionManager;
        this.fileManager = fileManager;
        this.inputManager = inputManager;
        this.scriptManager = scriptManager;
        this.console = console;
        this.idGenerator = idGenerator;
        this.running = running;
    }

    public void attachCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public Console getConsole() {
        return console;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public AtomicBoolean getRunning() {
        return running;
    }
}