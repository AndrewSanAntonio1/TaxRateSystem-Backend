package com.project.taxratesystem.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The single error envelope every non-2xx response uses (API.md §5).
 *
 * <p>{@code fields} is {@code null} unless the failure is field-level (bean validation on a
 * DTO, a {@link com.project.taxratesystem.common.exception.ValidationException}, or a body that
 * cannot be bound to its DTO), in which case it maps each offending field to a display message.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields
) {

    /** General error - no field map. */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    /** Field-level error - includes the {@code fields} map. */
    public static ErrorResponse ofFields(int status, String error, String message, String path,
                                         Map<String, String> fields) {
        return new ErrorResponse(Instant.now(), status, error, message, path, fields);
    }
}

