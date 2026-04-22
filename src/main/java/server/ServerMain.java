package server;

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
import org.lab5.exceptions.FileWriteException;
import org.lab5.exceptions.ValidationException;
import org.lab5.io.Console;
import org.lab5.io.InputManager;
import org.lab5.managers.CollectionManager;
import org.lab5.managers.FileManager;
import org.lab5.managers.IdGenerator;
import org.lab5.managers.ScriptManager;
import org.lab5.models.OrganizationType;
import org.lab5.models.Organization;
import org.lab5.util.JsonMapperFactory;
import shared.CommandRequest;
import shared.CommandResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMain {
    public static final String ENV_COLLECTION_FILE = "ORG_COLLECTION_FILE";
    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) {
        int port = parsePort(args);
        String fileName = System.getenv(ENV_COLLECTION_FILE);
        if (fileName == null || fileName.isBlank()) {
            System.err.println("Environment variable " + ENV_COLLECTION_FILE + " is not set");
            return;
        }

        ObjectMapper mapper = JsonMapperFactory.create();
        FileManager fileManager = new FileManager(Path.of(fileName.trim()), mapper);
        CollectionManager collectionManager = new CollectionManager();
        IdGenerator idGenerator = new IdGenerator();
        ScriptManager scriptManager = new ScriptManager();
        BufferConsole bufferConsole = new BufferConsole();
        InputManager inputManager = new InputManager(bufferConsole, scriptManager);
        CommandContext context = new CommandContext(
                collectionManager,
                fileManager,
                inputManager,
                scriptManager,
                bufferConsole,
                idGenerator,
                new AtomicBoolean(true)
        );

        CommandManager commandManager = createCommandManager();
        commandManager.setContext(context);

        loadCollection(fileManager, collectionManager, idGenerator);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.err.println("Server started on localhost:" + port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    handleClient(socket, commandManager, context);
                } catch (Exception e) {
                    System.err.println("Client handling failed: " + safeMessage(e));
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + safeMessage(e));
        }
    }

    private static void handleClient(Socket socket, CommandManager commandManager, CommandContext context) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Object raw;
                try {
                    raw = in.readObject();
                } catch (EOFException eofException) {
                    break;
                } catch (ClassNotFoundException e) {
                    out.writeObject(CommandResponse.error("Unsupported request class: " + e.getMessage()));
                    out.flush();
                    continue;
                }

                if (!(raw instanceof CommandRequest request)) {
                    out.writeObject(CommandResponse.error("Unexpected request type"));
                    out.flush();
                    continue;
                }

                CommandResponse response;
                try {
                    response = process(request, commandManager, context);
                } catch (Exception e) {
                    response = CommandResponse.error("Server error: " + safeMessage(e));
                }
                out.writeObject(response);
                out.flush();
            }
        }
    }

    private static CommandResponse process(CommandRequest request, CommandManager commandManager, CommandContext context) {
        String name = request.getName();
        if (name == null || name.isBlank()) {
            return CommandResponse.error("Command name must not be empty");
        }

        return switch (name) {
            case "add" -> handleAdd(request, context);
            case "update" -> handleUpdate(request, context);
            case "add_if_min" -> handleAddIfMin(request, context);
            case "remove_lower" -> handleRemoveLower(request, context);
            case "remove_all_by_postal_address" -> handleRemoveAllByPostalAddress(request, context);
            case "remove_by_id" -> handleRemoveById(request, context);
            case "__internal_get_by_id" -> handleGetById(request, context);
            case "clear" -> handleClear(context);
            case "save" -> handleSave(context);
            case "remove_head" -> handleRemoveHead(context);
            case "count_greater_than_type" -> handleCountGreaterThanType(request, context);
            case "print_field_descending_postal_address", "show", "info", "help" ->
                    handleWithCommandManager(name, request.getArgs(), commandManager, context);
            case "execute_script" -> handleWithCommandManager(name, request.getArgs(), commandManager, context);
            default -> CommandResponse.error("Unknown command: " + name);
        };
    }

    private static CommandResponse handleWithCommandManager(
            String name,
            String[] args,
            CommandManager commandManager,
            CommandContext context
    ) {
        BufferConsole console = (BufferConsole) context.getConsole();
        console.reset();
        commandManager.handle(buildCommandLine(name, args));
        if (console.hasErrors()) {
            return CommandResponse.error(console.consumeOutput());
        }
        return CommandResponse.ok(console.consumeOutput());
    }

    private static String buildCommandLine(String name, String[] args) {
        if (args == null || args.length == 0) {
            return name;
        }
        return name + " " + String.join(" ", args);
    }

    private static CommandResponse handleAdd(CommandRequest request, CommandContext context) {
        if (request.getDraft() == null) {
            return CommandResponse.error("Command add requires draft payload");
        }
        try {
            Organization organization = context.getCollectionManager().addNew(
                    request.getDraft(),
                    context.getIdGenerator(),
                    ZonedDateTime.now()
            );
            return CommandResponse.ok("Added organization id=" + organization.getId(), organization.getId());
        } catch (Exception e) {
            return CommandResponse.error("Command add failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleUpdate(CommandRequest request, CommandContext context) {
        if (request.getTargetId() == null) {
            return CommandResponse.error("Command update requires target id");
        }
        if (request.getDraft() == null) {
            return CommandResponse.error("Command update requires draft payload");
        }
        try {
            Organization updated = context.getCollectionManager().update(request.getTargetId(), request.getDraft());
            return CommandResponse.ok("Updated organization id=" + updated.getId(), updated.getId());
        } catch (Exception e) {
            return CommandResponse.error("Command update failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleAddIfMin(CommandRequest request, CommandContext context) {
        if (request.getDraft() == null) {
            return CommandResponse.error("Command add_if_min requires draft payload");
        }
        try {
            Organization organization = context.getCollectionManager().addIfMin(
                    request.getDraft(),
                    context.getIdGenerator(),
                    ZonedDateTime.now()
            );
            if (organization == null) {
                return CommandResponse.ok("Element is not minimal, nothing added");
            }
            return CommandResponse.ok("Added minimal organization id=" + organization.getId(), organization.getId());
        } catch (Exception e) {
            return CommandResponse.error("Command add_if_min failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleRemoveLower(CommandRequest request, CommandContext context) {
        if (request.getDraft() == null) {
            return CommandResponse.error("Command remove_lower requires draft payload");
        }
        try {
            int removed = context.getCollectionManager().removeLower(request.getDraft());
            return CommandResponse.ok("Removed elements: " + removed, removed);
        } catch (Exception e) {
            return CommandResponse.error("Command remove_lower failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleRemoveAllByPostalAddress(CommandRequest request, CommandContext context) {
        try {
            int removed = context.getCollectionManager().removeAllByPostalAddress(request.getAddress());
            return CommandResponse.ok("Removed elements: " + removed, removed);
        } catch (Exception e) {
            return CommandResponse.error("Command remove_all_by_postal_address failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleRemoveById(CommandRequest request, CommandContext context) {
        if (request.getArgs().length != 1) {
            return CommandResponse.error("usage: remove_by_id <id>");
        }
        final int id;
        try {
            id = Integer.parseInt(request.getArgs()[0]);
        } catch (NumberFormatException e) {
            return CommandResponse.error("id must be an integer");
        }
        try {
            boolean removed = context.getCollectionManager().removeById(id);
            if (!removed) {
                return CommandResponse.error("organization with id=" + id + " not found");
            }
            return CommandResponse.ok("Removed organization id=" + id, id);
        } catch (Exception e) {
            return CommandResponse.error("Command remove_by_id failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleGetById(CommandRequest request, CommandContext context) {
        final int id;
        if (request.getTargetId() != null) {
            id = request.getTargetId();
        } else if (request.getArgs().length == 1) {
            try {
                id = Integer.parseInt(request.getArgs()[0]);
            } catch (NumberFormatException e) {
                return CommandResponse.error("id must be an integer");
            }
        } else {
            return CommandResponse.error("usage: get_by_id <id>");
        }
        Organization organization = context.getCollectionManager().findById(id);
        if (organization == null) {
            return CommandResponse.error("organization with id=" + id + " not found");
        }
        return CommandResponse.ok("Found organization id=" + id, organization);
    }

    private static CommandResponse handleClear(CommandContext context) {
        try {
            context.getCollectionManager().clear();
            return CommandResponse.ok("Collection cleared");
        } catch (Exception e) {
            return CommandResponse.error("Command clear failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleSave(CommandContext context) {
        try {
            context.getFileManager().saveCollection(context.getCollectionManager().snapshotAll());
            return CommandResponse.ok("Saved to " + context.getFileManager().getDataFile());
        } catch (FileWriteException e) {
            return CommandResponse.error("Command save failed: " + safeMessage(e));
        } catch (Exception e) {
            return CommandResponse.error("Command save failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleRemoveHead(CommandContext context) {
        try {
            Organization removed = context.getCollectionManager().removeHead();
            if (removed == null) {
                return CommandResponse.error("collection is empty");
            }
            return CommandResponse.ok("Removed head: " + removed, removed);
        } catch (Exception e) {
            return CommandResponse.error("Command remove_head failed: " + safeMessage(e));
        }
    }

    private static CommandResponse handleCountGreaterThanType(CommandRequest request, CommandContext context) {
        OrganizationType type = request.getOrganizationType();
        if (type == null) {
            if (request.getArgs().length != 1) {
                return CommandResponse.error("usage: count_greater_than_type <type>");
            }
            try {
                type = OrganizationType.valueOf(request.getArgs()[0].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return CommandResponse.error("unknown OrganizationType: " + request.getArgs()[0]);
            }
        }
        try {
            int count = context.getCollectionManager().countGreaterThanType(type);
            return CommandResponse.ok("Elements count: " + count, count);
        } catch (Exception e) {
            return CommandResponse.error("Command count_greater_than_type failed: " + safeMessage(e));
        }
    }

    private static void loadCollection(
            FileManager fileManager,
            CollectionManager collectionManager,
            IdGenerator idGenerator
    ) {
        try {
            List<Organization> organizations = fileManager.loadCollection();
            collectionManager.replaceAll(organizations);
            idGenerator.bootstrap(organizations);
        } catch (FileReadException | ValidationException e) {
            System.err.println("Could not load collection, starting empty: " + safeMessage(e));
        }
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

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    private static String safeMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getMessage();
    }

    private static final class BufferConsole implements Console {
        private final StringBuilder out = new StringBuilder();
        private boolean hasErrors;

        @Override
        public void print(String text) {
            out.append(text);
        }

        @Override
        public void println(String text) {
            out.append(text).append(System.lineSeparator());
        }

        @Override
        public void printError(String text) {
            hasErrors = true;
            out.append("ERROR: ").append(text).append(System.lineSeparator());
        }

        @Override
        public String readLineInteractive() {
            return null;
        }

        private void reset() {
            out.setLength(0);
            hasErrors = false;
        }

        private boolean hasErrors() {
            return hasErrors;
        }

        private String consumeOutput() {
            String text = out.toString().trim();
            if (text.isEmpty()) {
                return "OK";
            }
            return text;
        }
    }
}
