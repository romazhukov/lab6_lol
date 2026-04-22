package org.lab5.io;

import org.lab5.exceptions.CommandExecutionException;
import org.lab5.exceptions.ValidationException;
import org.lab5.models.Address;
import org.lab5.models.Coordinates;
import org.lab5.models.Organization;
import org.lab5.models.OrganizationType;
import org.lab5.util.Validators;

public class OrganizationBuilder {
    private final Console console;
    private final InputManager inputManager;

    public OrganizationBuilder(Console console, InputManager inputManager) {
        this.console = console;
        this.inputManager = inputManager;
    }

    public Organization.Draft readOrganizationDraft() throws CommandExecutionException {
        console.println("--- Organization fields ---");

        String name = inputManager.readValidatedLine(
                "name (non-empty): ",
                raw -> (raw == null || raw.trim().isBlank()) ? "name must not be empty" : null
        ).trim();

        console.println("--- Coordinates ---");
        int x = inputManager.readInt("x (int, required): ");
        float y = inputManager.readFloat("y (float): ");

        double annualTurnover = inputManager.readDouble("annualTurnover (> 0): ");
        long employeesCount = inputManager.readLong("employeesCount (> 0): ");
        OrganizationType type = inputManager.readOrganizationTypeNullable("type");
        Address postalAddress = readAddressNullable();

        Organization.Draft draft = new Organization.Draft(
                name,
                new Coordinates(x, y),
                annualTurnover,
                employeesCount,
                type,
                postalAddress
        );

        try {
            draft.validate();
        } catch (ValidationException e) {
            throw new CommandExecutionException(e.getMessage());
        }

        return draft;
    }

    public Organization.Draft readOrganizationDraftForUpdate(Organization existing) throws CommandExecutionException {
        console.println("--- Update organization fields ---");

        console.print("name [" + existing.getName() + "] (empty = keep): ");
        String nameInput = readLineOrThrow();
        String name = isEmpty(nameInput) ? existing.getName() : nameInput.trim();

        console.println("--- Coordinates ---");

        console.print("x [" + existing.getCoordinates().getX() + "] (empty = keep): ");
        String xInput = readLineOrThrow();
        int x = isEmpty(xInput) ? existing.getCoordinates().getX() : parseInt(xInput, "x must be an integer");

        console.print("y [" + existing.getCoordinates().getY() + "] (empty = keep): ");
        String yInput = readLineOrThrow();
        float y = isEmpty(yInput) ? existing.getCoordinates().getY() : parseFloat(yInput, "y must be a float");

        console.print("annualTurnover [" + existing.getAnnualTurnover() + "] (empty = keep): ");
        String turnoverInput = readLineOrThrow();
        double annualTurnover = isEmpty(turnoverInput)
                ? existing.getAnnualTurnover()
                : parseDouble(turnoverInput, "annualTurnover must be a number");

        console.print("employeesCount [" + existing.getEmployeesCount() + "] (empty = keep): ");
        String employeesInput = readLineOrThrow();
        long employeesCount = isEmpty(employeesInput)
                ? existing.getEmployeesCount()
                : parseLong(employeesInput, "employeesCount must be an integer");

        OrganizationType type = readTypeForUpdate(existing.getType());
        Address postalAddress = readAddressForUpdate(existing.getPostalAddress());

        Organization.Draft draft = new Organization.Draft(
                name,
                new Coordinates(x, y),
                annualTurnover,
                employeesCount,
                type,
                postalAddress
        );

        try {
            draft.validate();
        } catch (ValidationException e) {
            throw new CommandExecutionException(e.getMessage());
        }

        return draft;
    }

    public Address readAddressFilter() throws CommandExecutionException {
        return readAddressNullable();
    }

    private Address readAddressNullable() throws CommandExecutionException {
        console.println("--- Postal address (optional) ---");
        console.println("Leave street empty to skip postal address entirely.");

        String street = inputManager.readValidatedLine(
                "street (<=190 chars, empty = skip whole address): ",
                raw -> {
                    if (raw == null) {
                        return "unexpected input";
                    }
                    String s = raw.trim();
                    if (s.isEmpty()) {
                        return null;
                    }
                    if (s.length() > 190) {
                        return "street length must be <= 190";
                    }
                    return null;
                }
        ).trim();

        if (street.isEmpty()) {
            return null;
        }

        String zip = inputManager.readValidatedLine(
                "zipCode (<=30 chars, empty allowed as null): ",
                raw -> {
                    if (raw == null) {
                        return "unexpected input";
                    }
                    String s = raw.trim();
                    if (s.length() > 30) {
                        return "zipCode length must be <= 30";
                    }
                    return null;
                }
        ).trim();

        if (zip.isEmpty()) {
            zip = null;
        }

        try {
            return Validators.buildAddress(street, zip);
        } catch (ValidationException e) {
            throw new CommandExecutionException(e.getMessage());
        }
    }

    private Address readAddressForUpdate(Address oldAddress) throws CommandExecutionException {
        console.println("--- Postal address ---");
        console.println("Empty input keeps the old value. Enter '-' to remove the whole address.");

        String oldStreet = oldAddress == null ? "null" : String.valueOf(oldAddress.getStreet());
        String oldZip = oldAddress == null ? "null" : String.valueOf(oldAddress.getZipCode());

        console.print("street [" + oldStreet + "] (empty = keep, '-' = remove): ");
        String streetInput = readLineOrThrow();

        if (isEmpty(streetInput)) {
            return oldAddress;
        }

        if (streetInput.trim().equals("-")) {
            return null;
        }

        String street = streetInput.trim();
        if (street.length() > 190) {
            throw new CommandExecutionException("street length must be <= 190");
        }

        console.print("zipCode [" + oldZip + "] (empty = keep): ");
        String zipInput = readLineOrThrow();

        String zip;
        if (isEmpty(zipInput)) {
            zip = oldAddress == null ? null : oldAddress.getZipCode();
        } else {
            zip = zipInput.trim();
        }

        if (zip != null && zip.length() > 30) {
            throw new CommandExecutionException("zipCode length must be <= 30");
        }

        try {
            return Validators.buildAddress(street, zip);
        } catch (ValidationException e) {
            throw new CommandExecutionException(e.getMessage());
        }
    }

    private OrganizationType readTypeForUpdate(OrganizationType oldType) throws CommandExecutionException {
        String current = oldType == null ? "null" : oldType.name();

        while (true) {
            console.print("type [" + current + "] (PUBLIC, TRUST, PRIVATE_LIMITED_COMPANY, empty = keep, '-' = null): ");
            String raw = readLineOrThrow();

            if (isEmpty(raw)) {
                return oldType;
            }

            if (raw.trim().equals("-")) {
                return null;
            }

            try {
                return OrganizationType.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                console.printError("unknown OrganizationType: " + raw);
                if (inputManager.isScriptMode()) {
                    throw new CommandExecutionException("script input error: unknown OrganizationType: " + raw);
                }
            }
        }
    }

    private String readLineOrThrow() throws CommandExecutionException {
        String line = inputManager.readLine();
        if (line == null) {
            throw new CommandExecutionException("unexpected end of input");
        }
        return line;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private int parseInt(String s, String error) throws CommandExecutionException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new CommandExecutionException(error);
        }
    }

    private float parseFloat(String s, String error) throws CommandExecutionException {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            throw new CommandExecutionException(error);
        }
    }

    private double parseDouble(String s, String error) throws CommandExecutionException {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            throw new CommandExecutionException(error);
        }
    }

    private long parseLong(String s, String error) throws CommandExecutionException {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new CommandExecutionException(error);
        }
    }
}