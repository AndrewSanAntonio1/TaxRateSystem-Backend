package com.project.taxratesystem.auth.dto;

/**
 * Body of {@code POST /auth/password-reset/verify} (API.md §6.8).
 *
 * <p>The {@code resetToken} is single-use and is <strong>not</strong> a session credential: it is
 * accepted only by {@code POST /auth/password-reset/confirm} and is worthless once used or expired.
 */
public class PasswordResetVerifyResponse {

    private String resetToken;
    private long expiresInSeconds;

    public PasswordResetVerifyResponse() {
    }

    public PasswordResetVerifyResponse(String resetToken, long expiresInSeconds) {
        this.resetToken = resetToken;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}