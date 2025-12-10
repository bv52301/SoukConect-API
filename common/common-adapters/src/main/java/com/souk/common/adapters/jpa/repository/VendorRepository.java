package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    // Native query leveraging MySQL JSON index: matches when category is a member of the JSON array
    @Query(value = "SELECT * FROM vendor_details WHERE JSON_CONTAINS(supportedCategories, JSON_QUOTE(:category))", nativeQuery = true)
    List<Vendor> findByCategory(@Param("category") String category);

    // Find vendors by category including products search - UNION of:
    // 1. Vendors with matching supportedCategories
    // 2. Vendors whose products have matching category or subcategory in category_details
    @Query(value = "SELECT DISTINCT v.* FROM vendor_details v WHERE " +
            "JSON_CONTAINS(v.supportedCategories, JSON_QUOTE(:category)) " +
            "UNION " +
            "SELECT DISTINCT v.* FROM vendor_details v " +
            "INNER JOIN products p ON v.vendor_id = p.vendor_id " +
            "WHERE JSON_CONTAINS(p.category_details, JSON_QUOTE(:category), '$.category') " +
            "OR JSON_CONTAINS(p.category_details, JSON_QUOTE(:category), '$.subcategory')",
            nativeQuery = true)
    List<Vendor> findByCategoryIncludingProducts(@Param("category") String category);
}
