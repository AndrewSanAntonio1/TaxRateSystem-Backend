package com.project.taxratesystem.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * The three notification switches of API.md §11, rendered by the client's notification screen.
 *
 * <p>Defaults after registration: {@code taxUpdates=true}, {@code calculationReminders=false},
 * {@code generalNotifications=true}.
 */
@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "tax_updates", nullable = false)
    private boolean taxUpdates;

    @Column(name = "calculation_reminders", nullable = false)
    private boolean calculationReminders;

    @Column(name = "general_notifications", nullable = false)
    private boolean generalNotifications;

    /** The defaults of API.md §11 for a newly registered account. */
    public static NotificationSettings defaults() {
        return NotificationSettings.builder()
                .taxUpdates(true)
                .calculationReminders(false)
                .generalNotifications(true)
                .build();
    }
}

