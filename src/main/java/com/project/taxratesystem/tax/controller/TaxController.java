package com.project.taxratesystem.tax.controller;

import com.project.taxratesystem.tax.dto.TaxBracketResponse;
import com.project.taxratesystem.tax.dto.TaxExampleResponse;
import com.project.taxratesystem.tax.dto.TaxResponse;
import com.project.taxratesystem.tax.service.TaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The public, read-only tax catalogue of API.md §8.
 *
 * <p>All four routes are permit-all in {@code SecurityConfig} (§12): the catalogue is reference
 * data, identical for every client. Unknown {@code code} values are {@code 404}s (§13.6).
 */
@RestController
@RequestMapping("/api/v1/taxes")
public class TaxController {

    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    /** §8.1 - the ten-type catalogue in summary form. */
    @GetMapping
    public ResponseEntity<List<TaxResponse>> list() {
        return ResponseEntity.ok(taxService.list());
    }

    /** §8.2 - one type with its bracket table and worked example. */
    @GetMapping("/{code}")
    public ResponseEntity<TaxResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(taxService.get(code));
    }

    /** §8.3 - bracket table only. */
    @GetMapping("/{code}/brackets")
    public ResponseEntity<List<TaxBracketResponse>> brackets(@PathVariable String code) {
        return ResponseEntity.ok(taxService.brackets(code));
    }

    /** §8.4 - worked example(s) only. */
    @GetMapping("/{code}/examples")
    public ResponseEntity<List<TaxExampleResponse>> examples(@PathVariable String code) {
        return ResponseEntity.ok(taxService.examples(code));
    }
}

