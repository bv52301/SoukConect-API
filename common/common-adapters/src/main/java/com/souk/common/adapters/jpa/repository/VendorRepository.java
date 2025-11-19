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
}
