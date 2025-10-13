package com.souk.product.api.dto;

import com.souk.common.domain.Product;
import com.souk.common.domain.ProductImage;
import com.fasterxml.jackson.annotation.JsonInclude;

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
        List<ImageResponse> images
) {
    public static ProductResponse from(Product product) {
        List<ImageResponse> imageResponses = null;

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageResponses = product.getImages().stream()
                    .map(ImageResponse::from)
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
                imageResponses
        );
    }

    public record ImageResponse(
            Long id,
            String url,
            String mimeType,
            Integer width,
            Integer height,
            Integer sizeKb,
            String storageProvider,
            String validationStatus
    ) {
        public static ImageResponse from(ProductImage img) {
            return new ImageResponse(
                    img.getId(),
                    img.getImageUrl(),
                    img.getMimeType(),
                    img.getWidth(),
                    img.getHeight(),
                    img.getSizeKb(),
                    img.getStorageProvider() != null ? img.getStorageProvider().name() : null,
                    img.getValidationStatus() != null ? img.getValidationStatus().name() : null
            );
        }
    }
}
