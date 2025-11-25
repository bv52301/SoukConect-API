package com.souk.common.port;

import com.souk.common.domain.VendorLocation;
import java.util.List;

/**
 * Domain-specific query port for VendorLocation lookups
 */
public interface VendorLocationQueryPort {
    List<VendorLocation> findByVendorId(Long vendorId);
    List<VendorLocation> findByVendorIdAndStatus(Long vendorId, VendorLocation.Status status);
}
