package com.project.taxratesystem.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the numeric one-time codes of API.md §6 (length from {@code app.otp.length},
 * default {@code 6}). Uses {@link SecureRandom} so codes are not guessable.
 */
@Component
public class OtpGenerator {

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    /**
     * @param length number of digits, between 4 and 12
     * @return a zero-padded numeric code of exactly {@code length} digits
     */
    public String generate(int length) {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException("OTP length must be between " + MIN_LENGTH + " and " + MAX_LENGTH);
        }
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}

