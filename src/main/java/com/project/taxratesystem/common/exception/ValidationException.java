package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * A request body that failed field-level validation (API.md §5): the response is {@code 422}
 * with a {@code fields} map, or {@code 400} when the JSON itself could not be parsed.
 */
public class ValidationException extends ApiException {

    private static final String DEFAULT_MESSAGE = "Invalid request";

    public ValidationException(Map<String, String> fields) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, DEFAULT_MESSAGE, fields);
    }

    public ValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    public ValidationException(HttpStatus status, String message, Map<String, String> fields) {
        super(status, message, fields);
    }

    /** A body that could not be bound to its DTO at all ({@code 400}). */
    public static ValidationException malformedBody(String message) {
        return new ValidationException(HttpStatus.BAD_REQUEST, DEFAULT_MESSAGE, Map.of("body", message));
    }
}

