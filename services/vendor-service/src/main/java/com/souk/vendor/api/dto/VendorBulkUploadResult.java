package com.souk.vendor.api.dto;

import java.util.List;

/**
 * Result of a bulk vendor upload operation
 */
public record VendorBulkUploadResult(
        int totalRows,
        int successCount,
        int failureCount,
        List<RowResult> results
) {
    public record RowResult(
            int rowNumber,
            String vendorName,
            boolean success,
            String message,
            Long vendorId
    ) {}
}
