package com.project.taxratesystem.tax.repository;

import com.project.taxratesystem.tax.entity.TaxExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxExampleRepository extends JpaRepository<TaxExample, Integer> {

    List<TaxExample> findByTaxTypeIdOrderByDisplayOrderAsc(Integer taxTypeId);

    boolean existsByTaxTypeId(Integer taxTypeId);
}

