package com.project.taxratesystem.tax.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One row of a tax type's bracket table (API.md §8.2, §8.6).
 *
 * <p>Both columns are display strings rendered verbatim by the client, e.g.
 * {@code ₱400,001 – ₱800,000} / {@code ₱22,500 + 20% of excess}.
 */
@Entity
@Table(name = "tax_brackets", indexes = {
        @Index(name = "idx_tax_brackets_type_order", columnList = "tax_type_id, display_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_type_id", nullable = false)
    private TaxType taxType;

    /** {@code range} is a MySQL reserved word, hence the column name. */
    @Column(name = "bracket_range", nullable = false, length = 160)
    private String range;

    @Column(name = "rate", nullable = false, length = 80)
    private String rate;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Convenience factory used by the seeder. */
    public static TaxBracket of(String range, String rate) {
        return TaxBracket.builder().range(range).rate(rate).build();
    }
}

