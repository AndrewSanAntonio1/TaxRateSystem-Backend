package com.project.taxratesystem.calculation.entity;

import com.project.taxratesystem.calculation.enums.CalculationStatus;
import com.project.taxratesystem.tax.entity.TaxType;
import com.project.taxratesystem.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One persisted calculation - the history entry of API.md §9.1/§10. Saving is implicit: a
 * successful {@code POST /calculations} stores this row against the caller.
 *
 * <p>Money is {@link BigDecimal} mapped to {@code decimal(15,2)}; the computed amount is rounded
 * once, HALF_UP, before it is stored.
 */
@Entity
@Table(name = "calculations", indexes = {
        @Index(name = "idx_calculations_user_created", columnList = "user_id, created_at, id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Calculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_type_id", nullable = false)
    private TaxType taxType;

    @Column(name = "taxable_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableIncome;

    @Column(name = "calculated_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal calculatedTax;

    /** Always {@code PHP} (API.md §14). */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Display string, e.g. {@code ₱400,001 – ₱800,000 (20% excess rate)}. */
    @Column(name = "applicable_bracket", nullable = false, length = 160)
    private String applicableBracket;

    /** Display string, e.g. {@code TRAIN Law Series (Jan 1, 2023)}. */
    @Column(name = "effective_rule", nullable = false, length = 120)
    private String effectiveRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalculationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Set when the caller deletes the entry. Rows are soft-deleted so {@code DELETE
     * /calculations/{id}} can stay idempotent for an id that once belonged to the caller
     * (API.md §10.3) while still returning {@code 404} for an id that never did.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "calculation", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<CalculationBreakdown> breakdown = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void addBreakdownStep(String label, String value, String detail) {
        CalculationBreakdown step = CalculationBreakdown.builder()
                .label(label)
                .value(value)
                .detail(detail)
                .displayOrder(breakdown.size())
                .build();
        step.setCalculation(this);
        breakdown.add(step);
    }
}

