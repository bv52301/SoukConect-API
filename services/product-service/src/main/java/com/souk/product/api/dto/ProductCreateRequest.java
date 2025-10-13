package com.souk.product.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import com.souk.common.domain.ProductImage;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCreateRequest(

        @NotBlank String name,
        @NotBlank String sku,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull Long vendorId,
        @NotNull Boolean available,

        JsonNode categoryDetails,
        JsonNode schedule,

        @Size(min = 1, message = "At least one image must be provided")
        List<ImageRequest> images

) {
    public Product toDomain() {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setPrice(price);
        product.setVendorId(vendorId);
        product.setAvailable(available);
        product.setCategoryDetails(categoryDetails);
        product.setSchedule(schedule);

        if (images != null && !images.isEmpty()) {
            product.setImages(
                    images.stream()
                            .map(ImageRequest::toDomain)
                            .collect(Collectors.toList())
            );
        }

        return product;
    }

    // ✅ Nested Image DTO — aligned with product_images schema
    public record ImageRequest(
            @NotBlank String url,               // corresponds to image_url
            String mimeType,                    // mime_type
            Integer width,                      // width
            Integer height,                     // height
            Integer sizeKb,                     // size_kb
            String storageProvider,             // storage_provider
            String validationStatus             // validation_status (optional)
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
            } else {
                img.setValidationStatus(ProductImage.ValidationStatus.PENDING);
            }

            return img;
        }
    }
}
