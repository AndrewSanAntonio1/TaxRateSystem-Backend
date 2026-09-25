package com.project.taxratesystem.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/password-reset/confirm} (API.md §6.9).
 */
public class PasswordResetConfirmRequest {

    @NotBlank(message = "Reset token is required.")
    private String resetToken;

    @NotBlank(message = "New password is required.")
    private String newPassword;

    @NotBlank(message = "Confirm password is required.")
    private String confirmPassword;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
