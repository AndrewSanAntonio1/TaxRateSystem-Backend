package com.project.taxratesystem.auth.service;

import com.project.taxratesystem.common.exception.ValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The password rules of API.md §13.2, applied to every place a password is set or changed:
 * {@code POST /auth/register}, {@code PUT /users/me/password} and
 * {@code POST /auth/password-reset/confirm}.
 *
 * <p>Failures are returned as a {@code 422} with a single {@code password} entry in the
 * {@code fields} map so the client can render the first rule that failed.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    /**
     * @throws ValidationException with a {@code fields} map when the password violates any rule.
     */
    public static void validate(String password) {
        if (password == null) {
            throw new ValidationException("password", "Password is required.");
        }
        if (password.length() < MIN_LENGTH) {
            throw new ValidationException("password",
                    "Password must be at least " + MIN_LENGTH + " characters.");
        }
        if (password.length() > MAX_LENGTH) {
            throw new ValidationException("password",
                    "Password must be at most " + MAX_LENGTH + " characters.");
        }
        if (password.isBlank()) {
            throw new ValidationException("password", "Password must not be all whitespace.");
        }
        boolean hasUpper = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasUpper) {
            throw new ValidationException("password", "Password must contain at least one uppercase letter.");
        }
        if (!hasDigit) {
            throw new ValidationException("password", "Password must contain at least one digit.");
        }
    }

    /**
     * @throws ValidationException when {@code confirmPassword} does not match {@code newPassword}.
     */
    public static void validateMatch(String newPassword, String confirmPassword) {
        if (newPassword == null || confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new ValidationException("confirmPassword", "Passwords do not match.");
        }
    }

    /** @return the first rule that failed, or {@code null} when the password is valid. */
    public static String firstViolation(String password) {
        try {
            validate(password);
            return null;
        } catch (ValidationException ex) {
            Map<String, String> fields = ex.getFields();
            return fields != null && fields.containsKey("password") ? fields.get("password") : ex.getMessage();
        }
    }
}