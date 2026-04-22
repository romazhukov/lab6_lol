package org.lab5.util;

import org.lab5.exceptions.ValidationException;
import org.lab5.models.Address;
import org.lab5.models.Coordinates;

public final class Validators {

    private Validators() {
    }

    public static void requireNonBlankName(String value) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException("name must not be empty");
        }
    }

    public static void requirePositiveTurnover(double value) throws ValidationException {
        if (value <= 0) {
            throw new ValidationException("annualTurnover must be > 0");
        }
    }

    public static void requirePositiveEmployees(long value) throws ValidationException {
        if (value <= 0) {
            throw new ValidationException("employeesCount must be > 0");
        }
    }

    public static void validateCoordinates(Coordinates coordinates) throws ValidationException {
        if (coordinates == null || coordinates.getX() == null) {
            throw new ValidationException("coordinates.x must not be null");
        }
    }

    public static void validateStreet(String street) throws ValidationException {
        if (street != null && street.length() > 190) {
            throw new ValidationException("street length must be <= 190");
        }
    }

    public static void validateZip(String zip) throws ValidationException {
        if (zip != null && zip.length() > 30) {
            throw new ValidationException("zipCode length must be <= 30");
        }
    }

    public static Address buildAddress(String street, String zip) throws ValidationException {
        validateStreet(street);
        validateZip(zip);
        return new Address(street, zip);
    }
}