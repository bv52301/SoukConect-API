package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.Cuisine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CuisineRepository extends JpaRepository<Cuisine, Long> {

    // Get distinct categories (non-null, non-empty, trimmed)
    @Query(value = "SELECT DISTINCT TRIM(category) FROM cuisines WHERE category IS NOT NULL AND TRIM(category) != '' ORDER BY TRIM(category)", nativeQuery = true)
    List<String> findDistinctCategories();

    // Get distinct subcategories (non-null, non-empty, trimmed)
    @Query(value = "SELECT DISTINCT TRIM(subcategory) FROM cuisines WHERE subcategory IS NOT NULL AND TRIM(subcategory) != '' ORDER BY TRIM(subcategory)", nativeQuery = true)
    List<String> findDistinctSubcategories();
}
