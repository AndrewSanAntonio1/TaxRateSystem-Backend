package com.project.taxratesystem.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Composes and sends the OTP e-mails of API.md §6.1 and §6.7.
 *
 * <p>The body never contains anything but the code and its lifetime - no account details, no
 * links with credentials.
 */
@Service
public class OtpEmailService {

    private final EmailService emailService;
    private final long otpExpirationMinutes;
    private final long resetExpirationMinutes;

    public OtpEmailService(EmailService emailService,
                           @Value("${app.otp.expiration-minutes:5}") long otpExpirationMinutes,
                           @Value("${app.password-reset.expiration-minutes:15}") long resetExpirationMinutes) {
        this.emailService = emailService;
        this.otpExpirationMinutes = otpExpirationMinutes;
        this.resetExpirationMinutes = resetExpirationMinutes;
    }

    /** The code that activates a brand-new account ({@code OtpPurpose.REGISTRATION}). */
    public void sendRegistrationCode(String email, String code) {
        String body = """
                Welcome to TaxRateSystem!

                Your verification code is: %s

                Enter it in the app to activate your account. The code expires in %d minutes.
                If you did not create an account, you can safely ignore this e-mail.
                """.formatted(code, otpExpirationMinutes);
        emailService.sendPlainText(email, "Your TaxRateSystem verification code", body);
    }

    /** The code that starts a password reset ({@code OtpPurpose.PASSWORD_RESET}). */
    public void sendPasswordResetCode(String email, String code) {
        String body = """
                You asked to reset your TaxRateSystem password.

                Your reset code is: %s

                Enter it in the app to choose a new password. The code expires in %d minutes.
                If you did not request a reset, you can safely ignore this e-mail - your password
                stays unchanged.
                """.formatted(code, resetExpirationMinutes);
        emailService.sendPlainText(email, "Your TaxRateSystem password reset code", body);
    }
}

