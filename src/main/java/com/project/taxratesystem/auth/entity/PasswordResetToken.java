package com.project.taxratesystem.auth.entity;

import com.project.taxratesystem.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * The short-lived, single-use token handed out by {@code POST /auth/password-reset/verify} and
 * accepted only by {@code POST /auth/password-reset/confirm} (API.md §6.8, §6.9).
 *
 * <p>Only the SHA-256 hash is stored. It is never a session credential: it cannot authenticate a
 * request, and its lifetime is {@code app.password-reset.expiration-minutes} (15 minutes).
 */
@Entity
@Table(name = "password_reset_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_password_reset_tokens_hash", columnNames = "token_hash")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex of the opaque token handed to the client. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
