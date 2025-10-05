package com.souk.product.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank String name,
        @NotNull  @DecimalMin("0.00") BigDecimal price,
        @NotBlank String sku,
        @NotNull  Long vendorId,
        @NotNull  Boolean available,
        JsonNode categoryDetails,
        JsonNode productImage,
        JsonNode schedule
) {
    public Product toDomain() {
        var p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setSku(sku);
        p.setVendorId(vendorId);
        p.setAvailable(available);
        p.setCategoryDetails(categoryDetails);
        p.setProductImage(productImage);
        p.setSchedule(schedule);
        return p;
    }
}
