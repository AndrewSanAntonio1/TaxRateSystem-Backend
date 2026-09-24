package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Base class for every exception that maps onto the standard error envelope of API.md §5.
 *
 * <p>{@code fields} is {@code null} unless the failure is field-level, in which case it is
 * serialised as the {@code fields} map of the validation envelope.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> fields;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus status, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.fields = fields;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public boolean isFieldLevel() {
        return fields != null && !fields.isEmpty();
    }
}
