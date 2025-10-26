package com.souk.common.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_media",
        indexes = {
                @Index(name = "idx_media_product", columnList = "product_id"),
                @Index(name = "idx_media_status", columnList = "validation_status"),
                @Index(name = "idx_media_uploaded", columnList = "uploaded_at")
        }
)
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType = MediaType.IMAGE; // Default to IMAGE

    @Column(name = "media_url", nullable = false, length = 1000)
    private String mediaUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private StorageProvider storageProvider = StorageProvider.LOCAL; // Default

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "size_kb")
    private Integer sizeKb;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "resolution", length = 50)
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 20)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Column(name = "validation_error", columnDefinition = "TEXT")
    private String validationError;

    @Column(name = "uploaded_at", updatable = false, insertable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // ===== ENUMS =====
    public enum StorageProvider {
        LOCAL, S3, CLOUDFLARE, GCP
    }

    public enum ValidationStatus {
        PENDING, VALIDATED, REJECTED
    }

    public enum MediaType {
        IMAGE, VIDEO
    }

    // ===== AUTO-DETECTION LOGIC =====
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        this.storageProvider = detectStorageProvider(mediaUrl);
    }

    private StorageProvider detectStorageProvider(String mediaUrl) {
        if (mediaUrl == null) return StorageProvider.LOCAL;
        String lower = mediaUrl.toLowerCase();

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

    // ===== GETTERS & SETTERS =====
    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }

    public String getMediaUrl() { return mediaUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getValidationError() { return validationError; }
    public void setValidationError(String validationError) { this.validationError = validationError; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
