package com.souk.product.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Update request for products. All fields are optional; when present they overwrite the existing value.
 * Categories are normalized to an array shape for consistency with creation.
 */
public record ProductUpdateRequest(
        String name,
        String sku,
        BigDecimal price,
        Long vendorId,
        Boolean available,
        JsonNode categoryDetails,   // expected: array of objects; single object will be wrapped
        JsonNode schedule
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Product applyTo(Product existing) {
        if (name != null) existing.setName(name);
        if (sku != null) existing.setSku(sku);
        if (price != null) existing.setPrice(price);
        if (vendorId != null) existing.setVendorId(vendorId);
        if (available != null) existing.setAvailable(available);
        if (categoryDetails != null) {
            existing.setCategoryDetails(normalizeCategoryPayload(categoryDetails));
        }
        if (schedule != null) existing.setSchedule(schedule);
        return existing;
    }

    private static JsonNode normalizeCategoryPayload(JsonNode input) {
        if (input == null) return null;
        if (input.isArray()) return input;
        return MAPPER.createArrayNode().add(input);
    }
}
