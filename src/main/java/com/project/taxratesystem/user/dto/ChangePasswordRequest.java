package com.project.taxratesystem.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code PUT /users/me/password} (API.md §7.4).
 *
 * <p>A wrong {@code currentPassword} is a {@code 401} (deliberately distinct from validation); the
 * §13.2 policy on {@code newPassword} and the match check are raised as {@code 422}s by
 * {@code PasswordPolicy}.
 */
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required.")
    private String currentPassword;

    @NotBlank(message = "New password is required.")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required.")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
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

