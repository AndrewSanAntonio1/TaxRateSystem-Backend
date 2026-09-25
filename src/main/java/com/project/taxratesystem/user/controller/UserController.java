package com.project.taxratesystem.user.controller;

import com.project.taxratesystem.security.AuthenticatedUser;
import com.project.taxratesystem.user.dto.AddressRequest;
import com.project.taxratesystem.user.dto.ChangePasswordRequest;
import com.project.taxratesystem.user.dto.ChangePasswordResponse;
import com.project.taxratesystem.user.dto.NotificationSettingsRequest;
import com.project.taxratesystem.user.dto.NotificationSettingsResponse;
import com.project.taxratesystem.user.dto.UpdateProfileRequest;
import com.project.taxratesystem.user.dto.UserResponse;
import com.project.taxratesystem.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller-scoped profile routes of API.md §7 and §11.
 *
 * <p>No route accepts a user id: every operation acts on the authenticated subject only, which
 * removes IDOR bugs by construction. All routes require a bearer token (§12) and answer with the
 * full §7.1 profile where the contract says so.
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** §7.1 - read the caller's profile. */
    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(userService.profile(principal.id()));
    }

    /** §7.2 - partial update; absent fields stay unchanged, explicit nulls are cleared. */
    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.id(), request));
    }

    /** §7.3 - create or replace the single address (full replace, not a patch). */
    @PutMapping("/address")
    public ResponseEntity<UserResponse> updateAddress(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(principal.id(), request));
    }

    /** §7.4 - change the password; other sessions are signed out. */
    @PutMapping("/password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.id(), principal.sessionId(), request);
        return ResponseEntity.ok(new ChangePasswordResponse("Password changed successfully."));
    }

    /** §7.5/§11.1 - read the three notification switches. */
    @GetMapping("/notification-settings")
    public ResponseEntity<NotificationSettingsResponse> notificationSettings(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(userService.notificationSettings(principal.id()));
    }

    /** §7.6/§11.2 - persist the full switch set. */
    @PutMapping("/notification-settings")
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(userService.updateNotificationSettings(principal.id(), request));
    }
}

