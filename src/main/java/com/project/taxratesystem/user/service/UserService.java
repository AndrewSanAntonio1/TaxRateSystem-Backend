package com.project.taxratesystem.user.service;

import com.project.taxratesystem.auth.repository.RefreshTokenRepository;
import com.project.taxratesystem.auth.service.PasswordPolicy;
import com.project.taxratesystem.common.exception.ConflictException;
import com.project.taxratesystem.common.exception.ResourceNotFoundException;
import com.project.taxratesystem.common.exception.UnauthorizedException;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.user.dto.AddressRequest;
import com.project.taxratesystem.user.dto.ChangePasswordRequest;
import com.project.taxratesystem.user.dto.NotificationSettingsRequest;
import com.project.taxratesystem.user.dto.NotificationSettingsResponse;
import com.project.taxratesystem.user.dto.UpdateProfileRequest;
import com.project.taxratesystem.user.dto.UserResponse;
import com.project.taxratesystem.user.entity.Address;
import com.project.taxratesystem.user.entity.NotificationSettings;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.Gender;
import com.project.taxratesystem.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The profile, address, password and notification operations of API.md §7 and §11 - every one of
 * them acts on the authenticated caller only, never on a user id sent by the client.
 *
 * <p>The §13.3/§13.4 rules are enforced here instead of with bean-validation annotations because
 * {@code PUT /users/me} distinguishes "field absent" from "field sent as null": absent leaves the
 * value unchanged, explicit null clears it, and any other value must satisfy the rule.
 */
@Service
public class UserService {

    /** PH mobile number of §13.1: exactly 11 digits starting with 09. */
    private static final Pattern PHONE_NUMBER = Pattern.compile("^09\\d{9}$");

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 60;
    private static final int MIN_AGE = 18;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** §7.1 - the caller's full profile object. */
    @Transactional(readOnly = true)
    public UserResponse profile(Integer userId) {
        return UserResponse.of(requireUser(userId));
    }

    /**
     * §7.2 - partial update: absent = unchanged, explicit null = clear. Returns the full §7.1
     * object after the change.
     *
     * @throws ConflictException   {@code 409} phone number already used by another account
     * @throws ValidationException {@code 422} invalid name, phone, gender or birth date
     */
    @Transactional
    public UserResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = requireUser(userId);

        if (request.isFirstNamePresent()) {
            user.setFirstName(requireName(request.getFirstName(), "firstName", "First name"));
        }
        if (request.isLastNamePresent()) {
            user.setLastName(requireName(request.getLastName(), "lastName", "Last name"));
        }
        if (request.isPhoneNumberPresent()) {
            user.setPhoneNumber(requireAvailablePhone(user, request.getPhoneNumber()));
        }
        if (request.isGenderPresent()) {
            user.setGender(parseGender(request.getGender()));
        }
        if (request.isDateOfBirthPresent()) {
            user.setDateOfBirth(parseDateOfBirth(request.getDateOfBirth()));
        }
        return UserResponse.of(userRepository.save(user));
    }

    /** §7.3 - create or replace the single address; returns the full §7.1 object. */
    @Transactional
    public UserResponse updateAddress(Integer userId, AddressRequest request) {
        User user = requireUser(userId);
        Address address = new Address();
        address.setStreet(request.getStreet().trim());
        address.setBarangay(trimToNull(request.getBarangay()));
        address.setCity(request.getCity().trim());
        address.setProvince(request.getProvince().trim());
        address.setPostalCode(trimToNull(request.getPostalCode()));
        user.setAddress(address);
        return UserResponse.of(userRepository.save(user));
    }

    /**
     * §7.4 - change the password of the caller. Every session except the caller's is revoked, so
     * the other devices are signed out while this one stays signed in.
     *
     * @param sessionId the {@code sid} claim of the bearer token (the caller's refresh rotation
     *                  family, §3); {@code null} when the token predates the claim, in which case
     *                  every session is revoked as §6.9 does
     * @throws UnauthorizedException {@code 401} wrong current password
     * @throws ConflictException     {@code 409} new password equal to the current one
     * @throws ValidationException   {@code 422} mismatch or weak new password (§13.2)
     */
    @Transactional
    public void changePassword(Integer userId, String sessionId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new ConflictException("New password must be different from the current password.");
        }
        PasswordPolicy.validateMatch(request.getNewPassword(), request.getConfirmPassword());
        PasswordPolicy.validate(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // §7.4: every other session dies, the caller's stays. The bearer token's `sid` claim names
        // the refresh rotation family of this call (§3); a token without it (minted before the
        // claim existed) falls back to revoking everything, as a password reset does (§6.9).
        Instant now = Instant.now();
        if (sessionId == null) {
            refreshTokenRepository.revokeAllForUser(user.getId(), now);
        } else {
            refreshTokenRepository.revokeAllForUserExceptFamily(user.getId(), sessionId, now);
        }
    }

    /** §7.5/§11.1 - the three switches, with the §11 defaults when no row exists yet. */
    @Transactional(readOnly = true)
    public NotificationSettingsResponse notificationSettings(Integer userId) {
        User user = requireUser(userId);
        NotificationSettings settings = user.getNotificationSettings();
        return NotificationSettingsResponse.of(
                settings != null ? settings : NotificationSettings.defaults());
    }

    /** §7.6/§11.2 - persist the full switch set; all three booleans are required. */
    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(Integer userId,
                                                                   NotificationSettingsRequest request) {
        User user = requireUser(userId);
        NotificationSettings settings = user.getNotificationSettings();
        if (settings == null) {
            settings = NotificationSettings.defaults();
            user.setNotificationSettings(settings);
        }
        settings.setTaxUpdates(request.getTaxUpdates());
        settings.setCalculationReminders(request.getCalculationReminders());
        settings.setGeneralNotifications(request.getGeneralNotifications());
        userRepository.save(user);
        return NotificationSettingsResponse.of(settings);
    }

    /**
     * Loads the caller behind the bearer token; a token whose account is gone is an error, and an
     * account that is not usable gets the §4 {@code 403} via {@link User#requireUsable()}.
     */
    private User requireUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No account for this session."));
        user.requireUsable();
        return user;
    }

    /** Names may not be cleared: both are required on the account (§13.1, §13.4). */
    private static String requireName(String value, String field, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, label + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(field, label + " must be between " + MIN_NAME_LENGTH
                    + " and " + MAX_NAME_LENGTH + " characters.");
        }
        return trimmed;
    }

    /** Null/blank clears the number; anything else must be a free PH mobile number (§7.2). */
    private String requireAvailablePhone(User user, String value) {
        String phone = trimToNull(value);
        if (phone == null) {
            return null;
        }
        if (!PHONE_NUMBER.matcher(phone).matches()) {
            throw new ValidationException("phoneNumber",
                    "Phone number must be 11 digits starting with 09.");
        }
        boolean takenByAnother = userRepository.findByPhoneNumber(phone)
                .filter(owner -> !owner.getId().equals(user.getId()))
                .isPresent();
        if (takenByAnother) {
            throw new ConflictException("That phone number is already in use by another account.");
        }
        return phone;
    }

    /** Null clears the value; any other string must be one of the §7.1 vocabulary. */
    private static Gender parseGender(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("gender",
                    "Gender must be one of MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY.");
        }
    }

    /** Null clears the value; any other string must be a past date with an 18+ age (§13.4). */
    private static LocalDate parseDateOfBirth(String value) {
        if (value == null) {
            return null;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException("dateOfBirth",
                    "Date of birth must be a valid date (YYYY-MM-DD).");
        }
        LocalDate today = LocalDate.now();
        if (!date.isBefore(today)) {
            throw new ValidationException("dateOfBirth", "Date of birth must be in the past.");
        }
        if (date.isAfter(today.minusYears(MIN_AGE))) {
            throw new ValidationException("dateOfBirth",
                    "You must be at least " + MIN_AGE + " years old.");
        }
        return date;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}



