package com.project.taxratesystem.user.dto;

/**
 * Body of {@code PUT /users/me/password} on success (API.md §7.4).
 */
public class ChangePasswordResponse {

    private String message;

    public ChangePasswordResponse() {
    }

    public ChangePasswordResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
