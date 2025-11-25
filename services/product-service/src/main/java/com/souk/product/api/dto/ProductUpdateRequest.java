package com.souk.product.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Update request for products. All fields are optional; when present they overwrite the existing value.
 * Categories are normalized to an array shape for consistency with creation.
 * Location assignments are managed separately via the ProductController.
 */
public record ProductUpdateRequest(
        String name,
        String sku,
        BigDecimal price,
        Long vendorId,
        Boolean available,
        String description,
        JsonNode categoryDetails,   // expected: array of objects; single object will be wrapped
        JsonNode schedule,
        Boolean useVendorSchedule,
        List<LocationAssignment> locations
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Product applyTo(Product existing) {
        if (name != null) existing.setName(name);
        if (sku != null) existing.setSku(sku);
        if (price != null) existing.setPrice(price);
        if (vendorId != null) existing.setVendorId(vendorId);
        if (available != null) existing.setAvailable(available);
        if (description != null) existing.setDescription(description);
        if (categoryDetails != null) {
            existing.setCategoryDetails(normalizeCategoryPayload(categoryDetails));
        }
        if (schedule != null) existing.setSchedule(schedule);
        if (useVendorSchedule != null) existing.setUseVendorSchedule(useVendorSchedule);
        // Note: locations are handled separately in the controller
        return existing;
    }

    private static JsonNode normalizeCategoryPayload(JsonNode input) {
        if (input == null) return null;
        if (input.isArray()) return input;
        return MAPPER.createArrayNode().add(input);
    }

    // Nested record for location assignments
    public record LocationAssignment(
            @NotNull Long vendorLocationId,
            @NotBlank String sku,
            Boolean available,
            Integer stock
    ) {}
}
