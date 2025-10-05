package com.souk.product.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank String name,
        @NotNull  @DecimalMin("0.00") BigDecimal price,
        @NotBlank String sku,
        @NotNull  Long vendorId,
        @NotNull  Boolean available,
        JsonNode categoryDetails,
        JsonNode productImage,
        JsonNode schedule
) {
    /** Apply this update onto an existing domain object. */
    public Product applyTo(Product existing) {
        existing.setName(name);
        existing.setPrice(price);
        existing.setSku(sku);
        existing.setVendorId(vendorId);
        existing.setAvailable(available);
        existing.setCategoryDetails(categoryDetails);
        existing.setProductImage(productImage);
        existing.setSchedule(schedule);
        return existing;
    }
}
