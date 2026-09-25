package com.project.taxratesystem.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /users/me/notification-settings} (API.md §7.6, §11.2).
 *
 * <p>All three switches are required - the client always sends the full set - so a missing one is
 * a {@code 422} with that field in the {@code fields} map.
 */
public class NotificationSettingsRequest {

    @NotNull(message = "Tax updates preference is required.")
    private Boolean taxUpdates;

    @NotNull(message = "Calculation reminders preference is required.")
    private Boolean calculationReminders;

    @NotNull(message = "General notifications preference is required.")
    private Boolean generalNotifications;

    public Boolean getTaxUpdates() {
        return taxUpdates;
    }

    public void setTaxUpdates(Boolean taxUpdates) {
        this.taxUpdates = taxUpdates;
    }

    public Boolean getCalculationReminders() {
        return calculationReminders;
    }

    public void setCalculationReminders(Boolean calculationReminders) {
        this.calculationReminders = calculationReminders;
    }

    public Boolean getGeneralNotifications() {
        return generalNotifications;
    }

    public void setGeneralNotifications(Boolean generalNotifications) {
        this.generalNotifications = generalNotifications;
    }
}

