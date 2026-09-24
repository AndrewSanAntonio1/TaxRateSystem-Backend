package com.project.taxratesystem.tax.entity;

import com.project.taxratesystem.tax.enums.TaxTypeCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One row per tax type of API.md §8.5/§8.6, with its bracket table and worked example.
 *
 * <p>All display strings ({@code currentRate}, bracket {@code range}/{@code rate}, example
 * {@code amount}/{@code computation}/{@code estimatedTax}) are stored exactly as the mobile app
 * renders them: peso signs, en-dashes and phrasing are part of the contract, not formatting to be
 * "improved".
 */
@Entity
@Table(name = "tax_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tax_types_code", columnNames = "code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The stable lower-snake-case transport key, e.g. {@code personal_income_tax}. */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /** Display string, e.g. {@code 0% – 35%}. */
    @Column(name = "current_rate", nullable = false, length = 50)
    private String currentRate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @OneToMany(mappedBy = "taxType", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<TaxBracket> brackets = new ArrayList<>();

    @OneToMany(mappedBy = "taxType", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<TaxExample> examples = new ArrayList<>();

    /** Resolves the enum counterpart of {@link #code}. */
    public TaxTypeCode codeEnum() {
        return TaxTypeCode.fromCode(code)
                .orElseThrow(() -> new IllegalStateException("Unknown tax type code in the database: " + code));
    }

    public void addBracket(TaxBracket bracket) {
        bracket.setTaxType(this);
        bracket.setDisplayOrder(brackets.size());
        brackets.add(bracket);
    }

    public void addExample(TaxExample example) {
        example.setTaxType(this);
        example.setDisplayOrder(examples.size());
        examples.add(example);
    }
}

