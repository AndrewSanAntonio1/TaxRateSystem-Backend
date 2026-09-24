package com.project.taxratesystem.calculation.repository;

import com.project.taxratesystem.calculation.entity.Calculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * History access of API.md §10. Every query is scoped by {@code userId} - a user can only ever see
 * or delete their own rows, and "not owned" is reported as "not found".
 *
 * <p>Deletes are soft ({@code deletedAt}) so that repeating a delete of an id that once belonged to
 * the caller is still {@code 204} (§10.3).
 */
@Repository
public interface CalculationRepository extends JpaRepository<Calculation, Integer> {

    Page<Calculation> findByUserIdAndDeletedAtIsNull(Integer userId, Pageable pageable);

    /** Optional filter by tax-type code; an unknown code simply yields an empty page (§10.1). */
    Page<Calculation> findByUserIdAndTaxTypeCodeAndDeletedAtIsNull(Integer userId, String taxTypeCode,
                                                                    Pageable pageable);

    Optional<Calculation> findByIdAndUserIdAndDeletedAtIsNull(Integer id, Integer userId);

    /** Includes soft-deleted rows, so a repeat delete can still be attributed to its owner. */
    Optional<Calculation> findByIdAndUserId(Integer id, Integer userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Calculation calculation set calculation.deletedAt = :when "
            + "where calculation.user.id = :userId and calculation.deletedAt is null")
    int softDeleteAllForUser(@Param("userId") Integer userId, @Param("when") Instant when);

    long countByUserIdAndDeletedAtIsNull(Integer userId);
}

