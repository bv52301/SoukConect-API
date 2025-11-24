package com.souk.product.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Product;
import com.souk.common.domain.ProductMedia;
import com.souk.common.domain.ProductMedia.ValidationStatus;
import com.souk.common.domain.ProductMedia.StorageProvider;
import com.souk.common.domain.Vendor;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final DataAccessPort<Product, Long> productPort;
    private final ProductQueryPort productQueryPort;
    private final DataAccessPort<ProductMedia, Long> mediaPort;
    private final DataAccessPort<Vendor, Long> vendorPort;

    public ProductController(DataAccessPort<Product, Long> productPort,
                             ProductQueryPort productQueryPort,
                             DataAccessPort<ProductMedia, Long> mediaPort,
                             DataAccessPort<Vendor, Long> vendorPort) {
        this.productPort = productPort;
        this.productQueryPort = productQueryPort;
        this.mediaPort = mediaPort;
        this.vendorPort = vendorPort;
    }

    // ------------------------------------------------------------
    // 🔹 PRODUCT CRUD ENDPOINTS
    // ------------------------------------------------------------

    /** Get all products */
    @GetMapping
    public List<ProductResponse> listAll() {
        List<Product> products = productPort.findAll();
        Map<Long, Vendor> vendorMap = buildVendorMap(products);
        return products.stream()
                .map(p -> toResponse(p, vendorMap))
                .toList();
    }

    /** Get product by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable @Min(1) Long id) {
        return productPort.findById(id)
                .map(p -> {
                    Map<Long, Vendor> vendorMap = buildVendorMap(List.of(p));
                    return ResponseEntity.ok(toResponse(p, vendorMap));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get product by SKU */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        return productQueryPort.findBySku(sku)
                .map(p -> {
                    Map<Long, Vendor> vendorMap = buildVendorMap(List.of(p));
                    return ResponseEntity.ok(toResponse(p, vendorMap));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get products by vendor id */
    @GetMapping("/vendor/{vendorId}")
    public List<ProductResponse> getByVendor(@PathVariable @Min(1) Long vendorId) {
        List<Product> products = productQueryPort.findByVendorId(vendorId);
        Map<Long, Vendor> vendorMap = buildVendorMap(products);
        return products.stream()
                .map(p -> toResponse(p, vendorMap))
                .toList();
    }

    /** Create a new product */
    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductCreateRequest req) {
        Product toSave = req.toDomain();
        Product saved = productPort.save(toSave);
        Map<Long, Vendor> vendorMap = buildVendorMap(List.of(saved));
        return ResponseEntity
                .created(URI.create("/products/" + saved.getId()))
                .body(toResponse(saved, vendorMap));
    }

    /** Update an existing product */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable @Min(1) Long id,
                                                  @RequestBody @Valid ProductUpdateRequest req) {
        return productPort.findById(id)
                .map(existing -> {
                    Product updated = req.applyTo(existing);
                    Product saved = productPort.save(updated);
                    Map<Long, Vendor> vendorMap = buildVendorMap(List.of(saved));
                    return ResponseEntity.ok(toResponse(saved, vendorMap));
                })
                .orElse(ResponseEntity.notFound().build());
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
    // 🔹 HELPER METHODS FOR VENDOR SCHEDULE MERGING
    // ------------------------------------------------------------

    /**
     * Build a map of vendor IDs to Vendor objects for all unique vendor IDs in the product list.
     * This allows efficient lookup when merging schedules.
     */
    private Map<Long, Vendor> buildVendorMap(List<Product> products) {
        List<Long> vendorIds = products.stream()
                .map(Product::getVendorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        return vendorIds.stream()
                .map(vendorPort::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Vendor::getVendorId, Function.identity()));
    }

    /**
     * Convert a Product to ProductResponse, merging vendor schedule if needed.
     */
    private ProductResponse toResponse(Product product, Map<Long, Vendor> vendorMap) {
        Vendor vendor = vendorMap.get(product.getVendorId());
        return ProductResponse.from(product, vendor != null ? vendor.getSchedule() : null);
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
        return mediaPort.findById(mediaId)
                .filter(pm -> pm.getProduct() != null && pm.getProduct().getId() != null && pm.getProduct().getId().equals(productId))
                .map(pm -> {
                    mediaPort.deleteById(mediaId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
