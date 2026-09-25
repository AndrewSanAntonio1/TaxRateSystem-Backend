package com.project.taxratesystem.calculation.controller;

import com.project.taxratesystem.calculation.dto.CalculationRequest;
import com.project.taxratesystem.calculation.dto.CalculationResponse;
import com.project.taxratesystem.calculation.service.CalculationService;
import com.project.taxratesystem.common.exception.ValidationException;
import com.project.taxratesystem.common.response.PageResponse;
import com.project.taxratesystem.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The calculator and history routes of API.md §9/§10. Every route requires a bearer token and is
 * scoped to the caller; query-parameter failures are {@code 400}s (§13.6), body failures
 * {@code 422}s (§5).
 */

@RestController
@RequestMapping("/api/v1/calculations")
public class CalculationController {

    private final CalculationService calculationService;

    public CalculationController(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    /** §9.1 - compute, persist and return; the saved row is the history entry. */
    @PostMapping
    public ResponseEntity<CalculationResponse> calculate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CalculationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calculationService.calculate(principal.id(), request));
    }

    /** §10.1 - the caller's history, newest first by default. */
    @GetMapping("/history")
    public ResponseEntity<PageResponse<CalculationResponse>> history(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String taxType) {
        if (page < 0) {
            throw invalid("page", "must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw invalid("size", "must be between 1 and 100");
        }
        return ResponseEntity.ok(
                calculationService.history(principal.id(), page, size, sort, taxType));
    }

    /** §10.2 - one owned calculation with its full breakdown (foreign ids are 404). */
    @GetMapping("/{id}")
    public ResponseEntity<CalculationResponse> get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Integer id) {
        return ResponseEntity.ok(calculationService.get(principal.id(), id));
    }

    /** §10.3 - idempotent soft delete of one history entry. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Integer id) {
        calculationService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    /** §10.4 - clear the caller's whole history. */
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(@AuthenticationPrincipal AuthenticatedUser principal) {
        calculationService.clearHistory(principal.id());
        return ResponseEntity.noContent().build();
    }

    private static ValidationException invalid(String field, String message) {
        return new ValidationException(HttpStatus.BAD_REQUEST, "Invalid request", Map.of(field, message));
    }
}
