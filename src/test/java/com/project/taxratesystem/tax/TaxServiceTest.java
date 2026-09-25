package com.project.taxratesystem.tax;

import com.project.taxratesystem.common.exception.ResourceNotFoundException;
import com.project.taxratesystem.tax.dto.TaxBracketResponse;
import com.project.taxratesystem.tax.dto.TaxExampleResponse;
import com.project.taxratesystem.tax.dto.TaxResponse;
import com.project.taxratesystem.tax.entity.TaxBracket;
import com.project.taxratesystem.tax.entity.TaxExample;
import com.project.taxratesystem.tax.entity.TaxType;
import com.project.taxratesystem.tax.repository.TaxTypeRepository;
import com.project.taxratesystem.tax.service.TaxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the catalogue mapping of API.md §8 that need no database: the repository is a
 * mock, so the §8.1 summary shape, the §8.2 detail (ordered brackets + worked example), the two
 * sub-resource routes and the {@code 404} of an unknown code run under the plain test task,
 * without MySQL or the Spring context.
 */
class TaxServiceTest {

    private TaxTypeRepository taxTypeRepository;
    private TaxService taxService;

    @BeforeEach
    void setUp() {
        taxTypeRepository = mock(TaxTypeRepository.class);
        taxService = new TaxService(taxTypeRepository);
    }

    @Test
    void listReturnsSummaryRowsWithBracketCountAndExampleFlag() {
        when(taxTypeRepository.findAllByOrderByIdAsc()).thenReturn(List.of(personalIncomeTax()));

        List<TaxResponse> catalogue = taxService.list();

        assertThat(catalogue).hasSize(1);
        TaxResponse summary = catalogue.get(0);
        assertThat(summary.getCode()).isEqualTo("personal_income_tax");
        assertThat(summary.getName()).isEqualTo("Personal Income Tax");
        assertThat(summary.getDescription()).contains("TRAIN Law");
        assertThat(summary.getCurrentRate()).isEqualTo("0% – 35%");
        assertThat(summary.getEffectiveDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(summary.getBracketCount()).isEqualTo(3);
        assertThat(summary.getHasExample()).isTrue();
        // The §8.1 summary omits the bracket table and the example body.
        assertThat(summary.getBrackets()).isNull();
        assertThat(summary.getExample()).isNull();
    }

    @Test
    void detailReturnsOrderedBracketsAndTheWorkedExample() {
        when(taxTypeRepository.findByCode("personal_income_tax"))
                .thenReturn(Optional.of(personalIncomeTax()));

        TaxResponse detail = taxService.get("personal_income_tax");

        assertThat(detail.getBrackets())
                .extracting(TaxBracketResponse::getRange)
                .containsExactly("₱0 – ₱250,000", "₱250,001 – ₱400,000", "₱400,001 – ₱800,000");
        assertThat(detail.getBrackets().get(0).getRate()).isEqualTo("Exempt");
        assertThat(detail.getExample().getAmount()).isEqualTo("₱600,000");
        assertThat(detail.getExample().getComputation()).isEqualTo("₱22,500 + 20% of ₱200,000");
        assertThat(detail.getExample().getEstimatedTax()).isEqualTo("₱62,500");
        // The §8.2 detail omits the summary-only flags.
        assertThat(detail.getBracketCount()).isNull();
        assertThat(detail.getHasExample()).isNull();
    }

    @Test
    void bracketsOnlyReturnsTheOrderedTable() {
        when(taxTypeRepository.findByCode("vat")).thenReturn(Optional.of(vat()));

        List<TaxBracketResponse> brackets = taxService.brackets("vat");

        assertThat(brackets).extracting(TaxBracketResponse::getRange)
                .containsExactly("Sale of goods or properties", "Sale of services");
        assertThat(brackets.get(0).getRate()).isEqualTo("12%");
    }

    @Test
    void examplesOnlyReturnsTheWorkedRows() {
        when(taxTypeRepository.findByCode("vat")).thenReturn(Optional.of(vat()));

        List<TaxExampleResponse> examples = taxService.examples("vat");

        assertThat(examples).hasSize(1);
        assertThat(examples.get(0).getAmount()).isEqualTo("₱1,000,000");
        assertThat(examples.get(0).getEstimatedTax()).isEqualTo("₱120,000");
    }

    @Test
    void unknownCodeIs404ForEveryRoute() {
        when(taxTypeRepository.findByCode("gst")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxService.get("gst"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No tax type with code 'gst'.");
        assertThatThrownBy(() -> taxService.brackets("gst"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> taxService.examples("gst"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static TaxType personalIncomeTax() {
        TaxType taxType = TaxType.builder()
                .code("personal_income_tax")
                .name("Personal Income Tax")
                .description("Tax on an individual's net taxable income under the Philippine TRAIN Law.")
                .currentRate("0% – 35%")
                .effectiveDate(LocalDate.of(2023, 1, 1))
                .build();
        taxType.addBracket(TaxBracket.of("₱0 – ₱250,000", "Exempt"));
        taxType.addBracket(TaxBracket.of("₱250,001 – ₱400,000", "15% of excess over ₱250,000"));
        taxType.addBracket(TaxBracket.of("₱400,001 – ₱800,000", "₱22,500 + 20% of excess"));
        taxType.addExample(TaxExample.of("₱600,000", "₱22,500 + 20% of ₱200,000", "₱62,500"));
        return taxType;
    }

    private static TaxType vat() {
        TaxType taxType = TaxType.builder()
                .code("vat")
                .name("VAT")
                .description("Consumption tax on the sale of goods and services.")
                .currentRate("12%")
                .effectiveDate(LocalDate.of(2018, 1, 1))
                .build();
        taxType.addBracket(TaxBracket.of("Sale of goods or properties", "12%"));
        taxType.addBracket(TaxBracket.of("Sale of services", "12%"));
        taxType.addExample(TaxExample.of("₱1,000,000", "₱1,000,000 × 12%", "₱120,000"));
        return taxType;
    }
}

