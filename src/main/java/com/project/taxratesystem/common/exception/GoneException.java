package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A token that is well-formed but no longer valid because its lifetime elapsed
 * (API.md §6.9: {@code 410} for an expired reset token).
 */
public class GoneException extends ApiException {

    public GoneException(String message) {
        super(HttpStatus.GONE, message);
    }
}
