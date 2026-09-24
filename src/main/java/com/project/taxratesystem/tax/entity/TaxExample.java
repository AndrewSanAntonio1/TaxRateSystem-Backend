package com.project.taxratesystem.tax.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A worked example for a tax type (API.md §8.2, §8.4, §8.6) - display strings rendered verbatim,
 * e.g. {@code ₱600,000} → {@code ₱22,500 + 20% of ₱200,000} = {@code ₱62,500}.
 */
@Entity
@Table(name = "tax_examples", indexes = {
        @Index(name = "idx_tax_examples_type_order", columnList = "tax_type_id, display_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_type_id", nullable = false)
    private TaxType taxType;

    @Column(name = "amount", nullable = false, length = 80)
    private String amount;

    @Column(name = "computation", nullable = false, length = 200)
    private String computation;

    @Column(name = "estimated_tax", nullable = false, length = 80)
    private String estimatedTax;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Convenience factory used by the seeder. */
    public static TaxExample of(String amount, String computation, String estimatedTax) {
        return TaxExample.builder()
                .amount(amount)
                .computation(computation)
                .estimatedTax(estimatedTax)
                .build();
    }
}

