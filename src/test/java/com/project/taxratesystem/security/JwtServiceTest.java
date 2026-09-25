package com.project.taxratesystem.security;

import com.project.taxratesystem.auth.enums.TokenType;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the access-token format of API.md §3 that need no database: signature, expiry and
 * the §7.4 {@code sid} (session) claim. {@link JwtService} is pure JDK crypto, so the whole class
 * runs under the plain test task.
 */
class JwtServiceTest {

    /** Exactly {@link JwtService}'s 32-character minimum. */
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static final long FIFTEEN_MINUTES_MILLIS = 900000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, FIFTEEN_MINUTES_MILLIS);
    }

    @Test
    void accessTokenCarriesTheSessionFamilyOfTheLogin() {
        String token = jwtService.generateAccessToken(user(), "family-1");

        JwtService.JwtPayload payload = jwtService.parseAccessToken(token).orElseThrow();

        assertThat(payload.userId()).isEqualTo(7);
        assertThat(payload.subject()).isEqualTo("juan@example.com");
        assertThat(payload.sessionId()).isEqualTo("family-1");
        assertThat(payload.type()).isEqualTo(TokenType.ACCESS);
        assertThat(payload.expiresAt()).isAfter(payload.issuedAt());
    }

    @Test
    void tokenWithoutASessionHasNoSessionId() {
        String token = jwtService.generateAccessToken(user(), null);

        JwtService.JwtPayload payload = jwtService.parseAccessToken(token).orElseThrow();

        assertThat(payload.sessionId()).isNull();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user(), "family-1");
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[0] = payload[0] == 'a' ? 'b' : 'a';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThat(jwtService.parseAccessToken(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = jwtService.generateAccessToken(user(), "family-1");
        JwtService other = new JwtService("fedcba9876543210fedcba9876543210", FIFTEEN_MINUTES_MILLIS);

        assertThat(other.parseAccessToken(token)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService immediateExpiry = new JwtService(SECRET, 0L);
        String token = immediateExpiry.generateAccessToken(user(), "family-1");

        assertThat(immediateExpiry.parseAccessToken(token)).isEmpty();
    }

    private static User user() {
        return User.builder()
                .id(7)
                .email("juan@example.com")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .build();
    }
}
