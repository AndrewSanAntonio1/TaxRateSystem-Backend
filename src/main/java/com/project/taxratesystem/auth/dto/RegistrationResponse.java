package com.project.taxratesystem.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of {@code POST /auth/register} (API.md §6.1).
 *
 * <p>The account is created in {@code PENDING_VERIFICATION} state; the 6-digit code that activates
 * it is e-mailed by {@code OtpEmailService} and is not part of this response.
 */
public class RegistrationResponse {

    private String message;
    private Integer userId;
    private String email;
    private boolean emailVerificationRequired = true;

    public RegistrationResponse() {
    }

    public RegistrationResponse(String message, Integer userId, String email) {
        this.message = message;
        this.userId = userId;
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerificationRequired() {
        return emailVerificationRequired;
    }

    public void setEmailVerificationRequired(boolean emailVerificationRequired) {
        this.emailVerificationRequired = emailVerificationRequired;
    }
}