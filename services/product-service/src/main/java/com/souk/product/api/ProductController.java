package com.souk.product.api;

import com.souk.common.domain.Product;
import com.souk.common.domain.ProductMedia;
import com.souk.common.domain.ProductMedia.ValidationStatus;
import com.souk.common.domain.ProductMedia.StorageProvider;
import com.souk.common.port.DataAccessPort;
import com.souk.product.api.dto.ProductCreateRequest;
import com.souk.product.api.dto.ProductResponse;
import com.souk.product.api.dto.ProductUpdateRequest;
import com.souk.common.port.ProductQueryPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final DataAccessPort<Product, Long> productPort;
    private final ProductQueryPort productQueryPort;
    private final DataAccessPort<ProductMedia, Long> mediaPort;

    public ProductController(DataAccessPort<Product, Long> productPort,
                             ProductQueryPort productQueryPort,
                             DataAccessPort<ProductMedia, Long> mediaPort) {
        this.productPort = productPort;
        this.productQueryPort = productQueryPort;
        this.mediaPort = mediaPort;
    }

    // ------------------------------------------------------------
    // 🔹 PRODUCT CRUD ENDPOINTS
    // ------------------------------------------------------------

    /** Get all products */
    @GetMapping
    public List<ProductResponse> listAll() {
        return productPort.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    /** Get product by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable @Min(1) Long id) {
        return productPort.findById(id)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get product by SKU */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        return productQueryPort.findBySku(sku)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Create a new product */
    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductCreateRequest req) {
        Product toSave = req.toDomain();
        Product saved = productPort.save(toSave);
        return ResponseEntity
                .created(URI.create("/products/" + saved.getId()))
                .body(ProductResponse.from(saved));
    }

    /** Update an existing product */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable @Min(1) Long id,
                                                  @RequestBody @Valid ProductUpdateRequest req) {
        return productPort.findById(id)
                .map(existing -> {
                    Product updated = req.applyTo(existing);
                    Product saved = productPort.save(updated);
                    return ResponseEntity.ok(ProductResponse.from(saved));
                })
                .orElse(ResponseEntity.<ProductMedia>notFound().build());
    }

    /** Delete a product */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        return productPort.findById(id)
                .map(p -> {
                    productPort.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ------------------------------------------------------------
    // 🔹 PRODUCT MEDIA ENDPOINTS
    // ------------------------------------------------------------

    /** Upload media (image/video) metadata for a product */
    @PostMapping("/{productId}/media")
    public ResponseEntity<ProductMedia> uploadMedia(
            @PathVariable @Min(1) Long productId,
            @RequestBody @Valid ProductMedia uploadRequest
    ) {
        return productPort.findById(productId)
                .map(product -> {
                    ProductMedia media = new ProductMedia();
                    media.setProduct(product);
                    media.setMediaUrl(uploadRequest.getMediaUrl());
                    media.setDescription(uploadRequest.getDescription());
                    media.setMimeType(uploadRequest.getMimeType());
                    media.setWidth(uploadRequest.getWidth());
                    media.setHeight(uploadRequest.getHeight());
                    media.setSizeKb(uploadRequest.getSizeKb());
                    media.setDurationSeconds(uploadRequest.getDurationSeconds());
                    media.setResolution(uploadRequest.getResolution());

                    // auto-detect or fallback to LOCAL
                    media.setStorageProvider(uploadRequest.getStorageProvider() != null
                            ? uploadRequest.getStorageProvider()
                            : StorageProvider.LOCAL);

                    media.setValidationStatus(ValidationStatus.PENDING);
                    ProductMedia saved = mediaPort.save(media);

                    return ResponseEntity
                            .created(URI.create("/products/" + productId + "/media/" + saved.getId()))
                            .body(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** List all media for a product */
    @GetMapping("/{productId}/media")
    public ResponseEntity<List<ProductMedia>> listMedia(@PathVariable @Min(1) Long productId) {

        return productPort.findById(productId)
                .map(product -> ResponseEntity.ok(product.getMedia()))
                .orElseGet(()->ResponseEntity.<java.util.List<ProductMedia>>notFound().build());
    }

    /** Upload media binary (multipart) and create ProductMedia with a served URL */
    @PostMapping(value = "/{productId}/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductMedia> uploadMediaFile(
            @PathVariable @Min(1) Long productId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description
    ) {
        var opt = productPort.findById(productId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var product = opt.get();
        try {
            String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
            String safe = orig.replaceAll("[^a-zA-Z0-9._-]", "_");
            java.nio.file.Path base = java.nio.file.Paths.get(System.getProperty("user.home"), "souk-uploads", "products", String.valueOf(productId));
            java.nio.file.Files.createDirectories(base);
            java.nio.file.Path dest = base.resolve(System.currentTimeMillis() + "-" + safe);
            file.transferTo(dest.toFile());

            String mediaPath = "/uploads/products/" + productId + "/" + dest.getFileName();

            ProductMedia media = new ProductMedia();
            media.setProduct(product);
            media.setDescription(description);
            media.setMimeType(file.getContentType());
            media.setSizeKb((int) Math.max(1, file.getSize() / 1024));
            if (file.getContentType() != null && file.getContentType().startsWith("video")) {
                media.setMediaType(ProductMedia.MediaType.VIDEO);
            } else {
                media.setMediaType(ProductMedia.MediaType.IMAGE);
            }
            media.setValidationStatus(ProductMedia.ValidationStatus.PENDING);
            media.setStorageProvider(ProductMedia.StorageProvider.LOCAL);
            media.setMediaUrl(mediaPath);

            ProductMedia saved = mediaPort.save(media);
            return ResponseEntity
                    .created(URI.create("/products/" + productId + "/media/" + saved.getId()))
                    .body(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Delete a specific media item */
    @DeleteMapping("/{productId}/media/{mediaId}")
    public ResponseEntity<?> deleteMedia(
            @PathVariable @Min(1) Long productId,
            @PathVariable @Min(1) Long mediaId
    ) {
        return productPort.findById(productId)
                .map(product -> {
                    Optional<ProductMedia> target = product.getMedia().stream()
                            .filter(m -> m.getId().equals(mediaId))
                            .findFirst();

                    if (target.isPresent()) {
                        mediaPort.deleteById(mediaId);
                        return ResponseEntity.noContent().<Void>build();
                    }
                    return ResponseEntity.notFound().build();
                })
                .orElseGet(()->ResponseEntity.notFound().build());
    }
}
