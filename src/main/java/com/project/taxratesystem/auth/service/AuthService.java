package com.project.taxratesystem.auth.service;

import com.project.taxratesystem.auth.dto.AuthResponse;
import com.project.taxratesystem.auth.dto.RegisterRequest;
import com.project.taxratesystem.auth.entity.OtpCode;
import com.project.taxratesystem.auth.entity.RefreshToken;
import com.project.taxratesystem.auth.enums.OtpPurpose;
import com.project.taxratesystem.auth.repository.RefreshTokenRepository;
import com.project.taxratesystem.common.exception.ConflictException;
import com.project.taxratesystem.common.exception.ForbiddenException;
import com.project.taxratesystem.common.exception.UnauthorizedException;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.security.JwtService;
import com.project.taxratesystem.user.dto.AddressRequest;
import com.project.taxratesystem.user.entity.Address;
import com.project.taxratesystem.user.entity.NotificationSettings;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.UserStatus;
import com.project.taxratesystem.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The credential and session API of API.md §6.1-§6.5 (register, verify, login, refresh).
 *
 * <p>State machine: a registration lands in {@code PENDING_VERIFICATION} and is activated by the
 * e-mailed code; login issues a short-lived access token plus a persisted, hashed, rotatable
 * refresh token. Passwords are stored only as BCrypt hashes.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final Duration refreshExpiration;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OtpService otpService,
                       @Value("${jwt.refresh-token-expiration:604800000}") long refreshExpirationMillis) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.refreshExpiration = Duration.ofMillis(refreshExpirationMillis);
    }

    /**
     * Create a new account (API.md §6.1).
     *
     * <p>The account starts as {@code PENDING_VERIFICATION}; a 6-digit code is e-mailed. A duplicate
     * e-mail is reported with the same {@code 409} message regardless of the existing account's
     * state, and the branch takes comparable time so the address cannot be enumerated.
     */
    @Transactional
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email != null && email.equalsIgnoreCase(trimToNull(request.getPassword()))) {
            throw new ValidationException("password", "Password must not be the same as the e-mail address.");
        }
        PasswordPolicy.validate(request.getPassword());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            // Same message, same approximate cost: hash a dummy password so the timing is
            // indistinguishable from the success branch.
            passwordEncoder.encode(request.getPassword());
            throw new ConflictException("That e-mail address is already registered.");
        }
        String phoneNumber = trimToNull(request.getPhoneNumber());
        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ConflictException("That phone number is already registered.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(phoneNumber);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(Instant.now());
        user.setNotificationSettings(NotificationSettings.defaults());
        attachAddress(user, request.getAddress());

        User saved = userRepository.save(user);
        otpService.issueCode(saved, OtpPurpose.REGISTRATION);
        return saved;
    }

    /**
     * Activate a pending account with its e-mailed code (API.md §6.2).
     *
     * @throws com.project.taxratesystem.common.exception.ResourceNotFoundException {@code 404} no
     *         account, or the account is not pending
     * @throws ConflictException  {@code 409} the code was already used
     * @throws ValidationException {@code 422} wrong or expired code
     */
    @Transactional
    public void verifyRegistration(String email, String code) {
        OtpCode otp = otpService.verifyRegistration(email, code);
        User user = otp.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    /**
     * Exchange credentials for tokens (API.md §6.4).
     *
     * @throws UnauthorizedException {@code 401} unknown e-mail or wrong password (same message)
     * @throws ForbiddenException    {@code 403} unverified, suspended or deactivated account
     */
    @Transactional
    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new UnauthorizedException("Invalid e-mail or password."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid e-mail or password.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException(user.getStatus() == UserStatus.PENDING_VERIFICATION
                    ? "Verify your e-mail address before signing in."
                    : "This account is not available.");
        }
        return issueTokens(user);
    }

    /**
     * Rotate a refresh token and issue a new pair (API.md §6.5).
     *
     * <p>The presented token is revoked and replaced; presenting an already-rotated or revoked
     * token again revokes the whole rotation family (reuse detection).
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token."));

        Instant now = Instant.now();
        if (!existing.isUsable(now)) {
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), now);
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }
        User user = existing.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            existing.revoke(now);
            refreshTokenRepository.save(existing);
            throw new ForbiddenException("This account is not available.");
        }
        existing.revoke(now);
        refreshTokenRepository.save(existing);
        return issueTokens(user, existing.getFamilyId());
    }

    /**
     * Revoke a refresh token, or every refresh token of the caller (API.md §6.6).
     *
     * <p>Idempotent by contract: an unknown, already-revoked or foreign token is accepted silently
     * with the same {@code 204}, so calling it twice is harmless and tokens cannot be probed. Only
     * tokens owned by the authenticated caller are ever touched. Access tokens are not revocable -
     * the presented bearer token stays valid until it expires, as §6.6 documents.
     */
    @Transactional
    public void logout(Integer userId, String refreshToken, boolean allDevices) {
        Instant now = Instant.now();
        if (allDevices) {
            refreshTokenRepository.revokeAllForUser(userId, now);
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .filter(token -> token.getUser().getId().equals(userId))
                .ifPresent(token -> {
                    token.revoke(now);
                    refreshTokenRepository.save(token);
                });
    }

    /** Build the auth response, persisting a brand-new refresh token in a fresh rotation family. */
    private AuthResponse issueTokens(User user) {
        return issueTokens(user, UUID.randomUUID().toString());
    }

    private AuthResponse issueTokens(User user, String familyId) {
        String accessToken = jwtService.generateAccessToken(user, familyId);
        String refreshToken = UUID.randomUUID().toString();
        Instant now = Instant.now();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(refreshToken))
                .familyId(familyId)
                .issuedAt(now)
                .expiresAt(now.plus(refreshExpiration))
                .build();
        refreshTokenRepository.save(token);

        AuthResponse.UserSummary summary = new AuthResponse.UserSummary(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
        return new AuthResponse(accessToken, refreshToken,
                jwtService.getAccessTokenExpirationSeconds(), summary);
    }

    /** SHA-256 hex of the raw token - the raw value is never stored. */
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

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void attachAddress(User user, AddressRequest addressRequest) {
        if (addressRequest == null) {
            return;
        }
        Address address = new Address();
        address.setStreet(addressRequest.getStreet().trim());
        address.setBarangay(trimToNull(addressRequest.getBarangay()));
        address.setCity(addressRequest.getCity().trim());
        address.setProvince(addressRequest.getProvince().trim());
        address.setPostalCode(trimToNull(addressRequest.getPostalCode()));
        user.setAddress(address);
    }
}
