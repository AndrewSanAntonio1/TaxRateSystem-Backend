package com.project.taxratesystem.user;

import com.project.taxratesystem.auth.repository.RefreshTokenRepository;
import com.project.taxratesystem.common.exception.ConflictException;
import com.project.taxratesystem.common.exception.UnauthorizedException;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.user.dto.AddressRequest;
import com.project.taxratesystem.user.dto.ChangePasswordRequest;
import com.project.taxratesystem.user.dto.NotificationSettingsRequest;
import com.project.taxratesystem.user.dto.NotificationSettingsResponse;
import com.project.taxratesystem.user.dto.UpdateProfileRequest;
import com.project.taxratesystem.user.dto.UserResponse;
import com.project.taxratesystem.user.entity.NotificationSettings;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.Gender;
import com.project.taxratesystem.user.enums.UserStatus;
import com.project.taxratesystem.user.repository.UserRepository;
import com.project.taxratesystem.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the profile slice of API.md §7/§11 that need no database: the repositories and
 * the encoder are mocks, so the §7.1 mapping, the absent-vs-null semantics of §7.2, the §7.3
 * address replace, the §7.4 password rules and the §7.5/§7.6 switches run under the plain test
 * task, without MySQL or the Spring context.
 */
class UserServiceTest {

    private static final Integer USER_ID = 7;
    private static final String SESSION_ID = "family-1";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenRepository refreshTokenRepository;
    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userService = new UserService(userRepository, passwordEncoder, refreshTokenRepository);

        user = User.builder()
                .id(USER_ID)
                .firstName("Juan")
                .lastName("Dela Cruz")
                .email("juan@example.com")
                .passwordHash("hash")
                .phoneNumber("09171234567")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.parse("2026-09-23T13:00:00Z"))
                .notificationSettings(NotificationSettings.defaults())
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void profileMapsTheFullApi71Object() {
        user.setGender(Gender.MALE);
        user.setDateOfBirth(LocalDate.of(1996, 4, 17));

        UserResponse profile = userService.profile(USER_ID);

        assertThat(profile.getId()).isEqualTo(USER_ID);
        assertThat(profile.getFirstName()).isEqualTo("Juan");
        assertThat(profile.getLastName()).isEqualTo("Dela Cruz");
        assertThat(profile.getEmail()).isEqualTo("juan@example.com");
        assertThat(profile.getPhoneNumber()).isEqualTo("09171234567");
        assertThat(profile.getGender()).isEqualTo("MALE");
        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(1996, 4, 17));
        assertThat(profile.getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getAddress()).isNull();     // omitted until an address is stored
        assertThat(profile.getNotificationSettings().isTaxUpdates()).isTrue();
        assertThat(profile.getNotificationSettings().isCalculationReminders()).isFalse();
        assertThat(profile.getCreatedAt()).isEqualTo(Instant.parse("2026-09-23T13:00:00Z"));
    }

    @Test
    void updateProfileLeavesAbsentFieldsUnchanged() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("  Juan Miguel  ");

        UserResponse updated = userService.updateProfile(USER_ID, request);

        assertThat(updated.getFirstName()).isEqualTo("Juan Miguel");     // trimmed
        assertThat(updated.getLastName()).isEqualTo("Dela Cruz");        // untouched
        assertThat(updated.getPhoneNumber()).isEqualTo("09171234567");   // untouched
    }

    @Test
    void updateProfileExplicitNullClearsOptionalFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber(null);
        request.setGender(null);
        request.setDateOfBirth(null);

        UserResponse updated = userService.updateProfile(USER_ID, request);

        assertThat(updated.getPhoneNumber()).isNull();
        assertThat(updated.getGender()).isNull();
        assertThat(updated.getDateOfBirth()).isNull();
        // Names cannot be cleared - only omitted (they stay as they were).
        assertThat(updated.getFirstName()).isEqualTo("Juan");
    }

    @Test
    void updateProfilePhoneOfAnotherAccountIs409() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("09179876543");
        when(userRepository.findByPhoneNumber("09179876543"))
                .thenReturn(Optional.of(User.builder().id(99).build()));

        assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateProfileInvalidValuesAre422() {
        UpdateProfileRequest shortName = new UpdateProfileRequest();
        shortName.setFirstName("J");
        assertThatThrownBy(() -> userService.updateProfile(USER_ID, shortName))
                .isInstanceOf(ValidationException.class);

        UpdateProfileRequest badPhone = new UpdateProfileRequest();
        badPhone.setPhoneNumber("12345");
        assertThatThrownBy(() -> userService.updateProfile(USER_ID, badPhone))
                .isInstanceOf(ValidationException.class);

        UpdateProfileRequest badGender = new UpdateProfileRequest();
        badGender.setGender("MALEISH");
        assertThatThrownBy(() -> userService.updateProfile(USER_ID, badGender))
                .isInstanceOf(ValidationException.class);

        UpdateProfileRequest futureDate = new UpdateProfileRequest();
        futureDate.setDateOfBirth(LocalDate.now().plusDays(1).toString());
        assertThatThrownBy(() -> userService.updateProfile(USER_ID, futureDate))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateProfileUnderageDateOfBirthIs422() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDateOfBirth(LocalDate.now().minusYears(10).toString());

        assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateAddressWholesaleReplacesTheStoredAddress() {
        AddressRequest request = new AddressRequest();
        request.setStreet("  123 Rizal Avenue ");
        request.setCity("Manila");
        request.setProvince("Metro Manila");
        request.setPostalCode("1000");

        UserResponse updated = userService.updateAddress(USER_ID, request);

        assertThat(updated.getAddress().getStreet()).isEqualTo("123 Rizal Avenue");
        assertThat(updated.getAddress().getCity()).isEqualTo("Manila");
        assertThat(updated.getAddress().getProvince()).isEqualTo("Metro Manila");
        assertThat(updated.getAddress().getPostalCode()).isEqualTo("1000");
        assertThat(updated.getAddress().getBarangay()).isNull();   // omitted -> cleared
    }

    @Test
    void changePasswordWithAWrongCurrentPasswordIs401() {
        when(passwordEncoder.matches("WrongPassword123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, SESSION_ID,
                changeRequest("WrongPassword123", "SecurePassword456")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void changePasswordToTheSamePasswordIs409() {
        when(passwordEncoder.matches("SecurePassword123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, SESSION_ID,
                changeRequest("SecurePassword123", "SecurePassword123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changePasswordStoresTheNewHashAndRevokesEveryOtherSession() {
        when(passwordEncoder.matches("SecurePassword123", "hash")).thenReturn(true);
        when(passwordEncoder.encode("SecurePassword456")).thenReturn("newHash");

        userService.changePassword(USER_ID, SESSION_ID,
                changeRequest("SecurePassword123", "SecurePassword456"));

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        // §7.4: the caller's rotation family survives - only the other sessions are revoked.
        verify(refreshTokenRepository).revokeAllForUserExceptFamily(eq(USER_ID), eq(SESSION_ID),
                any(Instant.class));
    }

    @Test
    void changePasswordWithoutASessionIdFallsBackToRevokingEverySession() {
        when(passwordEncoder.matches("SecurePassword123", "hash")).thenReturn(true);
        when(passwordEncoder.encode("SecurePassword456")).thenReturn("newHash");

        userService.changePassword(USER_ID, null,
                changeRequest("SecurePassword123", "SecurePassword456"));

        verify(refreshTokenRepository).revokeAllForUser(eq(USER_ID), any(Instant.class));
    }

    @Test
    void notificationSettingsFallBackToDefaultsAndPersistUpdates() {
        User withoutSettings = User.builder()
                .id(USER_ID)
                .firstName("Juan")
                .lastName("Dela Cruz")
                .email("juan@example.com")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(withoutSettings));

        NotificationSettingsResponse defaults = userService.notificationSettings(USER_ID);
        assertThat(defaults.isTaxUpdates()).isTrue();
        assertThat(defaults.isCalculationReminders()).isFalse();
        assertThat(defaults.isGeneralNotifications()).isTrue();

        NotificationSettingsRequest request = new NotificationSettingsRequest();
        request.setTaxUpdates(false);
        request.setCalculationReminders(true);
        request.setGeneralNotifications(false);

        NotificationSettingsResponse updated =
                userService.updateNotificationSettings(USER_ID, request);

        assertThat(updated.isTaxUpdates()).isFalse();
        assertThat(updated.isCalculationReminders()).isTrue();
        assertThat(updated.isGeneralNotifications()).isFalse();
        assertThat(withoutSettings.getNotificationSettings()).isNotNull();
    }

    private static ChangePasswordRequest changeRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(newPassword);
        return request;
    }
}

