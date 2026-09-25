package com.project.taxratesystem.auth.service;

import com.project.taxratesystem.auth.dto.PasswordResetVerifyResponse;
import com.project.taxratesystem.auth.entity.PasswordResetToken;
import com.project.taxratesystem.auth.enums.OtpPurpose;
import com.project.taxratesystem.auth.repository.PasswordResetTokenRepository;
import com.project.taxratesystem.auth.repository.RefreshTokenRepository;
import com.project.taxratesystem.common.exception.ConflictException;
import com.project.taxratesystem.common.exception.GoneException;
import com.project.taxratesystem.common.exception.ResourceNotFoundException;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.notification.OtpEmailService;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.UserStatus;
import com.project.taxratesystem.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The password-recovery flow of API.md §6.7–§6.9.
 *
 * <p>The flow is two-step on purpose: the e-mailed code proves the caller owns the address, and
 * the short-lived reset token they get back proves they completed that step. Neither is a session
 * credential. On success every refresh token of the account is revoked so stolen sessions die.
 */
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration resetExpiration;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository resetTokenRepository,
                                OtpService otpService,
                                PasswordEncoder passwordEncoder,
                                RefreshTokenRepository refreshTokenRepository,
                                @Value("${app.password-reset.expiration-minutes:15}") long resetExpirationMinutes) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetExpiration = Duration.ofMinutes(resetExpirationMinutes);
    }

    /**
     * Start recovery (§6.7). The response is identical whether or not the address is registered;
     * a suspended or deactivated account is silently ignored, and a pending account may still
     * reset its password.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (user.getStatus() == UserStatus.SUSPENDED
                    || user.getStatus() == UserStatus.DEACTIVATED) {
                return; // silently ignored
            }
            otpService.issueCode(user, OtpPurpose.PASSWORD_RESET);
        });
    }

    /**
     * Exchange a verified code for a short-lived reset token (§6.8).
     *
     * @throws ValidationException {@code 422} wrong or expired code
     * @throws ConflictException   {@code 409} code already used
     */
    @Transactional
    public PasswordResetVerifyResponse verifyCode(String email, String code) {
        otpService.verifyPasswordResetCode(email, code);
        String normalized = email == null ? null : email.trim().toLowerCase();
        String token = generateToken();
        Instant now = Instant.now();
        User user = userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("No account for that e-mail address."));
        PasswordResetToken reset = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(token))
                .createdAt(now)
                .expiresAt(now.plus(resetExpiration))
                .build();
        resetTokenRepository.save(reset);
        return new PasswordResetVerifyResponse(token, resetExpirationSeconds());
    }

    /**
     * Set the new password (§6.9).
     *
     * @throws ValidationException {@code 422} mismatch or weak password
     * @throws ConflictException   {@code 409} token already used
     * @throws GoneException       {@code 410} token expired
     */
    @Transactional
    public void confirmReset(String resetToken, String newPassword, String confirmPassword) {
        PasswordPolicy.validate(newPassword);
        PasswordPolicy.validateMatch(newPassword, confirmPassword);

        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash(resetToken))
                .orElseThrow(() -> new GoneException("That reset token is invalid or has expired."));

        Instant now = Instant.now();
        if (token.isExpired(now)) {
            throw new GoneException("That reset token has expired.");
        }
        if (token.isUsed()) {
            throw new ConflictException("That reset token has already been used.");
        }
        token.setUsedAt(now);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Every session dies with the old password (API.md §6.9).
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
    }

    public long resetExpirationSeconds() {
        return resetExpiration.getSeconds();
    }

    /** Generate a fresh opaque token the client carries in the confirm call. */
    static String generateToken() {
        return UUID.randomUUID().toString();
    }

    /** SHA-256 hex of the opaque token - the raw value is never stored. */
    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
