package com.project.taxratesystem.auth.dto;

import com.project.taxratesystem.auth.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code POST /auth/resend-otp} (API.md §6.3).
 */
public class ResendOtpRequest {

    @Email(message = "Invalid e-mail address.")
    @NotBlank(message = "E-mail is required.")
    private String email;

    @NotNull(message = "Purpose is required.")
    private OtpPurpose purpose;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}
