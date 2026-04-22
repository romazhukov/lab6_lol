package client;

import org.lab5.exceptions.CommandExecutionException;
import org.lab5.io.InputManager;
import org.lab5.io.OrganizationBuilder;
import org.lab5.io.StandardConsole;
import org.lab5.managers.ScriptManager;
import org.lab5.models.Address;
import org.lab5.models.Organization;
import org.lab5.models.OrganizationType;
import shared.CommandRequest;
import shared.CommandResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientMain {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = parsePort(args);

        StandardConsole console = new StandardConsole();
        ScriptManager scriptManager = new ScriptManager();
        InputManager inputManager = new InputManager(console, scriptManager);
        OrganizationBuilder organizationBuilder = new OrganizationBuilder(console, inputManager);

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            console.println("Connected to server " + host + ":" + port);
            runInteractiveLoop(console, inputManager, organizationBuilder, out, in);
        } catch (IOException e) {
            console.printError("Server is unavailable: " + e.getMessage());
        }
    }

    private static void runInteractiveLoop(
            StandardConsole console,
            InputManager inputManager,
            OrganizationBuilder organizationBuilder,
            ObjectOutputStream out,
            ObjectInputStream in
    ) {
        while (true) {
            console.print("$ ");
            String line = inputManager.readLine();
            if (line == null) {
                return;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if ("exit".equals(line)) {
                return;
            }

            CommandRequest request;
            try {
                ParsedCommand parsed = parseCommand(line);
                request = buildRequest(parsed, organizationBuilder, out, in);
            } catch (CommandExecutionException e) {
                console.printError(e.getMessage());
                continue;
            }

            try {
                CommandResponse response = sendRequest(out, in, request);
                printResponse(console, response);
            } catch (EOFException e) {
                console.printError("Connection closed by server");
                return;
            } catch (IOException | ClassNotFoundException e) {
                console.printError("Failed to execute command: " + e.getMessage());
                return;
            }
        }
    }

    private static ParsedCommand parseCommand(String line) {
        String name;
        String[] args;
        if (line.startsWith("execute_script")) {
            name = "execute_script";
            String rest = line.substring("execute_script".length()).trim();
            args = rest.isEmpty() ? new String[0] : new String[]{rest};
        } else {
            String[] parts = line.split("\\s+");
            name = parts[0];
            args = new String[Math.max(0, parts.length - 1)];
            for (int i = 1; i < parts.length; i++) {
                args[i - 1] = parts[i];
            }
        }
        return new ParsedCommand(name, args);
    }

    private static CommandRequest buildRequest(
            ParsedCommand parsed,
            OrganizationBuilder builder,
            ObjectOutputStream out,
            ObjectInputStream in
    ) throws CommandExecutionException, IOException, ClassNotFoundException {
        String name = parsed.name;
        String[] args = parsed.args;
        Organization.Draft draft = null;
        Integer targetId = null;
        Address address = null;
        OrganizationType organizationType = null;

        if ("add".equals(name) || "add_if_min".equals(name) || "remove_lower".equals(name)) {
            draft = builder.readOrganizationDraft();
        } else if ("update".equals(name)) {
            if (args.length != 1) {
                throw new CommandExecutionException("usage: update <id>");
            }
            try {
                targetId = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new CommandExecutionException("id must be an integer");
            }
            CommandResponse getResponse = sendRequest(
                    out,
                    in,
                    new CommandRequest("__internal_get_by_id", new String[0], null, targetId, null, null)
            );
            if (!getResponse.isSuccess()) {
                throw new CommandExecutionException(getResponse.getMessage());
            }
            if (!(getResponse.getData() instanceof Organization existing)) {
                throw new CommandExecutionException("Server returned invalid data for update");
            }
            draft = builder.readOrganizationDraftForUpdate(existing);
        } else if ("remove_all_by_postal_address".equals(name)) {
            address = builder.readAddressFilter();
        } else if ("count_greater_than_type".equals(name)) {
            if (args.length != 1) {
                throw new CommandExecutionException("usage: count_greater_than_type <type>");
            }
            try {
                organizationType = OrganizationType.valueOf(args[0].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CommandExecutionException("unknown OrganizationType: " + args[0]);
            }
        }

        return new CommandRequest(name, args, draft, targetId, address, organizationType);
    }

    private static CommandResponse sendRequest(
            ObjectOutputStream out,
            ObjectInputStream in,
            CommandRequest request
    ) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        Object rawResponse = in.readObject();
        if (!(rawResponse instanceof CommandResponse response)) {
            return CommandResponse.error("Unexpected response from server");
        }
        return response;
    }

    private static void printResponse(StandardConsole console, CommandResponse response) {
        if (response.isSuccess()) {
            console.println(response.getMessage());
        } else {
            console.printError(response.getMessage());
        }
    }

    private static int parsePort(String[] args) {
        if (args.length < 2) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    private record ParsedCommand(String name, String[] args) {
    }
}
