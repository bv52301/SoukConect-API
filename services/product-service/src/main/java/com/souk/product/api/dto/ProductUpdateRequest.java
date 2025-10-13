package com.souk.product.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import com.souk.common.domain.ProductImage;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductUpdateRequest(

        String name,
        String sku,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
        BigDecimal price,

        Long vendorId,
        Boolean available,

        JsonNode categoryDetails,
        JsonNode schedule,

        List<ImageRequest> images
) {

    /**
     * Applies the fields in this update request to an existing Product entity.
     */
    public void applyTo(Product product) {
        if (name != null) product.setName(name);
        if (sku != null) product.setSku(sku);
        if (price != null) product.setPrice(price);
        if (vendorId != null) product.setVendorId(vendorId);
        if (available != null) product.setAvailable(available);
        if (categoryDetails != null) product.setCategoryDetails(categoryDetails);
        if (schedule != null) product.setSchedule(schedule);

        if (images != null && !images.isEmpty()) {
            product.setImages(
                    images.stream()
                            .map(ImageRequest::toDomain)
                            .collect(Collectors.toList())
            );
        }
    }

    /**
     * Nested record for image updates (similar to ProductCreateRequest.ImageRequest)
     */
    public record ImageRequest(
            Long id, // optional — if present, can be used to update an existing image
            String url,
            String mimeType,
            Integer width,
            Integer height,
            Integer sizeKb,
            String storageProvider,
            String validationStatus
    ) {
        public ProductImage toDomain() {
            ProductImage img = new ProductImage();
            img.setImageUrl(url);
            img.setMimeType(mimeType);
            img.setWidth(width);
            img.setHeight(height);
            img.setSizeKb(sizeKb);

            if (storageProvider != null) {
                try {
                    img.setStorageProvider(
                            ProductImage.StorageProvider.valueOf(storageProvider.toUpperCase())
                    );
                } catch (IllegalArgumentException ignored) {
                    img.setStorageProvider(ProductImage.StorageProvider.LOCAL);
                }
            }

            if (validationStatus != null) {
                try {
                    img.setValidationStatus(
                            ProductImage.ValidationStatus.valueOf(validationStatus.toUpperCase())
                    );
                } catch (IllegalArgumentException ignored) {
                    img.setValidationStatus(ProductImage.ValidationStatus.PENDING);
                }
            }

            return img;
        }
    }
}
