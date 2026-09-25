package com.project.taxratesystem.calculation;

import com.project.taxratesystem.calculation.dto.CalculationRequest;
import com.project.taxratesystem.calculation.dto.CalculationResponse;
import com.project.taxratesystem.calculation.entity.Calculation;
import com.project.taxratesystem.calculation.repository.CalculationRepository;
import com.project.taxratesystem.calculation.service.CalculationService;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.tax.entity.TaxType;
import com.project.taxratesystem.tax.repository.TaxTypeRepository;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.UserStatus;
import com.project.taxratesystem.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the calculation engine of API.md §9.2 that need no database: the repositories are
 * mocks, so the canonical case of §9.1 ({@code personal_income_tax} on ₱600,000 returning
 * ₱62,500.00) runs under the plain test task, without MySQL or the Spring context.
 */
class CalculationServiceTest {

    private static final Integer USER_ID = 7;

    private CalculationRepository calculationRepository;
    private CalculationService calculationService;
    private TaxTypeRepository taxTypeRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        calculationRepository = mock(CalculationRepository.class);
        taxTypeRepository = mock(TaxTypeRepository.class);
        userRepository = mock(UserRepository.class);
        calculationService = new CalculationService(calculationRepository, taxTypeRepository, userRepository);
        // save() echoes the entity so the response maps exactly what the service built.
        when(calculationRepository.save(any(Calculation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder()
                .id(USER_ID)
                .email("juan@example.com")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .build()));
    }

    @Test
    void canonicalPersonalIncomeCaseReturns62500() {
        givenTaxType("personal_income_tax", "Personal Income Tax");

        CalculationResponse response = calculate("personal_income_tax", "600000");

        assertThat(response.getCalculatedTax()).isEqualByComparingTo("62500.00");
        assertThat(response.getApplicableBracket()).isEqualTo("₱400,001 – ₱800,000 (20% excess rate)");
        assertThat(response.getEffectiveRule()).isEqualTo("TRAIN Law Series (Jan 1, 2023)");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getCurrency()).isEqualTo("PHP");
        assertThat(response.getBreakdown())
                .extracting(CalculationResponse.BreakdownStep::getLabel)
                .containsExactly("Base Tax", "Excess Amount", "Total Tax Due");
        assertThat(response.getBreakdown().get(0).getValue()).isEqualTo("₱22,500.00");
        assertThat(response.getBreakdown().get(1).getValue()).isEqualTo("₱200,000.00");
        assertThat(response.getBreakdown().get(1).getDetail()).isEqualTo("20% = ₱40,000.00");
        assertThat(response.getBreakdown().get(2).getValue()).isEqualTo("₱62,500.00");
    }

    @Test
    void exemptPersonalIncomeOmitsTheRateRows() {
        givenTaxType("personal_income_tax", "Personal Income Tax");

        CalculationResponse response = calculate("personal_income_tax", "200000");

        assertThat(response.getCalculatedTax()).isEqualByComparingTo("0.00");
        assertThat(response.getApplicableBracket()).isEqualTo("₱0 – ₱250,000 (Exempt)");
        assertThat(response.getBreakdown())
                .extracting(CalculationResponse.BreakdownStep::getLabel)
                .containsExactly("Total Tax Due");
    }

    @Test
    void flatRateVatReturnsTwelvePercent() {
        givenTaxType("vat", "VAT");

        CalculationResponse response = calculate("vat", "100000");

        assertThat(response.getCalculatedTax()).isEqualByComparingTo("12000.00");
        assertThat(response.getApplicableBracket()).isEqualTo("Sale of goods or properties (12%)");
        assertThat(response.getEffectiveRule()).isEqualTo("TRAIN Law (Jan 1, 2018)");
        assertThat(response.getBreakdown().get(0).getLabel()).isEqualTo("Gross Sales");
        assertThat(response.getBreakdown().get(0).getValue()).isEqualTo("₱100,000.00");
    }

    @Test
    void corporateIncomeAboveFiveMillionUsesTwentyFivePercent() {
        givenTaxType("corporate_income_tax", "Corporate Income Tax");

        CalculationResponse response = calculate("corporate_income_tax", "10000000");

        assertThat(response.getCalculatedTax()).isEqualByComparingTo("2500000.00");
        assertThat(response.getApplicableBracket())
                .isEqualTo("Domestic corp., net taxable income > ₱5M (25%)");
        assertThat(response.getEffectiveRule()).isEqualTo("CREATE Law (Jul 1, 2020)");
    }

    @Test
    void cgtSharesAboveThresholdAddsBaseTaxOfFiveThousand() {
        givenTaxType("cgt_shares", "CGT – Shares");

        CalculationResponse response = calculate("cgt_shares", "150000");

        assertThat(response.getCalculatedTax()).isEqualByComparingTo("10000.00");
        assertThat(response.getApplicableBracket()).isEqualTo("Net gain > ₱100,000 (10% of the excess)");
        assertThat(response.getBreakdown())
                .extracting(CalculationResponse.BreakdownStep::getLabel)
                .containsExactly("Base Tax", "Applicable Rate", "Total Tax Due");
        assertThat(response.getBreakdown().get(0).getValue()).isEqualTo("₱5,000.00");
        assertThat(response.getBreakdown().get(1).getDetail()).isEqualTo("On gain of ₱50,000.00");
    }

    @Test
    void estateAboveStandardDeductionTaxesOnlyTheExcess() {
        givenTaxType("estate_tax", "Estate Tax");

        CalculationResponse response = calculate("estate_tax", "15000000");

        assertThat(response.getCalculatedTax()).isEqualByComparingTo("600000.00");
        assertThat(response.getBreakdown())
                .extracting(CalculationResponse.BreakdownStep::getLabel)
                .containsExactly("Net Estate", "Standard Deduction", "Excess Rate", "Total Tax Due");
        assertThat(response.getBreakdown().get(3).getValue()).isEqualTo("₱600,000.00");
    }

    @Test
    void unknownTaxTypeIsAFieldLevelValidationFailure() {
        CalculationRequest request = request("gst", "1000");

        assertThatThrownBy(() -> calculationService.calculate(USER_ID, request))
                .isInstanceOf(ValidationException.class);
    }

    private CalculationResponse calculate(String taxType, String amount) {
        return calculationService.calculate(USER_ID, request(taxType, amount));
    }

    private void givenTaxType(String code, String name) {
        when(taxTypeRepository.findByCode(code)).thenReturn(Optional.of(TaxType.builder()
                .code(code)
                .name(name)
                .build()));
    }

    private static CalculationRequest request(String taxType, String amount) {
        CalculationRequest request = new CalculationRequest();
        request.setTaxType(taxType);
        request.setTaxableIncome(new BigDecimal(amount));
        return request;
    }

}
