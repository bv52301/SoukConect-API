package com.souk.product.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import com.souk.common.domain.ProductMedia;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ProductUpdateRequest(
        @NotNull Long id,
        String name,
        String sku,
        BigDecimal price,
        Long vendorId,
        Boolean available,
        JsonNode categoryDetails,
        JsonNode schedule,
        List<MediaRequest> media
) {
    public Product applyTo(Product existing) {
        if (name != null) existing.setName(name);
        if (sku != null) existing.setSku(sku);
        if (price != null) existing.setPrice(price);
        if (vendorId != null) existing.setVendorId(vendorId);
        if (available != null) existing.setAvailable(available);
        if (categoryDetails != null) existing.setCategoryDetails(categoryDetails);
        if (schedule != null) existing.setSchedule(schedule);

        // Replace media list if provided
        if (media != null && !media.isEmpty()) {
            List<ProductMedia> mediaEntities = media.stream()
                    .map(MediaRequest::toDomain)
                    .toList();
            mediaEntities.forEach(m -> m.setProduct(existing));
            existing.setMedia(mediaEntities);
        }

        return existing;
    }

    public record MediaRequest(
            String mediaUrl,
            String description,
            String mimeType,
            Integer width,
            Integer height,
            Integer sizeKb,
            Integer durationSeconds,
            String resolution,
            String mediaType,        // IMAGE or VIDEO
            String storageProvider,  // LOCAL, S3, CLOUDFLARE, GCP
            String validationStatus  // PENDING, VALIDATED, REJECTED
    ) {
        public ProductMedia toDomain() {
            ProductMedia media = new ProductMedia();

            media.setMediaUrl(mediaUrl);
            media.setDescription(description);
            media.setMimeType(mimeType);
            media.setWidth(width);
            media.setHeight(height);
            media.setSizeKb(sizeKb);
            media.setDurationSeconds(durationSeconds);
            media.setResolution(resolution);

            // Parse enums safely
            if (mediaType != null) {
                try {
                    media.setMediaType(ProductMedia.MediaType.valueOf(mediaType.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    media.setMediaType(ProductMedia.MediaType.IMAGE);
                }
            }

            if (storageProvider != null) {
                try {
                    media.setStorageProvider(ProductMedia.StorageProvider.valueOf(storageProvider.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    media.setStorageProvider(ProductMedia.StorageProvider.LOCAL);
                }
            }

            if (validationStatus != null) {
                try {
                    media.setValidationStatus(ProductMedia.ValidationStatus.valueOf(validationStatus.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    media.setValidationStatus(ProductMedia.ValidationStatus.PENDING);
                }
            }

            return media;
        }
    }
}