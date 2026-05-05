package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lab5.commands.CommandContext;
import org.lab5.commands.CommandManager;
import org.lab5.commands.impl.*;
import org.lab5.exceptions.FileReadException;
import org.lab5.exceptions.FileWriteException;
import org.lab5.exceptions.ValidationException;
import org.lab5.io.Console;
import org.lab5.io.InputManager;
import org.lab5.managers.CollectionManager;
import org.lab5.managers.FileManager;
import org.lab5.managers.IdGenerator;
import org.lab5.managers.ScriptManager;
import org.lab5.models.Organization;
import org.lab5.models.OrganizationType;
import org.lab5.util.JsonMapperFactory;
import shared.CommandRequest;
import shared.CommandResponse;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMain {
    private static final String ENV_COLLECTION_FILE = "ORG_COLLECTION_FILE";
    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) {
        int port = args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
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
        BufferConsole console = new BufferConsole();
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

        loadCollection(fileManager, collectionManager, idGenerator);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                fileManager.saveCollection(collectionManager.snapshotAll());
                System.err.println("Collection saved on shutdown");
            } catch (FileWriteException e) {
                System.err.println("Could not save collection on shutdown: " + message(e));
            }
        }));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.err.println("Server started on localhost:" + port);

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    handleClient(socket, commandManager, context);
                } catch (Exception e) {
                    System.err.println("Client error: " + message(e));
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + message(e));
        }
    }

    private static void handleClient(Socket socket, CommandManager commandManager, CommandContext context) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                try {
                    Object object = in.readObject();

                    if (object instanceof CommandRequest request) {
                        out.writeObject(process(request, commandManager, context));
                    } else {
                        out.writeObject(CommandResponse.error("Unexpected request type"));
                    }

                    out.flush();
                } catch (EOFException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    out.writeObject(CommandResponse.error("Unknown request class"));
                    out.flush();
                }
            }
        }
    }

    private static CommandResponse process(CommandRequest request, CommandManager commandManager, CommandContext context) {
        try {
            String name = request.getName();

            if (name == null || name.isBlank()) {
                return CommandResponse.error("Command name must not be empty");
            }

            switch (name) {
                case "add":
                    return add(request, context);
                case "update":
                    return update(request, context);
                case "add_if_min":
                    return addIfMin(request, context);
                case "remove_lower":
                    return removeLower(request, context);
                case "remove_all_by_postal_address":
                    return removeAllByPostalAddress(request, context);
                case "remove_by_id":
                    return removeById(request, context);
                case "__internal_get_by_id":
                    return getById(request, context);
                case "clear":
                    context.getCollectionManager().clear();
                    return CommandResponse.ok("Collection cleared");
                case "save":
                    return CommandResponse.error("save is server-only command");
                case "remove_head":
                    return removeHead(context);
                case "count_greater_than_type":
                    return countGreaterThanType(request, context);
                case "show":
                case "info":
                case "help":
                case "execute_script":
                case "print_field_descending_postal_address":
                    return runOldCommand(name, request.getArgs(), commandManager, context);
                default:
                    return CommandResponse.error("Unknown command: " + name);
            }
        } catch (Exception e) {
            return CommandResponse.error("Server error: " + message(e));
        }
    }

    private static CommandResponse add(CommandRequest request, CommandContext context) {
        if (request.getDraft() == null) {
            return CommandResponse.error("Command add requires draft");
        }

        try {
            Organization organization = context.getCollectionManager().addNew(
                    request.getDraft(),
                    context.getIdGenerator(),
                    ZonedDateTime.now()
            );

            return CommandResponse.ok("Added organization id=" + organization.getId(), organization.getId());
        } catch (Exception e) {
            return CommandResponse.error("Command add failed: " + message(e));
        }
    }

    private static CommandResponse update(CommandRequest request, CommandContext context) {
        if (request.getTargetId() == null) {
            return CommandResponse.error("Command update requires id");
        }

        if (request.getDraft() == null) {
            return CommandResponse.error("Command update requires draft");
        }

        try {
            Organization organization = context.getCollectionManager().update(
                    request.getTargetId(),
                    request.getDraft()
            );

            return CommandResponse.ok("Updated organization id=" + organization.getId(), organization.getId());
        } catch (Exception e) {
            return CommandResponse.error("Command update failed: " + message(e));
        }
    }

    private static CommandResponse addIfMin(CommandRequest request, CommandContext context) {
        if (request.getDraft() == null) {
            return CommandResponse.error("Command add_if_min requires draft");
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

            return CommandResponse.ok("Added organization id=" + organization.getId(), organization.getId());
        } catch (Exception e) {
            return CommandResponse.error("Command add_if_min failed: " + message(e));
        }
    }

    private static CommandResponse removeLower(CommandRequest request, CommandContext context) {
        if (request.getDraft() == null) {
            return CommandResponse.error("Command remove_lower requires draft");
        }

        try {
            int removed = context.getCollectionManager().removeLower(request.getDraft());
            return CommandResponse.ok("Removed elements: " + removed, removed);
        } catch (Exception e) {
            return CommandResponse.error("Command remove_lower failed: " + message(e));
        }
    }

    private static CommandResponse removeAllByPostalAddress(CommandRequest request, CommandContext context) {
        int removed = context.getCollectionManager().removeAllByPostalAddress(request.getAddress());
        return CommandResponse.ok("Removed elements: " + removed, removed);
    }

    private static CommandResponse removeById(CommandRequest request, CommandContext context) {
        if (request.getArgs().length != 1) {
            return CommandResponse.error("usage: remove_by_id <id>");
        }

        int id = parseId(request.getArgs()[0]);
        boolean removed = context.getCollectionManager().removeById(id);

        if (!removed) {
            return CommandResponse.error("organization with id=" + id + " not found");
        }

        return CommandResponse.ok("Removed organization id=" + id, id);
    }

    private static CommandResponse getById(CommandRequest request, CommandContext context) {
        if (request.getTargetId() == null) {
            return CommandResponse.error("internal get_by_id requires id");
        }

        int id = request.getTargetId();
        Organization organization = context.getCollectionManager().findById(id);

        if (organization == null) {
            return CommandResponse.error("organization with id=" + id + " not found");
        }

        return CommandResponse.ok("Found organization id=" + id, organization);
    }

    private static CommandResponse removeHead(CommandContext context) {
        Organization organization = context.getCollectionManager().removeHead();

        if (organization == null) {
            return CommandResponse.ok("collection is empty");
        }

        return CommandResponse.ok("Removed head: " + organization, organization);
    }

    private static CommandResponse countGreaterThanType(CommandRequest request, CommandContext context) {
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

        int count = context.getCollectionManager().countGreaterThanType(type);
        return CommandResponse.ok("count = " + count, count);
    }

    private static CommandResponse runOldCommand(
            String name,
            String[] args,
            CommandManager commandManager,
            CommandContext context
    ) {
        BufferConsole console = (BufferConsole) context.getConsole();
        console.reset();

        commandManager.handle(buildLine(name, args));

        if (console.hasErrors()) {
            return CommandResponse.error(console.read());
        }

        return CommandResponse.ok(console.read());
    }

    private static String buildLine(String name, String[] args) {
        if (args == null || args.length == 0) {
            return name;
        }

        return name + " " + String.join(" ", args);
    }

    private static int parseId(String value) {
        return Integer.parseInt(value.trim());
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
            System.err.println("Could not load collection, starting empty: " + message(e));
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

    private static String message(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getMessage();
    }

    private static class BufferConsole implements Console {
        private final StringBuilder text = new StringBuilder();
        private boolean hasErrors;

        @Override
        public void print(String value) {
            text.append(value);
        }

        @Override
        public void println(String value) {
            text.append(value).append(System.lineSeparator());
        }

        @Override
        public void printError(String value) {
            hasErrors = true;
            text.append(value).append(System.lineSeparator());
        }

        @Override
        public String readLineInteractive() {
            return null;
        }

        public void reset() {
            text.setLength(0);
            hasErrors = false;
        }

        public boolean hasErrors() {
            return hasErrors;
        }

        public String read() {
            String result = text.toString().trim();
            if (result.isEmpty()) {
                return "OK";
            }
            return result;
        }
    }
}