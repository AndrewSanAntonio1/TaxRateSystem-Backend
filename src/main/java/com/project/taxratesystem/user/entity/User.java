package com.project.taxratesystem.user.entity;

import com.project.taxratesystem.common.exception.ForbiddenException;
import com.project.taxratesystem.user.enums.Gender;
import com.project.taxratesystem.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A registered account (API.md §7.1). Registration stores only what the contract defines: the
 * name, e-mail, BCrypt hash, optional phone/gender/birth date, the lifecycle {@link UserStatus}
 * and the single optional address plus the always-present notification preferences.
 *
 * <p>Plaintext passwords are never stored - only {@link #passwordHash}.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_phone_number", columnNames = "phone_number")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    /** Stored trimmed and lower-cased; unique case-insensitively (API.md §6.1). */
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    /** BCrypt hash only - 60 characters today, 72 to leave room for algorithm options. */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    /** PH mobile number, 11 digits starting with 09 (API.md §13.1); optional. */
    @Column(name = "phone_number", length = 11)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Address address;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private NotificationSettings notificationSettings;

    /** Whether the account may sign in and use protected endpoints. */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Guards every protected operation (API.md §4, §10.1): only an {@code ACTIVE} account may act.
     * Suspended and deactivated accounts get {@code 403}; a pending account is told to verify.
     */
    public void requireUsable() {
        if (status == UserStatus.PENDING_VERIFICATION) {
            throw new ForbiddenException("Verify your e-mail address before signing in.");
        }
        if (status != UserStatus.ACTIVE) {
            throw new ForbiddenException("This account is not available.");
        }
    }

    /** Keeps both sides of the relationship consistent when an address is attached or replaced. */
    public void setAddress(Address address) {
        if (this.address == address) {
            return;
        }
        Address previous = this.address;
        this.address = address;
        if (previous != null) {
            previous.setUser(null);
        }
        if (address != null) {
            address.setUser(this);
        }
    }

    /** Keeps both sides of the relationship consistent for the notification preferences. */
    public void setNotificationSettings(NotificationSettings settings) {
        if (this.notificationSettings == settings) {
            return;
        }
        NotificationSettings previous = this.notificationSettings;
        this.notificationSettings = settings;
        if (previous != null) {
            previous.setUser(null);
        }
        if (settings != null) {
            settings.setUser(this);
        }
    }
}

