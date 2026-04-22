package org.lab5.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lab5.commands.CommandContext;
import org.lab5.commands.CommandManager;
import org.lab5.commands.impl.AddCommand;
import org.lab5.commands.impl.AddIfMinCommand;
import org.lab5.commands.impl.ClearCommand;
import org.lab5.commands.impl.CountGreaterThanTypeCommand;
import org.lab5.commands.impl.ExecuteScriptCommand;
import org.lab5.commands.impl.ExitCommand;
import org.lab5.commands.impl.HelpCommand;
import org.lab5.commands.impl.InfoCommand;
import org.lab5.commands.impl.PrintFieldDescendingPostalAddressCommand;
import org.lab5.commands.impl.RemoveAllByPostalAddressCommand;
import org.lab5.commands.impl.RemoveByIdCommand;
import org.lab5.commands.impl.RemoveHeadCommand;
import org.lab5.commands.impl.RemoveLowerCommand;
import org.lab5.commands.impl.SaveCommand;
import org.lab5.commands.impl.ShowCommand;
import org.lab5.commands.impl.UpdateCommand;
import org.lab5.exceptions.FileReadException;
import org.lab5.exceptions.ValidationException;
import org.lab5.io.Console;
import org.lab5.io.InputManager;
import org.lab5.io.StandardConsole;
import org.lab5.managers.CollectionManager;
import org.lab5.managers.FileManager;
import org.lab5.managers.IdGenerator;
import org.lab5.managers.ScriptManager;
import org.lab5.models.Organization;
import org.lab5.util.JsonMapperFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static final String ENV_COLLECTION_FILE = "ORG_COLLECTION_FILE";

    public static void main(String[] args) {
        Console console = new StandardConsole();

        String fileName = System.getenv(ENV_COLLECTION_FILE);
        if (fileName == null || fileName.isBlank()) {
            console.printError("Environment variable " + ENV_COLLECTION_FILE + " is not set");
            return;
        }

        Path path = Path.of(fileName.trim());
        ObjectMapper mapper = JsonMapperFactory.create();

        FileManager fileManager = new FileManager(path, mapper);
        CollectionManager collectionManager = new CollectionManager();
        IdGenerator idGenerator = new IdGenerator();
        ScriptManager scriptManager = new ScriptManager();
        InputManager inputManager = new InputManager(console, scriptManager);

        CommandContext context = new CommandContext(
                collectionManager,
                fileManager,
                inputManager,
                scriptManager,
                console,
                idGenerator,
                new AtomicBoolean(true)
        );

        CommandManager commandManager = createCommandManager();
        commandManager.setContext(context);

        try {
            List<Organization> organizations = fileManager.loadCollection();
            collectionManager.replaceAll(organizations);
            idGenerator.bootstrap(organizations);
        } catch (FileReadException e) {
            console.printError("Could not load collection: " + e.getMessage());
            console.printError("Starting with empty collection.");
        } catch (ValidationException e) {
            console.printError("Invalid data in file: " + e.getMessage());
            console.printError("Starting with empty collection.");
        }

        new ConsoleApplication(context, commandManager).run();
    }

    private static CommandManager createCommandManager() {
        CommandManager manager = new CommandManager();

        manager.register(new HelpCommand());
        manager.register(new InfoCommand());
        manager.register(new ShowCommand());
        manager.register(new AddCommand());
        manager.register(new UpdateCommand());
        manager.register(new RemoveByIdCommand());
        manager.register(new ClearCommand());
        manager.register(new SaveCommand());
        manager.register(new ExecuteScriptCommand());
        manager.register(new ExitCommand());
        manager.register(new RemoveHeadCommand());
        manager.register(new AddIfMinCommand());
        manager.register(new RemoveLowerCommand());
        manager.register(new RemoveAllByPostalAddressCommand());
        manager.register(new CountGreaterThanTypeCommand());
        manager.register(new PrintFieldDescendingPostalAddressCommand());

        return manager;
    }
}