package com.project.taxratesystem.calculation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One ordered line of a calculation's explanation (API.md §9.1 {@code breakdown}). The last line is
 * always labelled {@code Total Tax Due} - the client keys its sum styling on that exact string.
 */
@Entity
@Table(name = "calculation_breakdowns", indexes = {
        @Index(name = "idx_calculation_breakdowns_calc", columnList = "calculation_id, display_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculation_id", nullable = false)
    private Calculation calculation;

    @Column(name = "label", nullable = false, length = 60)
    private String label;

    @Column(name = "value", nullable = false, length = 60)
    private String value;

    @Column(name = "detail", length = 120)
    private String detail;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
