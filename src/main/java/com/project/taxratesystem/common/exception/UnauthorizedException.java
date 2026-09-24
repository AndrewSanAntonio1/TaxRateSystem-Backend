package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Missing, invalid or expired credentials (API.md §4: {@code 401}) - wrong e-mail or password,
 * an invalid bearer token, or a wrong current password on a password change.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
