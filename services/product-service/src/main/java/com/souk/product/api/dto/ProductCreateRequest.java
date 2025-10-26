package com.souk.product.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import com.souk.common.domain.ProductMedia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank String name,
        @NotBlank String sku,
        @NotNull BigDecimal price,
        @NotNull Long vendorId,
        Boolean available,
        JsonNode categoryDetails,
        JsonNode schedule,
        List<MediaRequest> media
) {
    public Product toDomain() {
        Product p = new Product();
        p.setName(name);
        p.setSku(sku);
        p.setPrice(price);
        p.setVendorId(vendorId);
        p.setAvailable(available != null ? available : Boolean.TRUE);
        p.setCategoryDetails(categoryDetails);
        p.setSchedule(schedule);

        // Attach media items if present
        if (media != null && !media.isEmpty()) {
            List<ProductMedia> mediaEntities = media.stream()
                    .map(MediaRequest::toDomain)
                    .toList();
            mediaEntities.forEach(m -> m.setProduct(p));
            p.setMedia(mediaEntities);
        }

        return p;
    }

    // Nested record for media items
    public record MediaRequest(
            @NotBlank String mediaUrl,
            String description,
            String mimeType,
            Integer width,
            Integer height,
            Integer sizeKb,
            Integer durationSeconds,
            String resolution,
            String mediaType,         // IMAGE or VIDEO
            String storageProvider     // LOCAL, S3, CLOUDFLARE, GCP
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

            if (mediaType != null) {
                try {
                    media.setMediaType(ProductMedia.MediaType.valueOf(mediaType.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    media.setMediaType(ProductMedia.MediaType.IMAGE); // default
                }
            } else {
                media.setMediaType(ProductMedia.MediaType.IMAGE);
            }

            if (storageProvider != null) {
                try {
                    media.setStorageProvider(ProductMedia.StorageProvider.valueOf(storageProvider.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    media.setStorageProvider(ProductMedia.StorageProvider.LOCAL);
                }
            } else {
                media.setStorageProvider(ProductMedia.StorageProvider.LOCAL);
            }

            media.setValidationStatus(ProductMedia.ValidationStatus.PENDING);
            return media;
        }
    }
}
