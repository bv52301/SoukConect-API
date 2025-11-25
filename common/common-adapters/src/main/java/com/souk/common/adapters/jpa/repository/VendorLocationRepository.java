package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.VendorLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorLocationRepository extends JpaRepository<VendorLocation, Long> {
    List<VendorLocation> findByVendorId(Long vendorId);
    List<VendorLocation> findByVendorIdAndStatus(Long vendorId, VendorLocation.Status status);
}
