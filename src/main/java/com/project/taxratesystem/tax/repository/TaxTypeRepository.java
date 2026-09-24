package com.project.taxratesystem.tax.repository;

import com.project.taxratesystem.tax.entity.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxTypeRepository extends JpaRepository<TaxType, Integer> {

    Optional<TaxType> findByCode(String code);

    boolean existsByCode(String code);

    /** The catalogue of §8.1 is returned in seed order - exactly ten rows, never paginated. */
    List<TaxType> findAllByOrderByIdAsc();
}

