package com.project.taxratesystem.user.enums;

/**
 * Account lifecycle states (API.md §7.1, §6.4). A new registration starts as
 * {@link #PENDING_VERIFICATION}; verifying the e-mailed code moves it to {@link #ACTIVE}.
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}

