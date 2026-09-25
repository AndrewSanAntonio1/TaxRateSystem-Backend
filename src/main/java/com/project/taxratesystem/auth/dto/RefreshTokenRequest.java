package com.project.taxratesystem.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/refresh} (API.md §6.5).
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required.")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
