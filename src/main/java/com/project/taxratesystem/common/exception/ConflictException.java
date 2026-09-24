package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A request that conflicts with existing state (API.md §4: {@code 409}) - duplicate e-mail,
 * an OTP or reset token that was already used, a phone number already taken.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
