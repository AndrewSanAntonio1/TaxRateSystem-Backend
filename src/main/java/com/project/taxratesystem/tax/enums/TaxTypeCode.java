package com.project.taxratesystem.tax.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/**
 * The ten tax-type codes of API.md §8.5.
 *
 * <p>The wire value is <strong>lower-snake-case</strong> and is identical to the Flutter client's
 * {@code TaxTypeId.id} strings, which the client compares verbatim. The Java constants stay
 * SCREAMING_SNAKE (Java convention) and {@link #getCode()} - annotated with {@link JsonValue} -
 * carries the transport value so the JSON never changes.
 */
public enum TaxTypeCode {

    PERSONAL_INCOME_TAX("personal_income_tax", "Personal Income Tax"),
    CORPORATE_INCOME_TAX("corporate_income_tax", "Corporate Income Tax"),
    VAT("vat", "VAT"),
    PERCENTAGE_TAX("percentage_tax", "Percentage Tax"),
    CGT_REAL_PROPERTY("cgt_real_property", "CGT – Real Property"),
    CGT_SHARES("cgt_shares", "CGT – Shares"),
    DOCUMENTARY_STAMP_TAX("documentary_stamp_tax", "Documentary Stamp Tax"),
    WITHHOLDING_TAX("withholding_tax", "Withholding Tax"),
    ESTATE_TAX("estate_tax", "Estate Tax"),
    REAL_PROPERTY_TAX("real_property_tax", "Real Property Tax");

    private final String code;
    private final String displayName;

    TaxTypeCode(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /** The stable transport key, e.g. {@code personal_income_tax}. */
    @JsonValue
    public String getCode() {
        return code;
    }

    /** The human-readable name, e.g. {@code Personal Income Tax}. */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves a wire code, or empty when the client sent a code this build does not know.
     * Unknown codes are a {@code 422} on calculations and a {@code 404} on catalogue lookups.
     */
    public static Optional<TaxTypeCode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(candidate -> candidate.code.equals(code))
                .findFirst();
    }
}

