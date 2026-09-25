package com.project.taxratesystem.security;

import com.project.taxratesystem.auth.enums.TokenType;
import com.project.taxratesystem.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Issues and verifies the HS256 JWT access tokens of API.md §3.
 *
 * <p>Claim set (the contract): {@code sub} = user e-mail, {@code uid} = numeric user id,
 * {@code iat}/{@code exp} = epoch seconds, {@code sid} = the refresh-token rotation family of the
 * session that obtained the token (nullable, §7.4), plus {@code typ} = {@link TokenType#ACCESS} so
 * a token of any other kind can never be replayed as a bearer token.
 *
 * <p>Implemented on JDK crypto (HmacSHA256 + Base64url) instead of adding a JWT dependency: the
 * dependency tree stays exactly as the scaffold declares it. The checks that make this safe are
 * explicit and unit-tested - the algorithm is pinned to {@code HS256}, the signature is compared in
 * constant time, and any malformed part yields an empty result instead of an exception.
 */
@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String EXPECTED_ALGORITHM = "HS256";
    private static final String ISSUER = "TaxRateSystem";

    /** HS256 secrets shorter than the hash output are brute-forceable; refuse to start with one. */
    private static final int MIN_SECRET_BYTES = 32;

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private static final String JWT_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final long accessTokenTtlMillis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-expiration:900000}") long accessTokenTtlMillis) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES
                    + " characters; generate one with e.g. 'openssl rand -base64 48'");
        }
        this.secret = secretBytes;
        this.accessTokenTtlMillis = accessTokenTtlMillis;
    }

    /** The {@code expiresIn} value of login/refresh responses (the access lifetime in seconds). */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenTtlMillis / 1000;
    }

    /**
     * Signs an access token for {@code user}; the raw token must never be logged.
     *
     * @param sessionId the refresh-token rotation family of the session that obtained the token, or
     *                  {@code null} when the caller has no session to keep (API.md §3, §7.4)
     */
    public String generateAccessToken(User user, String sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(accessTokenTtlMillis);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", user.getEmail());
        claims.put("uid", user.getId());
        claims.put("typ", TokenType.ACCESS.name());
        if (sessionId != null) {
            claims.put("sid", sessionId);
        }
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String header = BASE64_URL_ENCODER.encodeToString(JWT_HEADER.getBytes(StandardCharsets.UTF_8));
        String payload = BASE64_URL_ENCODER.encodeToString(
                objectMapper.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        try {
            return signingInput + "." + BASE64_URL_ENCODER.encodeToString(sign(signingInput));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not sign the access token", ex);
        }
    }

    /**
     * Verifies signature, algorithm and expiry of a bearer token.
     *
     * @return the claims, or empty when the token is malformed, tampered with, expired or not an
     *         access token - every case is a {@code 401} for the caller (API.md §3)
     */
    public Optional<JwtPayload> parseAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] presentedSignature = BASE64_URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, presentedSignature)) {
                return Optional.empty();
            }
            Map<String, Object> header = readJson(parts[0]);
            if (!EXPECTED_ALGORITHM.equals(header.get("alg"))) {
                return Optional.empty();
            }
            Map<String, Object> claims = readJson(parts[1]);
            if (!ISSUER.equals(claims.get("iss")) || !TokenType.ACCESS.name().equals(claims.get("typ"))) {
                return Optional.empty();
            }
            String subject = asString(claims.get("sub"));
            Integer userId = asInteger(claims.get("uid"));
            String sessionId = asString(claims.get("sid"));
            Long expiresAtEpoch = asLong(claims.get("exp"));
            Long issuedAtEpoch = asLong(claims.get("iat"));
            if (subject == null || subject.isBlank() || userId == null || expiresAtEpoch == null) {
                return Optional.empty();
            }
            Instant expiresAt = Instant.ofEpochSecond(expiresAtEpoch);
            if (!Instant.now().isBefore(expiresAt)) {
                return Optional.empty();
            }
            Instant issuedAt = issuedAtEpoch != null ? Instant.ofEpochSecond(issuedAtEpoch) : null;
            return Optional.of(new JwtPayload(userId, subject, sessionId, TokenType.ACCESS, issuedAt,
                    expiresAt));
        } catch (IllegalArgumentException | JacksonException | GeneralSecurityException ex) {
            return Optional.empty();
        }
    }

    /** The verified content of an access token. */
    public record JwtPayload(Integer userId, String subject, String sessionId, TokenType type,
                             Instant issuedAt, Instant expiresAt) {
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String base64UrlPart) {
        byte[] json = BASE64_URL_DECODER.decode(base64UrlPart);
        return objectMapper.readValue(new String(json, StandardCharsets.UTF_8), Map.class);
    }

    private byte[] sign(String signingInput) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    }

    private static String asString(Object value) {
        return value instanceof String text ? text : null;
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}

