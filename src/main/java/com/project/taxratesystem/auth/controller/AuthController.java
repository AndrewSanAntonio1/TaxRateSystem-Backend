package com.project.taxratesystem.auth.controller;

import com.project.taxratesystem.auth.dto.AuthResponse;
import com.project.taxratesystem.auth.dto.LoginRequest;
import com.project.taxratesystem.auth.dto.LogoutRequest;
import com.project.taxratesystem.auth.dto.OtpResponse;
import com.project.taxratesystem.auth.dto.PasswordResetConfirmRequest;
import com.project.taxratesystem.auth.dto.PasswordResetConfirmResponse;
import com.project.taxratesystem.auth.dto.PasswordResetRequest;
import com.project.taxratesystem.auth.dto.PasswordResetVerifyRequest;
import com.project.taxratesystem.auth.dto.PasswordResetVerifyResponse;
import com.project.taxratesystem.auth.dto.RefreshTokenRequest;
import com.project.taxratesystem.auth.dto.RegisterRequest;
import com.project.taxratesystem.auth.dto.RegistrationResponse;
import com.project.taxratesystem.auth.dto.ResendOtpRequest;
import com.project.taxratesystem.auth.dto.VerifyRegistrationRequest;
import com.project.taxratesystem.auth.dto.VerifyRegistrationResponse;
import com.project.taxratesystem.auth.enums.OtpPurpose;
import com.project.taxratesystem.auth.service.AuthService;
import com.project.taxratesystem.auth.service.OtpService;
import com.project.taxratesystem.auth.service.PasswordResetService;
import com.project.taxratesystem.common.exception.ResourceNotFoundException;
import com.project.taxratesystem.security.AuthenticatedUser;
import com.project.taxratesystem.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public authentication routes of API.md §6 (register, verify, resend, login, refresh and the
 * three password-reset steps).
 *
 * <p>Every route here is public: the caller's identity comes only from the credentials or codes in
 * the request body. Responses that could enumerate accounts are deliberately identical for
 * registered and unknown addresses.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, OtpService otpService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.otpService = otpService;
        this.passwordResetService = passwordResetService;
    }

    /** §6.1 - create the account and e-mail its verification code. */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        RegistrationResponse body = new RegistrationResponse(
                "Account created. Enter the 6-digit code we e-mailed you.",
                user.getId(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** §6.2 - activate the account with the e-mailed code. */
    @PostMapping("/verify-registration")
    public ResponseEntity<VerifyRegistrationResponse> verifyRegistration(
            @Valid @RequestBody VerifyRegistrationRequest request) {
        authService.verifyRegistration(request.getEmail(), request.getCode());
        VerifyRegistrationResponse body = new VerifyRegistrationResponse(
                "Account verified.", request.getEmail(), "ACTIVE");
        return ResponseEntity.ok(body);
    }

    /** §6.3 - re-send a code; the response never reveals whether the address exists. */
    @PostMapping("/resend-otp")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        OtpPurpose purpose = request.getPurpose();
        try {
            User user = otpService.resolveUser(request.getEmail());
            otpService.resendCode(user, purpose);
        } catch (ResourceNotFoundException ex) {
            // Unknown (or unavailable) address: still answer with the same 200 envelope.
        }
        return ResponseEntity.ok(
                otpEnvelope("If that address needs a code, one is on its way."));
    }

    /** §6.4 - exchange credentials for tokens. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
    }

    /** §6.5 - rotate the refresh token and issue a new pair. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /** §6.6 - revoke the presented refresh token, or every session when {@code allDevices}. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @Valid @RequestBody LogoutRequest request) {
        authService.logout(principal.id(), request.getRefreshToken(), request.isAllDevices());
        return ResponseEntity.noContent().build();
    }

    /** §6.7 - start password recovery; always the same response, registered or not. */
    @PostMapping("/password-reset/request")
    public ResponseEntity<OtpResponse> passwordResetRequest(
            @Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(
                otpEnvelope("If that address is registered, a reset code is on its way."));
    }

    /** §6.8 - exchange a valid recovery code for a single-use reset token. */
    @PostMapping("/password-reset/verify")
    public ResponseEntity<PasswordResetVerifyResponse> passwordResetVerify(
            @Valid @RequestBody PasswordResetVerifyRequest request) {
        return ResponseEntity.ok(
                passwordResetService.verifyCode(request.getEmail(), request.getCode()));
    }

    /** §6.9 - set the new password; every existing session of the account is revoked. */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<PasswordResetConfirmResponse> passwordResetConfirm(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request.getResetToken(), request.getNewPassword(),
                request.getConfirmPassword());
        return ResponseEntity.ok(
                new PasswordResetConfirmResponse("Password updated. Sign in with your new password."));
    }

    /** The shared envelope of §6.3/§6.7, with the real configured lifetimes. */
    private OtpResponse otpEnvelope(String message) {
        return new OtpResponse(message, otpService.otpExpirationSeconds(),
                otpService.resendCooldownSeconds());
    }
}
