package com.project.taxratesystem.user.dto;

import com.project.taxratesystem.user.entity.NotificationSettings;

/**
 * The three notification switches of API.md §7.5/§7.6 and §11, embedded in every
 * {@link UserResponse} and returned bare by the two settings routes.
 */
public class NotificationSettingsResponse {

    private boolean taxUpdates;
    private boolean calculationReminders;
    private boolean generalNotifications;

    public static NotificationSettingsResponse of(NotificationSettings settings) {
        NotificationSettingsResponse response = new NotificationSettingsResponse();
        response.taxUpdates = settings.isTaxUpdates();
        response.calculationReminders = settings.isCalculationReminders();
        response.generalNotifications = settings.isGeneralNotifications();
        return response;
    }

    public boolean isTaxUpdates() {
        return taxUpdates;
    }

    public void setTaxUpdates(boolean taxUpdates) {
        this.taxUpdates = taxUpdates;
    }

    public boolean isCalculationReminders() {
        return calculationReminders;
    }

    public void setCalculationReminders(boolean calculationReminders) {
        this.calculationReminders = calculationReminders;
    }

    public boolean isGeneralNotifications() {
        return generalNotifications;
    }

    public void setGeneralNotifications(boolean generalNotifications) {
        this.generalNotifications = generalNotifications;
    }
}
