package com.project.taxratesystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/password-reset/request} (API.md §6.7).
 *
 * <p>The response is identical whether or not the address is registered, so nothing here may be
 * used to reveal whether an account exists.
 */

public class PasswordResetRequest {

    @Email(message = "Invalid e-mail address.")
    @NotBlank(message = "E-mail is required.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
