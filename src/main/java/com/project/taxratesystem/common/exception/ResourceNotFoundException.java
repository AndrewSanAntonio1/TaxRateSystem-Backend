package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a path variable or lookup key does not resolve (API.md §4: {@code 404}).
 *
 * <p>Unknown path variables ({@code {code}}, {@code {id}}) are 404s, never validation errors.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}

