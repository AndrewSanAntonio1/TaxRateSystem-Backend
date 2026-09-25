package com.project.taxratesystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/login} (API.md §6.4).
 */
public class LoginRequest {

    @Email(message = "Invalid e-mail address.")
    @NotBlank(message = "E-mail is required.")
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
