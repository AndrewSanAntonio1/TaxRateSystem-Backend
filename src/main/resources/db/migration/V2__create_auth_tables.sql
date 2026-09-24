-- V2 - one-time codes, refresh credentials and password-reset tokens (API.md §3, §6).
-- Only hashes are stored: a leaked dump never contains a usable code or token.

CREATE TABLE otp_codes (
    id         INT         NOT NULL AUTO_INCREMENT,
    user_id    INT         NOT NULL,
    purpose    ENUM ('PASSWORD_RESET', 'REGISTRATION') NOT NULL,
    code_hash  VARCHAR(72) NOT NULL,
    attempts   INT         NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at    DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_otp_codes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_otp_codes_user_purpose (user_id, purpose, used_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id         INT         NOT NULL AUTO_INCREMENT,
    user_id    INT         NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id  VARCHAR(36) NOT NULL,
    issued_at  DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_family (family_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id         INT         NOT NULL AUTO_INCREMENT,
    user_id    INT         NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at    DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_password_reset_tokens_user (user_id, used_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

