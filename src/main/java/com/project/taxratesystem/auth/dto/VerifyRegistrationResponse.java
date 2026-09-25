package com.project.taxratesystem.auth.dto;

/**
 * Body of {@code POST /auth/verify-registration} (API.md §6.2).
 */
public class VerifyRegistrationResponse {

    private String message;
    private String email;
    private String status;

    public VerifyRegistrationResponse() {
    }

    public VerifyRegistrationResponse(String message, String email, String status) {
        this.message = message;
        this.email = email;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}