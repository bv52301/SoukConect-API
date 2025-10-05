package com.souk.product.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.souk.common.domain.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String sku,
        Long vendorId,
        Boolean available,
        String categoryDetails,
        String productImage,
        String schedule,
        LocalDateTime createdAt,
        LocalDateTime scheduleUpdated
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getSku(),
                p.getVendorId(),
                p.getAvailable(),
                p.getCategoryDetails(),
                p.getProductImage(),
                p.getSchedule(),
                p.getCreatedAt(),
                p.getScheduleUpdated()
        );
    }
}
