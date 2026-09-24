-- V3 - the tax catalogue: types, bracket tables and worked examples (API.md §8).
-- The ten tax types themselves are seeded by DataSeeder from §8.6, which is the data contract.

CREATE TABLE tax_types (
    id             INT          NOT NULL AUTO_INCREMENT,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    current_rate   VARCHAR(50)  NOT NULL,
    effective_date DATE         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tax_types_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE tax_brackets (
    id            INT          NOT NULL AUTO_INCREMENT,
    tax_type_id   INT          NOT NULL,
    bracket_range VARCHAR(160) NOT NULL,
    rate          VARCHAR(80)  NOT NULL,
    display_order INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tax_brackets_tax_type FOREIGN KEY (tax_type_id) REFERENCES tax_types (id) ON DELETE CASCADE,
    INDEX idx_tax_brackets_type_order (tax_type_id, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE tax_examples (
    id            INT          NOT NULL AUTO_INCREMENT,
    tax_type_id   INT          NOT NULL,
    amount        VARCHAR(80)  NOT NULL,
    computation   VARCHAR(200) NOT NULL,
    estimated_tax VARCHAR(80)  NOT NULL,
    display_order INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tax_examples_tax_type FOREIGN KEY (tax_type_id) REFERENCES tax_types (id) ON DELETE CASCADE,
    INDEX idx_tax_examples_type_order (tax_type_id, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

