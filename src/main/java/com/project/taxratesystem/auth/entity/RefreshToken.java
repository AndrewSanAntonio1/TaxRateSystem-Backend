package com.project.taxratesystem.auth.entity;

import com.project.taxratesystem.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A persisted refresh credential (API.md §3, §6.5). The raw token is never stored - only its
 * SHA-256 hash - and every rotation revokes the presented row and issues a new one in the same
 * {@link #familyId}. Presenting an already-revoked token revokes the whole family (reuse
 * detection).
 */
@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_tokens_hash", columnNames = "token_hash")
}, indexes = {
        @Index(name = "idx_refresh_tokens_family", columnList = "family_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex of the opaque token handed to the client. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** Rotation family: one per login, shared by every token it has been rotated into. */
    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set when the token is rotated, revoked, or reused. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** A token that can still be exchanged for a new pair. */
    public boolean isUsable(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    public void revoke(Instant when) {
        if (this.revokedAt == null) {
            this.revokedAt = when;
        }
    }
}

