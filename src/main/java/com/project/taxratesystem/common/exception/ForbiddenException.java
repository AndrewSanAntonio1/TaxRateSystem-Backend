package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Authenticated but not allowed to act (API.md §4: {@code 403}) - an unverified, suspended or
 * deactivated account.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
