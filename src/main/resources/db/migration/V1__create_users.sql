-- V1 - accounts, the single optional address and the notification preferences (API.md §7, §11).
-- Column types mirror the JPA mappings exactly: ddl-auto=validate fails on any drift.

CREATE TABLE users (
    id            INT          NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(60)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(72)  NOT NULL,
    phone_number  VARCHAR(11)  NULL,
    gender        ENUM ('FEMALE', 'MALE', 'OTHER', 'PREFER_NOT_TO_SAY') NULL,
    date_of_birth DATE         NULL,
    status        ENUM ('ACTIVE', 'DEACTIVATED', 'PENDING_VERIFICATION', 'SUSPENDED') NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE addresses (
    id          INT          NOT NULL AUTO_INCREMENT,
    user_id     INT          NOT NULL,
    street      VARCHAR(160) NOT NULL,
    barangay    VARCHAR(80)  NULL,
    city        VARCHAR(80)  NOT NULL,
    province    VARCHAR(80)  NOT NULL,
    postal_code VARCHAR(4)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_addresses_user UNIQUE (user_id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notification_settings (
    id                    INT NOT NULL AUTO_INCREMENT,
    user_id               INT NOT NULL,
    tax_updates           BIT NOT NULL,
    calculation_reminders BIT NOT NULL,
    general_notifications BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_settings_user UNIQUE (user_id),
    CONSTRAINT fk_notification_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

