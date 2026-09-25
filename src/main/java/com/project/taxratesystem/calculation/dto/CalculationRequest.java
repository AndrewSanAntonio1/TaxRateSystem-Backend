package com.project.taxratesystem.calculation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Body of {@code POST /calculations} (API.md §9.1, §13.5).
 *
 * <p>An unknown {@code taxType} is a field-level {@code 422} raised by the service - not a binding
 * failure - because the value is checked against the ten codes of §8.5 after deserialisation.
 */

public class CalculationRequest {

    @NotBlank(message = "Tax type is required.")
    private String taxType;

    @NotNull(message = "Taxable income is required.")
    @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0")
    @DecimalMax(value = "999999999999.99", message = "must not exceed 999999999999.99")
    @Digits(integer = 12, fraction = 2, message = "must have at most 2 decimal places")
    private BigDecimal taxableIncome;

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }

    public BigDecimal getTaxableIncome() {
        return taxableIncome;
    }

    public void setTaxableIncome(BigDecimal taxableIncome) {
        this.taxableIncome = taxableIncome;
    }
}
