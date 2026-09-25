package com.project.taxratesystem.tax.service;

import com.project.taxratesystem.common.exception.ResourceNotFoundException;
import com.project.taxratesystem.tax.dto.TaxBracketResponse;
import com.project.taxratesystem.tax.dto.TaxExampleResponse;
import com.project.taxratesystem.tax.dto.TaxResponse;
import com.project.taxratesystem.tax.entity.TaxType;
import com.project.taxratesystem.tax.repository.TaxTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The read-only tax catalogue of API.md §8.
 *
 * <p>The data is seeded by {@code DataSeeder} from §8.6 and identical for every client, so every
 * route here is public (no token). An unknown {@code code} is a {@code 404} with the contract's
 * message - never a validation error (§13.6).
 */
@Service
public class TaxService {

    private final TaxTypeRepository taxTypeRepository;

    public TaxService(TaxTypeRepository taxTypeRepository) {
        this.taxTypeRepository = taxTypeRepository;
    }

    /** §8.1 - all ten types in seed order, summary form, never paginated. */
    @Transactional(readOnly = true)
    public List<TaxResponse> list() {
        return taxTypeRepository.findAllByOrderByIdAsc().stream()
                .map(TaxResponse::summaryOf)
                .toList();
    }

    /** §8.2 - one type with its bracket table and worked example. */
    @Transactional(readOnly = true)
    public TaxResponse get(String code) {
        return TaxResponse.detailOf(requireType(code));
    }

    /** §8.3 - the bracket table only, in display order. */
    @Transactional(readOnly = true)
    public List<TaxBracketResponse> brackets(String code) {
        return requireType(code).getBrackets().stream()
                .map(TaxBracketResponse::of)
                .toList();
    }

    /** §8.4 - the worked example(s) only; today exactly one per type. */
    @Transactional(readOnly = true)
    public List<TaxExampleResponse> examples(String code) {
        return requireType(code).getExamples().stream()
                .map(TaxExampleResponse::of)
                .toList();
    }

    private TaxType requireType(String code) {
        return taxTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tax type with code '" + code + "'."));
    }
}

