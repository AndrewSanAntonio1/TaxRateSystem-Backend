package com.project.taxratesystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Body of {@code POST /auth/verify-registration} (API.md §6.2).
 */
public class VerifyRegistrationRequest {

    @Email(message = "Invalid e-mail address.")
    @NotBlank(message = "E-mail is required.")
    private String email;

    @NotBlank(message = "Code is required.")
    @Pattern(regexp = "^\\d{6}$", message = "Code must be exactly 6 digits.")
    private String code;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
