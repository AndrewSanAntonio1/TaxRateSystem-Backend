-- V4 - saved calculations and their explanation rows (API.md §9, §10).
-- deleted_at implements the idempotent DELETE of §10.3: a row that once belonged to the caller
-- stays attributable after deletion.

CREATE TABLE calculations (
    id                 INT            NOT NULL AUTO_INCREMENT,
    user_id            INT            NOT NULL,
    tax_type_id        INT            NOT NULL,
    taxable_income     DECIMAL(15, 2) NOT NULL,
    calculated_tax     DECIMAL(15, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    applicable_bracket VARCHAR(160)   NOT NULL,
    effective_rule     VARCHAR(120)   NOT NULL,
    status             ENUM ('COMPLETED', 'FAILED') NOT NULL,
    created_at         DATETIME(6)    NOT NULL,
    deleted_at         DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_calculations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_calculations_tax_type FOREIGN KEY (tax_type_id) REFERENCES tax_types (id),
    INDEX idx_calculations_user_created (user_id, created_at, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE calculation_breakdowns (
    id             INT          NOT NULL AUTO_INCREMENT,
    calculation_id INT          NOT NULL,
    label          VARCHAR(60)  NOT NULL,
    value          VARCHAR(60)  NOT NULL,
    detail         VARCHAR(120) NULL,
    display_order  INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_calculation_breakdowns_calculation FOREIGN KEY (calculation_id) REFERENCES calculations (id) ON DELETE CASCADE,
    INDEX idx_calculation_breakdowns_calc (calculation_id, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

