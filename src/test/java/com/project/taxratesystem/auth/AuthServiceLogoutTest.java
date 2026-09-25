package com.project.taxratesystem.auth;

import com.project.taxratesystem.auth.entity.RefreshToken;
import com.project.taxratesystem.auth.repository.RefreshTokenRepository;
import com.project.taxratesystem.auth.service.AuthService;
import com.project.taxratesystem.auth.service.OtpService;
import com.project.taxratesystem.security.JwtService;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.UserStatus;
import com.project.taxratesystem.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the logout/revocation rules of API.md §6.6 that need no database: only the
 * caller's own refresh tokens are touched, unknown and already-revoked values stay silent, and the
 * {@code allDevices} branch revokes every session of the caller.
 */
class AuthServiceLogoutTest {

    private static final Integer USER_ID = 7;
    private static final Integer OTHER_USER_ID = 99;

    private RefreshTokenRepository refreshTokenRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        OtpService otpService = mock(OtpService.class);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtService, otpService, 604800000L);
    }

    @Test
    void logoutRevokesThePresentedTokenOfTheCaller() {
        RefreshToken token = tokenOf(USER_ID);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.logout(USER_ID, "raw-refresh-token", false);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logoutIsIdempotentForAnAlreadyRevokedToken() {
        RefreshToken token = tokenOf(USER_ID);
        Instant firstRevocation = Instant.parse("2026-09-25T00:00:00Z");
        token.revoke(firstRevocation);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.logout(USER_ID, "raw-refresh-token", false);

        assertThat(token.getRevokedAt()).isEqualTo(firstRevocation);
    }

    @Test
    void logoutNeverTouchesAnotherUsersToken() {
        RefreshToken foreign = tokenOf(OTHER_USER_ID);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(foreign));

        authService.logout(USER_ID, "raw-refresh-token", false);

        assertThat(foreign.isRevoked()).isFalse();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logoutWithAnUnknownTokenStillSucceeds() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout(USER_ID, "raw-refresh-token", false);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logoutAllDevicesRevokesEverySessionOfTheCaller() {
        authService.logout(USER_ID, "raw-refresh-token", true);

        verify(refreshTokenRepository).revokeAllForUser(eq(USER_ID), any(Instant.class));
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }

    private static RefreshToken tokenOf(Integer userId) {
        Instant now = Instant.now();
        return RefreshToken.builder()
                .id(1)
                .user(User.builder().id(userId).status(UserStatus.ACTIVE).build())
                .tokenHash("hash")
                .familyId("family")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
    }
}
