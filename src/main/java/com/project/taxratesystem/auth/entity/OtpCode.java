package com.project.taxratesystem.auth.entity;

import com.project.taxratesystem.auth.enums.OtpPurpose;
import com.project.taxratesystem.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A one-time code e-mailed to a user (API.md §6.1, §6.2, §6.3, §6.7, §6.8).
 *
 * <p>Only the BCrypt hash is stored; the plaintext code exists solely inside the e-mail. A code is
 * single-use ({@link #usedAt}) and is invalidated after five failed attempts
 * ({@link #attempts}).
 */
@Entity
@Table(name = "otp_codes", indexes = {
        @Index(name = "idx_otp_codes_user_purpose", columnList = "user_id, purpose, used_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {

    /** Attempts allowed before the code is invalidated (API.md §6.2). */
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 72)
    private String codeHash;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** When the code was successfully used - a used code can never be used again. */
    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isAttemptsExhausted() {
        return attempts >= MAX_ATTEMPTS;
    }
}

