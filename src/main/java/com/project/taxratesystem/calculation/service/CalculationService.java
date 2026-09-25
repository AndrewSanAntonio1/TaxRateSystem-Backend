package com.project.taxratesystem.calculation.service;

import com.project.taxratesystem.calculation.dto.CalculationRequest;
import com.project.taxratesystem.calculation.dto.CalculationResponse;
import com.project.taxratesystem.calculation.entity.Calculation;
import com.project.taxratesystem.calculation.enums.CalculationStatus;
import com.project.taxratesystem.calculation.repository.CalculationRepository;
import com.project.taxratesystem.common.exception.ResourceNotFoundException;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.common.response.PageResponse;
import com.project.taxratesystem.tax.entity.TaxType;
import com.project.taxratesystem.tax.enums.TaxTypeCode;
import com.project.taxratesystem.tax.repository.TaxTypeRepository;
import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The calculator and history engine of API.md §9 and §10.
 *
 * <p>Every rule of §9.2 is reproduced exactly so a figure does not change when the client switches
 * from its offline engine to this API; the canonical case is {@code personal_income_tax} with
 * ₱600,000 returning ₱62,500.00. Money is {@link BigDecimal} end to end and the final amount is
 * rounded once, HALF_UP, at scale 2. History is strictly scoped to the caller.
 */

@Service
public class CalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CORPORATE_REDUCED_CEILING = new BigDecimal("5000000");
    private static final BigDecimal CGT_SHARES_THRESHOLD = new BigDecimal("100000");
    private static final BigDecimal CGT_SHARES_BASE = new BigDecimal("5000");
    private static final BigDecimal ESTATE_STANDARD_DEDUCTION = new BigDecimal("5000000");

    private static final BigDecimal CORPORATE_REDUCED_RATE = new BigDecimal("0.20");
    private static final BigDecimal CORPORATE_RATE = new BigDecimal("0.25");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
    private static final BigDecimal PERCENTAGE_TAX_RATE = new BigDecimal("0.03");
    private static final BigDecimal CGT_PROPERTY_RATE = new BigDecimal("0.06");
    private static final BigDecimal CGT_SHARES_LOW_RATE = new BigDecimal("0.05");
    private static final BigDecimal CGT_SHARES_HIGH_RATE = new BigDecimal("0.10");
    private static final BigDecimal DST_RATE = new BigDecimal("0.015");
    private static final BigDecimal WITHHOLDING_RATE = new BigDecimal("0.10");
    private static final BigDecimal ESTATE_RATE = new BigDecimal("0.06");
    private static final BigDecimal RPT_RATE = new BigDecimal("0.01");

    private static final String PIT_RULE = "TRAIN Law Series (Jan 1, 2023)";
    private static final String TRAIN_RULE_2018 = "TRAIN Law (Jan 1, 2018)";
    private static final String CGT_RULE = "NIRC – Capital Gains (Jan 1, 1998)";
    private static final String DST_RULE = "NIRC – DST (Jan 1, 2005)";
    private static final String RPT_RULE = "Local Government Code (Jan 1, 1992)";
    private static final String CORPORATE_RULE = "CREATE Law (Jul 1, 2020)";

    /** The only progressive table of §9.2 (TRAIN Law, effective 2023-01-01). */
    private static final List<ProgressiveBracket> PERSONAL_INCOME_BRACKETS = List.of(
            new ProgressiveBracket("₱0 – ₱250,000", new BigDecimal("0"), new BigDecimal("250000"),
                    new BigDecimal("0"), new BigDecimal("0")),
            new ProgressiveBracket("₱250,001 – ₱400,000", new BigDecimal("250000"), new BigDecimal("400000"),
                    new BigDecimal("0"), new BigDecimal("0.15")),
            new ProgressiveBracket("₱400,001 – ₱800,000", new BigDecimal("400000"), new BigDecimal("800000"),
                    new BigDecimal("22500"), new BigDecimal("0.20")),
            new ProgressiveBracket("₱800,001 – ₱2,000,000", new BigDecimal("800000"), new BigDecimal("2000000"),
                    new BigDecimal("102500"), new BigDecimal("0.25")),
            new ProgressiveBracket("₱2,000,001 – ₱8,000,000", new BigDecimal("2000000"), new BigDecimal("8000000"),
                    new BigDecimal("402500"), new BigDecimal("0.30")),
            new ProgressiveBracket("Over ₱8,000,000", new BigDecimal("8000000"), null,
                    new BigDecimal("2202500"), new BigDecimal("0.35")));

    /** Sort whitelist of §10.1/§15 mapped to entity property paths. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "calculatedTax", "calculatedTax",
            "taxType", "taxType.code");

    private final CalculationRepository calculationRepository;
    private final TaxTypeRepository taxTypeRepository;
    private final UserRepository userRepository;

    public CalculationService(CalculationRepository calculationRepository,
                              TaxTypeRepository taxTypeRepository,
                              UserRepository userRepository) {
        this.calculationRepository = calculationRepository;
        this.taxTypeRepository = taxTypeRepository;
        this.userRepository = userRepository;
    }

    /**
     * Compute, persist and return one calculation (API.md §9.1). Persisting it <em>is</em> the
     * client's "save to history": there is no separate save endpoint.
     */
    @Transactional
    public CalculationResponse calculate(Integer userId, CalculationRequest request) {
        TaxTypeCode code = TaxTypeCode.fromCode(request.getTaxType())
                .orElseThrow(() -> new ValidationException("taxType", "unknown tax type code"));
        TaxType taxType = taxTypeRepository.findByCode(code.getCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tax type with code '" + code.getCode() + "'."));
        User user = requireUser(userId);

        Result result = compute(code, request.getTaxableIncome());

        Calculation calculation = new Calculation();
        calculation.setUser(user);
        calculation.setTaxType(taxType);
        calculation.setTaxableIncome(request.getTaxableIncome());
        calculation.setCalculatedTax(result.tax());
        calculation.setCurrency("PHP");
        calculation.setApplicableBracket(result.applicableBracket());
        calculation.setEffectiveRule(result.effectiveRule());
        calculation.setStatus(CalculationStatus.COMPLETED);
        calculation.setCreatedAt(Instant.now());
        for (Step step : result.steps()) {
            calculation.addBreakdownStep(step.label(), step.value(), step.detail());
        }
        return CalculationResponse.of(calculationRepository.save(calculation));
    }

    /**
     * The caller's history, newest first by default (API.md §10.1/§15). An unknown {@code taxType}
     * filter yields an empty page, never a {@code 404}.
     */
    @Transactional(readOnly = true)
    public PageResponse<CalculationResponse> history(Integer userId, int page, int size, String sort,
                                                     String taxType) {
        Pageable pageable = PageRequest.of(page, size, sortOf(sort));
        Page<Calculation> results = (taxType == null || taxType.isBlank())
                ? calculationRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                : calculationRepository.findByUserIdAndTaxTypeCodeAndDeletedAtIsNull(userId, taxType,
                        pageable);
        return PageResponse.from(results, CalculationResponse::of);
    }

    /** One owned calculation with its full breakdown; a foreign id is a {@code 404} (§10.2). */
    @Transactional(readOnly = true)
    public CalculationResponse get(Integer userId, Integer id) {
        Calculation calculation = calculationRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No calculation with id " + id + "."));
        return CalculationResponse.of(calculation);
    }

    /** Idempotent soft delete: an id that once belonged to the caller stays a {@code 204} (§10.3). */
    @Transactional
    public void delete(Integer userId, Integer id) {
        Calculation calculation = calculationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No calculation with id " + id + "."));
        if (!calculation.isDeleted()) {
            calculation.setDeletedAt(Instant.now());
        }
    }

    /** Clears the caller's whole history and never anyone else's (§10.4). */
    @Transactional
    public void clearHistory(Integer userId) {
        calculationRepository.softDeleteAllForUser(userId, Instant.now());
    }

    /** The caller of a protected calculation call; a non-active account gets a {@code 403}. */
    private User requireUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No account for this session."));
        user.requireUsable();
        return user;
    }

    private static Sort sortOf(String sort) {
        String value = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort.trim();
        String[] parts = value.split(",");
        if (parts.length != 2) {
            throw invalidSort();
        }
        String property = SORT_FIELDS.get(parts[0].trim());
        Sort.Direction direction = switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> null;
        };
        if (property == null || direction == null) {
            throw invalidSort();
        }
        // Ties on the primary key are broken by id desc so paging is stable (API.md §15).
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private static ValidationException invalidSort() {
        return new ValidationException(HttpStatus.BAD_REQUEST, "Invalid request",
                Map.of("sort", "must be one of createdAt, calculatedTax, taxType with asc or desc"));
    }

    /** The ten rules of §9.2, reproduced exactly so the API matches the offline engine. */
    private Result compute(TaxTypeCode code, BigDecimal amount) {
        return switch (code) {
            case PERSONAL_INCOME_TAX -> personalIncome(amount);
            case CORPORATE_INCOME_TAX -> corporateIncome(amount);
            case VAT -> flat(amount, VAT_RATE, "Sale of goods or properties", "Gross Sales",
                    TRAIN_RULE_2018);
            case PERCENTAGE_TAX -> flat(amount, PERCENTAGE_TAX_RATE, "Gross sales or receipts ≤ ₱3M",
                    "Gross Sales", TRAIN_RULE_2018);
            case CGT_REAL_PROPERTY -> flat(amount, CGT_PROPERTY_RATE, "Selling price or zonal value",
                    "Selling Price", CGT_RULE);
            case DOCUMENTARY_STAMP_TAX -> flat(amount, DST_RATE, "Deeds of sale", "Document Value",
                    DST_RULE);
            case WITHHOLDING_TAX -> flat(amount, WITHHOLDING_RATE, "Professional fees", "Gross Payment",
                    TRAIN_RULE_2018);
            case REAL_PROPERTY_TAX -> flat(amount, RPT_RATE, "Basic RPT (city or municipality)",
                    "Assessed Value", RPT_RULE);
            case CGT_SHARES -> cgtShares(amount);
            case ESTATE_TAX -> estate(amount);
        };
    }

    /** Flat-rate family: one amount, one rate, three rows (§9.2). */
    private Result flat(BigDecimal amount, BigDecimal rate, String bracket, String amountLabel,
                        String rule) {
        BigDecimal tax = money(amount.multiply(rate));
        String percent = percent(rate);
        return new Result(tax, bracket + " (" + percent + ")", rule, List.of(
                new Step(amountLabel, peso(amount), null),
                new Step("Rate Applied", percent, null),
                new Step("Total Tax Due", peso(tax), null)));
    }

    /** Progressive family (TRAIN Law, §9.2): the first bracket the income does not exceed wins. */
    private Result personalIncome(BigDecimal income) {
        ProgressiveBracket bracket = PERSONAL_INCOME_BRACKETS.stream()
                .filter(candidate -> candidate.upperBound() == null
                        || income.compareTo(candidate.upperBound()) <= 0)
                .findFirst()
                .orElseThrow();
        if (bracket.rate().signum() == 0) {
            return new Result(money(BigDecimal.ZERO), bracket.label() + " (Exempt)", PIT_RULE, List.of(
                    new Step("Total Tax Due", peso(BigDecimal.ZERO), null)));
        }
        BigDecimal excess = income.subtract(bracket.lowerBound());
        BigDecimal excessTax = money(excess.multiply(bracket.rate()));
        BigDecimal tax = money(bracket.baseTax().add(excess.multiply(bracket.rate())));
        String percent = percent(bracket.rate());
        return new Result(tax, bracket.label() + " (" + percent + " excess rate)", PIT_RULE, List.of(
                new Step("Base Tax", peso(bracket.baseTax()), null),
                new Step("Excess Amount", peso(excess), percent + " = " + peso(excessTax)),
                new Step("Total Tax Due", peso(tax), null)));
    }

    /** CREATE Law (§9.2): 20% at or below ₱5M net income, otherwise 25%. */
    private Result corporateIncome(BigDecimal income) {
        boolean reduced = income.compareTo(CORPORATE_REDUCED_CEILING) <= 0;
        BigDecimal rate = reduced ? CORPORATE_REDUCED_RATE : CORPORATE_RATE;
        BigDecimal tax = money(income.multiply(rate));
        String bracket = reduced ? "Domestic corp., net taxable income ≤ ₱5M"
                : "Domestic corp., net taxable income > ₱5M";
        String percent = percent(rate);
        return new Result(tax, bracket + " (" + percent + ")", CORPORATE_RULE, List.of(
                new Step("Net Taxable Income", peso(income), null),
                new Step("Rate Applied", percent, null),
                new Step("Total Tax Due", peso(tax), null)));
    }

    /** CGT on shares (§9.2): 5% up to a ₱100,000 gain, then ₱5,000 + 10% of the excess. */
    private Result cgtShares(BigDecimal gain) {
        List<Step> steps = new ArrayList<>();
        if (gain.compareTo(CGT_SHARES_THRESHOLD) > 0) {
            BigDecimal excess = gain.subtract(CGT_SHARES_THRESHOLD);
            BigDecimal tax = money(CGT_SHARES_BASE.add(excess.multiply(CGT_SHARES_HIGH_RATE)));
            steps.add(new Step("Base Tax", peso(CGT_SHARES_BASE), null));
            steps.add(new Step("Applicable Rate", "10%", "On gain of " + peso(excess)));
            steps.add(new Step("Total Tax Due", peso(tax), null));
            return new Result(tax, "Net gain > ₱100,000 (10% of the excess)", CGT_RULE, List.copyOf(steps));
        }
        BigDecimal tax = money(gain.multiply(CGT_SHARES_LOW_RATE));
        steps.add(new Step("Applicable Rate", "5%", "On gain of " + peso(gain)));
        steps.add(new Step("Total Tax Due", peso(tax), null));
        return new Result(tax, "Net gain ≤ ₱100,000 (5%)", CGT_RULE, List.copyOf(steps));
    }

    /** Estate tax (§9.2): ₱5M standard deduction, then 6% of the excess. */
    private Result estate(BigDecimal netEstate) {
        BigDecimal excess = netEstate.subtract(ESTATE_STANDARD_DEDUCTION).max(BigDecimal.ZERO);
        boolean exempt = netEstate.compareTo(ESTATE_STANDARD_DEDUCTION) <= 0;
        BigDecimal tax = exempt ? money(BigDecimal.ZERO) : money(excess.multiply(ESTATE_RATE));
        String bracket = exempt ? "Net estate ≤ ₱5M (standard deduction) (Exempt)"
                : "Net estate > ₱5M (6% of the excess)";
        return new Result(tax, bracket, TRAIN_RULE_2018, List.of(
                new Step("Net Estate", peso(netEstate), null),
                new Step("Standard Deduction", peso(ESTATE_STANDARD_DEDUCTION), null),
                new Step("Excess Rate", percent(ESTATE_RATE), "On excess of " + peso(excess)),
                new Step("Total Tax Due", peso(tax), null)));
    }

    /** The single rounding point: final amounts are HALF_UP at scale 2 (API.md §9.1). */
    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** A peso display string, e.g. 62500 → {@code ₱62,500.00}. */
    private static String peso(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "₱" + format.format(value.setScale(2, RoundingMode.HALF_UP));
    }

    /** A rate display string, e.g. 0.20 → {@code 20%} and 0.015 → {@code 1.5%}. */
    private static String percent(BigDecimal rate) {
        return rate.multiply(HUNDRED).stripTrailingZeros().toPlainString() + "%";
    }

    /** One explanation row before it is persisted (API.md §9.1 {@code breakdown}). */
    private record Step(String label, String value, String detail) {
    }

    /** A computed tax plus everything the response reports about it. */
    private record Result(BigDecimal tax, String applicableBracket, String effectiveRule, List<Step> steps) {
    }

    /** One row of the TRAIN progressive table (§9.2); a null upper bound means unbounded. */
    private record ProgressiveBracket(String label, BigDecimal lowerBound, BigDecimal upperBound,
                                      BigDecimal baseTax, BigDecimal rate) {
    }
}
