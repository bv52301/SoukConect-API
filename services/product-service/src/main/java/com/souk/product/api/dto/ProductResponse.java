package com.souk.product.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
                product.getCategoryDetails(),
                product.getSchedule(),
                mediaResponses
        );
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
