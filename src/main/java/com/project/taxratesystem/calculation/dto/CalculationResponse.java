package com.project.taxratesystem.calculation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.taxratesystem.calculation.entity.Calculation;
import com.project.taxratesystem.calculation.entity.CalculationBreakdown;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The full explanation of one persisted calculation (API.md §9.1), reused verbatim by the history
 * list of §10.1.
 *
 * <p>Every display string (peso signs, en-dashes, bracket wording) is produced by
 * {@code CalculationService} and rendered by the client exactly as sent.
 */

public class CalculationResponse {

    private Integer id;
    private String taxType;
    private String taxTypeName;
    private BigDecimal taxableIncome;
    private BigDecimal calculatedTax;
    private String currency;
    private String applicableBracket;
    private String effectiveRule;
    private String status;
    private List<BreakdownStep> breakdown;
    private Instant createdAt;

    public CalculationResponse() {
    }

    /** Maps a persisted calculation (with its ordered breakdown) to the wire shape of §9.1. */
    public static CalculationResponse of(Calculation calculation) {
        CalculationResponse response = new CalculationResponse();
        response.id = calculation.getId();
        response.taxType = calculation.getTaxType().getCode();
        response.taxTypeName = calculation.getTaxType().getName();
        response.taxableIncome = calculation.getTaxableIncome();
        response.calculatedTax = calculation.getCalculatedTax();
        response.currency = calculation.getCurrency();
        response.applicableBracket = calculation.getApplicableBracket();
        response.effectiveRule = calculation.getEffectiveRule();
        response.status = calculation.getStatus().name();
        response.breakdown = calculation.getBreakdown().stream().map(BreakdownStep::of).toList();
        response.createdAt = calculation.getCreatedAt();
        return response;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }

    public String getTaxTypeName() {
        return taxTypeName;
    }

    public void setTaxTypeName(String taxTypeName) {
        this.taxTypeName = taxTypeName;
    }

    public BigDecimal getTaxableIncome() {
        return taxableIncome;
    }

    public void setTaxableIncome(BigDecimal taxableIncome) {
        this.taxableIncome = taxableIncome;
    }

    public BigDecimal getCalculatedTax() {
        return calculatedTax;
    }

    public void setCalculatedTax(BigDecimal calculatedTax) {
        this.calculatedTax = calculatedTax;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getApplicableBracket() {
        return applicableBracket;
    }

    public void setApplicableBracket(String applicableBracket) {
        this.applicableBracket = applicableBracket;
    }

    public String getEffectiveRule() {
        return effectiveRule;
    }

    public void setEffectiveRule(String effectiveRule) {
        this.effectiveRule = effectiveRule;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<BreakdownStep> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<BreakdownStep> breakdown) {
        this.breakdown = breakdown;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** One ordered explanation row; {@code detail} is omitted when the row has none (§9.1). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BreakdownStep {

        private String label;
        private String value;
        private String detail;

        public BreakdownStep() {
        }

        public BreakdownStep(String label, String value, String detail) {
            this.label = label;
            this.value = value;
            this.detail = detail;
        }

        static BreakdownStep of(CalculationBreakdown step) {
            return new BreakdownStep(step.getLabel(), step.getValue(), step.getDetail());
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}
