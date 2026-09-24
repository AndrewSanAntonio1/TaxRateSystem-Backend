package com.project.taxratesystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * The OTP resend cooldown is still active (API.md §6.3: {@code 429} with a {@code Retry-After}
 * header counting the remaining seconds).
 */
public class TooManyRequestsException extends ApiException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = Math.max(retryAfterSeconds, 1);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
