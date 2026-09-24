package com.project.taxratesystem.tax.repository;

import com.project.taxratesystem.tax.entity.TaxBracket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxBracketRepository extends JpaRepository<TaxBracket, Integer> {

    List<TaxBracket> findByTaxTypeIdOrderByDisplayOrderAsc(Integer taxTypeId);

    long countByTaxTypeId(Integer taxTypeId);
}

