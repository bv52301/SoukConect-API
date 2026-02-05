package com.souk.product.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import com.souk.common.domain.ProductLocation;
import com.souk.common.domain.ProductMedia;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        Long vendorId,
        Boolean available,
        String description,
        List<String> categories,
        Object categoryDetails,
        Object schedule,
        Boolean useVendorSchedule,
        List<LocationAssignmentResponse> locations,
        List<MediaResponse> media
) {
    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, JsonNode vendorSchedule) {
        List<MediaResponse> mediaResponses = null;

        if (product.getMedia() != null && !product.getMedia().isEmpty()) {
            mediaResponses = product.getMedia().stream()
                    .map(MediaResponse::from)
                    .toList();
        }

        List<LocationAssignmentResponse> locationResponses = null;
        if (product.getLocations() != null && !product.getLocations().isEmpty()) {
            locationResponses = product.getLocations().stream()
                    .map(LocationAssignmentResponse::from)
                    .toList();
        }

        // If product uses vendor schedule and vendor schedule is provided, use it; otherwise use product's own schedule
        JsonNode effectiveSchedule = (product.getUseVendorSchedule() != null && product.getUseVendorSchedule() && vendorSchedule != null)
                ? vendorSchedule
                : product.getSchedule();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getVendorId(),
                product.getAvailable(),
                product.getDescription(),
                extractCategories(product.getCategoryDetails()),
                product.getCategoryDetails(),
                effectiveSchedule,
                product.getUseVendorSchedule(),
                locationResponses,
                mediaResponses
        );
    }

    private static List<String> extractCategories(Object categoryDetails) {
        if (categoryDetails == null) return null;
        // If stored as array of objects, take first element's Category array
        if (categoryDetails instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof java.util.Map<?,?> map) {
                Object cat = map.get("Category");
                if (cat instanceof List<?> clist) {
                    return clist.stream().map(Object::toString).toList();
                }
            }
        }
        // Fallback: single object
        if (categoryDetails instanceof java.util.Map<?,?> map) {
            Object cat = map.get("Category");
            if (cat instanceof List<?> clist) {
                return clist.stream().map(Object::toString).toList();
            }
        }
        return null;
    }

    public record MediaResponse(
            Long id,
            String mediaType,
            String url,
            String description,
            String mimeType,
            Integer width,
            Integer height,
            Integer sizeKb,
            Integer durationSeconds,
            String resolution,
            String storageProvider,
            String validationStatus,
            String validationError
    ) {
        public static MediaResponse from(ProductMedia media) {
            return new MediaResponse(
                    media.getId(),
                    media.getMediaType() != null ? media.getMediaType().name() : null,
                    media.getMediaUrl(),
                    media.getDescription(),
                    media.getMimeType(),
                    media.getWidth(),
                    media.getHeight(),
                    media.getSizeKb(),
                    media.getDurationSeconds(),
                    media.getResolution(),
                    media.getStorageProvider() != null ? media.getStorageProvider().name() : null,
                    media.getValidationStatus() != null ? media.getValidationStatus().name() : null,
                    media.getValidationError()
            );
        }
    }

    public record LocationAssignmentResponse(
            Long vendorLocationId,
            String locationName,
            String sku,
            Boolean available,
            Integer stock
    ) {
        public static LocationAssignmentResponse from(ProductLocation location) {
            return new LocationAssignmentResponse(
                    location.getAddress().getId(),
                    location.getAddress().getLabel(),
                    location.getSku(),
                    location.getAvailable(),
                    location.getStock()
            );
        }
    }
}
