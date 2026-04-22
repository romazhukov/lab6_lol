package org.lab5.util;

import org.lab5.exceptions.ValidationException;
import org.lab5.models.OrganizationType;

public final class ParserUtils {

    private ParserUtils() {
    }

    public static int parseIntStrict(String s) throws ValidationException {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            throw new ValidationException("expected int: " + s);
        }
    }

    public static long parseLongStrict(String s) throws ValidationException {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            throw new ValidationException("expected long: " + s);
        }
    }

    public static double parseDoubleStrict(String s) throws ValidationException {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            throw new ValidationException("expected double: " + s);
        }
    }

    public static float parseFloatStrict(String s) throws ValidationException {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            throw new ValidationException("expected float: " + s);
        }
    }

    public static OrganizationType parseOrganizationType(String s) throws ValidationException {
        if (s == null || s.isBlank()) {
            throw new ValidationException("type is empty");
        }
        try {
            return OrganizationType.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new ValidationException("unknown type: " + s);
        }
    }
}