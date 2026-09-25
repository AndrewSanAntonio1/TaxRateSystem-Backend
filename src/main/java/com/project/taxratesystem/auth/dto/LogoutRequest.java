package com.project.taxratesystem.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/logout} (API.md §6.6).
 *
 * <p>{@code allDevices} is optional and defaults to {@code false}: the presented refresh token is
 * revoked, or every refresh token of the caller when it is {@code true}. The call is idempotent -
 * an unknown or already-revoked token still answers {@code 204}.
 */
public class LogoutRequest {

    @NotBlank(message = "Refresh token is required.")
    private String refreshToken;

    private Boolean allDevices;

    /** Absent is the same as {@code false} - only the current session is revoked. */
    public boolean isAllDevices() {
        return Boolean.TRUE.equals(allDevices);
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Boolean getAllDevices() {
        return allDevices;
    }

    public void setAllDevices(Boolean allDevices) {
        this.allDevices = allDevices;
    }
}
