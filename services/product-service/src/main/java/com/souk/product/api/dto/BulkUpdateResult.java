package com.souk.product.api.dto;

import java.util.List;

/**
 * Result of a bulk update operation
 */
public record BulkUpdateResult(
        int totalRows,
        int successCount,
        int failureCount,
        List<RowResult> results
) {
    public record RowResult(
            int rowNumber,
            String sku,
            boolean success,
            String message
    ) {}
}
