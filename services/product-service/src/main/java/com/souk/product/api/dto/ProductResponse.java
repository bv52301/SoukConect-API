package com.souk.product.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
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
        List<String> categories,
        Object categoryDetails,
        Object schedule,
        List<MediaResponse> media
) {
    public static ProductResponse from(Product product) {
        List<MediaResponse> mediaResponses = null;

        if (product.getMedia() != null && !product.getMedia().isEmpty()) {
            mediaResponses = product.getMedia().stream()
                    .map(MediaResponse::from)
                    .toList();
        }

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getVendorId(),
                product.getAvailable(),
                extractCategories(product.getCategoryDetails()),
                product.getCategoryDetails(),
                product.getSchedule(),
                mediaResponses
        );
    }

    @SuppressWarnings("unchecked")
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
}
