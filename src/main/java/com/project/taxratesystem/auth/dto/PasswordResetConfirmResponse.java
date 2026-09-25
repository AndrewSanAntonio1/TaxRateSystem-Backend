package com.project.taxratesystem.auth.dto;

/**
 * Body of {@code POST /auth/password-reset/confirm} (API.md §6.9).
 */
public class PasswordResetConfirmResponse {

    private String message;

    public PasswordResetConfirmResponse() {
    }

    public PasswordResetConfirmResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}