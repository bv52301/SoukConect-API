package com.souk.common.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_images",
        indexes = {
                @Index(name = "idx_product_id", columnList = "product_id"),
                @Index(name = "idx_validation_status", columnList = "validation_status")
        })
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private StorageProvider storageProvider = StorageProvider.LOCAL; // default

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "size_kb")
    private Integer sizeKb;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 20)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Column(name = "uploaded_at", updatable = false, insertable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // --- ENUMS ---
    public enum StorageProvider {
        LOCAL, S3, CLOUDFLARE, GCP
    }

    public enum ValidationStatus {
        PENDING, VALIDATED, REJECTED
    }

    // --- AUTO-DETECTION LOGIC ---
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.storageProvider = detectStorageProvider(imageUrl);
    }

    // --- PRIVATE HELPER ---
    private StorageProvider detectStorageProvider(String imageUrl) {
        if (imageUrl == null) return StorageProvider.LOCAL;
        String lower = imageUrl.toLowerCase();

        if (lower.contains("s3.amazonaws.com") || lower.contains(".s3.")) {
            return StorageProvider.S3;
        } else if (lower.contains("storage.googleapis.com")) {
            return StorageProvider.GCP;
        } else if (lower.contains("imagedelivery.net") || lower.contains("r2.cloudflarestorage.com")) {
            return StorageProvider.CLOUDFLARE;
        } else if (lower.startsWith("/") || lower.startsWith("file:") || lower.contains("localhost")) {
            return StorageProvider.LOCAL;
        }
        return StorageProvider.LOCAL; // fallback
    }

    // --- GETTERS & SETTERS ---
    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getImageUrl() { return imageUrl; }

    public StorageProvider getStorageProvider() { return storageProvider; }
    public void setStorageProvider(StorageProvider storageProvider) { this.storageProvider = storageProvider; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Integer getSizeKb() { return sizeKb; }
    public void setSizeKb(Integer sizeKb) { this.sizeKb = sizeKb; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
