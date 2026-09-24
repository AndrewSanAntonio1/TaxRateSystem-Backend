package com.project.taxratesystem.calculation.enums;

/**
 * Persisted outcome of a calculation (API.md §9.1). Every calculation that is returned to the
 * caller is {@link #COMPLETED}; {@link #FAILED} is reserved for a future asynchronous flow.
 */
public enum CalculationStatus {
    COMPLETED,
    FAILED
}

