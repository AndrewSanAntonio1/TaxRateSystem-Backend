package com.project.taxratesystem.auth.service;

import com.project.taxratesystem.auth.entity.OtpCode;
import com.project.taxratesystem.auth.enums.OtpPurpose;
import com.project.taxratesystem.auth.repository.OtpRepository;
import com.project.taxratesystem.common.exception.ConflictException;
import com.project.taxratesystem.common.exception.ResourceNotFoundException;
import com.project.taxratesystem.common.exception.TooManyRequestsException;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.common.util.OtpGenerator;
import com.project.taxratesystem.notification.OtpEmailService;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.UserStatus;
import com.project.taxratesystem.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * The one-time-code engine of API.md §6 (registration and password reset).
 *
 * <p>Codes are never stored in the clear - only their BCrypt hash - and each code is single-use
 * with a five-attempt limit. The resend throttle is enforced here so a caller cannot flood a
 * mailbox, and every failure that could enumerate accounts is reported identically.
 */
@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final OtpGenerator otpGenerator;
    private final OtpEmailService otpEmailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final Duration otpExpiration;
    private final Duration resendCooldown;
    private final int otpLength;

    public OtpService(OtpRepository otpRepository,
                      OtpGenerator otpGenerator,
                      OtpEmailService otpEmailService,
                      PasswordEncoder passwordEncoder,
                      UserRepository userRepository,
                      @Value("${app.otp.expiration-minutes:5}") long otpExpirationMinutes,
                      @Value("${app.otp.resend-cooldown-seconds:45}") long resendCooldownSeconds,
                      @Value("${app.otp.length:6}") int otpLength) {
        this.otpRepository = otpRepository;
        this.otpGenerator = otpGenerator;
        this.otpEmailService = otpEmailService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.otpExpiration = Duration.ofMinutes(otpExpirationMinutes);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
        this.otpLength = otpLength;
    }

    /** Issue and e-mail a fresh code, invalidating any previous live code for that purpose. */
    @Transactional
    public OtpCode issueCode(User user, OtpPurpose purpose) {
        Instant now = Instant.now();
        otpRepository.deleteAllByUserAndPurposeAndUsedAtIsNull(user, purpose);
        String code = otpGenerator.generate(otpLength);
        OtpCode otp = OtpCode.builder()
                .user(user)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .attempts(0)
                .createdAt(now)
                .expiresAt(now.plus(otpExpiration))
                .build();
        otpRepository.save(otp);
        sendCode(user, purpose, code);
        return otp;
    }

    /**
     * Verify a registration code (API.md §6.2) and mark it used.
     *
     * @throws ResourceNotFoundException {@code 404} no pending account for that e-mail
     * @throws ConflictException         {@code 409} the code was already used
     * @throws ValidationException       {@code 422} wrong or expired code
     * @throws TooManyRequestsException  {@code 429} five failed attempts
     */
    @Transactional
    public OtpCode verifyRegistration(String email, String code) {
        User user = resolvePendingUser(email);
        return consumeVerifiedCode(user, OtpPurpose.REGISTRATION, code);
    }

    /**
     * Verify a password-reset code (API.md §6.8) and mark it used. A missing, suspended or
     * deactivated account fails exactly like a wrong code, so the response never reveals whether
     * an address is registered.
     */
    @Transactional
    public OtpCode verifyPasswordResetCode(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(candidate -> candidate.getStatus() != UserStatus.SUSPENDED
                        && candidate.getStatus() != UserStatus.DEACTIVATED)
                .orElseThrow(() -> new ValidationException("code", "Invalid code."));
        return consumeVerifiedCode(user, OtpPurpose.PASSWORD_RESET, code);
    }

    /** Re-send a code (API.md §6.3), enforcing the cooldown measured from the previous code. */
    @Transactional
    public OtpCode resendCode(User user, OtpPurpose purpose) {
        Instant now = Instant.now();
        otpRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, purpose).ifPresent(last -> {
            Instant eligibleAt = last.getCreatedAt().plus(resendCooldown);
            if (now.isBefore(eligibleAt)) {
                throw new TooManyRequestsException("Resend cooldown active.",
                        Duration.between(now, eligibleAt).getSeconds());
            }
        });
        return issueCode(user, purpose);
    }

    /** The number of seconds a freshly issued code stays valid (response envelope of §6.3/§6.7). */
    public long otpExpirationSeconds() {
        return otpExpiration.getSeconds();
    }

    public long resendCooldownSeconds() {
        return resendCooldown.getSeconds();
    }

    public int otpLength() {
        return otpLength;
    }

    /**
     * Resolve a user for a public resend call; a suspended or deactivated account is reported as
     * missing so the caller answers with the same generic {@code 200} for every address.
     */
    public User resolveUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getStatus() != UserStatus.SUSPENDED
                        && user.getStatus() != UserStatus.DEACTIVATED)
                .orElseThrow(() -> new ResourceNotFoundException("No account for that e-mail address."));
    }

    private User resolvePendingUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending account for that e-mail address."));
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new ResourceNotFoundException("No pending account for that e-mail address.");
        }
        return user;
    }

    /** The newest live code is the only candidate; a used code is a 409, a missing one a 422. */
    private OtpCode consumeVerifiedCode(User user, OtpPurpose purpose, String code) {
        OtpCode otp = otpRepository
                .findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, purpose)
                .orElseThrow(() -> unavailableCode(user, purpose));
        assertCodeValid(otp, code);
        otp.setUsedAt(Instant.now());
        otpRepository.save(otp);
        return otp;
    }

    private RuntimeException unavailableCode(User user, OtpPurpose purpose) {
        return otpRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
                .filter(OtpCode::isUsed)
                .<RuntimeException>map(used -> new ConflictException("That code has already been used."))
                .orElseGet(() -> new ValidationException("code", "Invalid code."));
    }

    private void assertCodeValid(OtpCode otp, String code) {
        Instant now = Instant.now();
        if (otp.isExpired(now)) {
            throw new ValidationException("code", "That code has expired.");
        }
        if (otp.isAttemptsExhausted()) {
            throw new TooManyRequestsException("Too many failed attempts. Request a new code.",
                    resendCooldownSeconds());
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new ValidationException("code", "Invalid code.");
        }
    }

    private void sendCode(User user, OtpPurpose purpose, String code) {
        if (purpose == OtpPurpose.REGISTRATION) {
            otpEmailService.sendRegistrationCode(user.getEmail(), code);
        } else {
            otpEmailService.sendPasswordResetCode(user.getEmail(), code);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

}
