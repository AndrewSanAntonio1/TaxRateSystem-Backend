package com.project.taxratesystem.auth.dto;

/**
 * Common shape for the two "code sent" responses (API.md §6.3, §6.7).
 *
 * <p>Both endpoints return the same envelope so the client can reuse one model; the only
 * difference is the {@code message} text.
 */
public class OtpResponse {

    private String message;
    private long otpExpiresInSeconds;
    private long resendAvailableInSeconds;

    public OtpResponse() {
    }

    public OtpResponse(String message, long otpExpiresInSeconds, long resendAvailableInSeconds) {
        this.message = message;
        this.otpExpiresInSeconds = otpExpiresInSeconds;
        this.resendAvailableInSeconds = resendAvailableInSeconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getOtpExpiresInSeconds() {
        return otpExpiresInSeconds;
    }

    public void setOtpExpiresInSeconds(long otpExpiresInSeconds) {
        this.otpExpiresInSeconds = otpExpiresInSeconds;
    }

    public long getResendAvailableInSeconds() {
        return resendAvailableInSeconds;
    }

    public void setResendAvailableInSeconds(long resendAvailableInSeconds) {
        this.resendAvailableInSeconds = resendAvailableInSeconds;
    }
}