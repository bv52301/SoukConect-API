package com.souk.product.api;

import com.souk.common.domain.Product;
import com.souk.common.domain.ProductLocation;
import com.souk.common.domain.VendorLocation;
import com.souk.common.domain.ProductMedia;
import com.souk.common.domain.ProductMedia.ValidationStatus;
import com.souk.common.domain.ProductMedia.StorageProvider;
import com.souk.common.domain.Vendor;
import com.souk.common.port.DataAccessPort;
import com.souk.product.api.dto.ProductCreateRequest;
import com.souk.product.api.dto.ProductResponse;
import com.souk.product.api.dto.ProductUpdateRequest;
import com.souk.common.port.ProductQueryPort;
import com.souk.product.util.ExcelTemplateGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
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
    private final DataAccessPort<VendorLocation, Long> vendorLocationPort;

    public ProductController(DataAccessPort<Product, Long> productPort,
                             ProductQueryPort productQueryPort,
                             DataAccessPort<ProductMedia, Long> mediaPort,
                             DataAccessPort<Vendor, Long> vendorPort,
                             DataAccessPort<VendorLocation, Long> vendorLocationPort) {
        this.productPort = productPort;
        this.productQueryPort = productQueryPort;
        this.mediaPort = mediaPort;
        this.vendorPort = vendorPort;
        this.vendorLocationPort = vendorLocationPort;
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

        // Handle location assignments
        if (req.locations() != null && !req.locations().isEmpty()) {
            List<ProductLocation> locationAssignments = new ArrayList<>();
            for (var loc : req.locations()) {
                VendorLocation vendorLocation = vendorLocationPort.findById(loc.vendorLocationId())
                        .orElseThrow(() -> new RuntimeException("Vendor location not found: " + loc.vendorLocationId()));

                ProductLocation assignment = new ProductLocation(
                        saved,
                        vendorLocation,
                        loc.sku(),
                        loc.available() != null ? loc.available() : true,
                        loc.stock() != null ? loc.stock() : 0
                );
                locationAssignments.add(assignment);
            }
            saved.setLocations(locationAssignments);
            saved = productPort.save(saved);
        }

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

                    // Handle location assignments update
                    if (req.locations() != null) {
                        // Clear existing locations
                        updated.getLocations().clear();

                        // Add new location assignments
                        List<ProductLocation> locationAssignments = new ArrayList<>();
                        for (var loc : req.locations()) {
                            VendorLocation vendorLocation = vendorLocationPort.findById(loc.vendorLocationId())
                                    .orElseThrow(() -> new RuntimeException("Vendor location not found: " + loc.vendorLocationId()));

                            ProductLocation assignment = new ProductLocation(
                                    updated,
                                    vendorLocation,
                                    loc.sku(),
                                    loc.available() != null ? loc.available() : true,
                                    loc.stock() != null ? loc.stock() : 0
                            );
                            locationAssignments.add(assignment);
                        }
                        updated.setLocations(locationAssignments);
                    }

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

    /** Download Excel template for bulk product upload with vendor dropdown */
    @GetMapping(value = "/bulk-upload-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadBulkUploadTemplate() {
        try {
            List<Vendor> vendors = vendorPort.findAll();
            byte[] excelBytes = ExcelTemplateGenerator.createProductBulkUploadTemplate(vendors);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product-bulk-upload-template.xlsx");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            headers.add("X-Content-Type-Options", "nosniff");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Bulk update products from Excel file */
    @PostMapping(value = "/bulk-update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.souk.product.api.dto.BulkUpdateResult> bulkUpdate(
            @RequestPart("file") MultipartFile file
    ) {
        try {
            List<com.souk.product.util.ExcelParser.ProductRow> rows =
                    com.souk.product.util.ExcelParser.parseExcel(file);

            List<com.souk.product.api.dto.BulkUpdateResult.RowResult> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < rows.size(); i++) {
                com.souk.product.util.ExcelParser.ProductRow row = rows.get(i);
                int rowNumber = i + 2; // Excel row (1-indexed + 1 for header)

                try {
                    // Find product by SKU
                    Optional<Product> productOpt = productQueryPort.findBySku(row.sku);
                    Product product;
                    boolean isNewProduct = productOpt.isEmpty();

                    if (isNewProduct) {
                        // Creating new product - vendorId is mandatory
                        if (row.vendorId == null) {
                            results.add(new com.souk.product.api.dto.BulkUpdateResult.RowResult(
                                    rowNumber, row.sku, false, "VendorID is required for new products"
                            ));
                            failureCount++;
                            continue;
                        }

                        // Verify vendor exists
                        Optional<Vendor> vendorOpt = vendorPort.findById(row.vendorId);
                        if (vendorOpt.isEmpty()) {
                            results.add(new com.souk.product.api.dto.BulkUpdateResult.RowResult(
                                    rowNumber, row.sku, false, "Vendor not found with ID: " + row.vendorId
                            ));
                            failureCount++;
                            continue;
                        }

                        product = new Product();
                        product.setSku(row.sku);
                        product.setVendorId(row.vendorId);
                    } else {
                        product = productOpt.get();

                        // Update vendorId if provided
                        if (row.vendorId != null) {
                            // Verify vendor exists
                            Optional<Vendor> vendorOpt = vendorPort.findById(row.vendorId);
                            if (vendorOpt.isEmpty()) {
                                results.add(new com.souk.product.api.dto.BulkUpdateResult.RowResult(
                                        rowNumber, row.sku, false, "Vendor not found with ID: " + row.vendorId
                                ));
                                failureCount++;
                                continue;
                            }
                            product.setVendorId(row.vendorId);
                        }
                    }

                    // Update fields if provided
                    if (row.name != null && !row.name.isEmpty()) {
                        product.setName(row.name);
                    }
                    if (row.price != null) {
                        product.setPrice(row.price);
                    }
                    if (row.description != null) {
                        product.setDescription(row.description);
                    }
                    if (row.available != null) {
                        product.setAvailable(row.available);
                    }
                    if (row.categoryDetails != null) {
                        product.setCategoryDetails(row.categoryDetails);
                    }
                    if (row.schedule != null) {
                        product.setSchedule(row.schedule);
                    }
                    if (row.useVendorSchedule != null) {
                        product.setUseVendorSchedule(row.useVendorSchedule);
                    }

                    // Update media if provided
                    if (row.mediaUrls != null && !row.mediaUrls.isEmpty()) {
                        // Clear existing media
                        if (product.getMedia() != null) {
                            product.getMedia().clear();
                        }

                        // Add new media
                        List<ProductMedia> newMedia = new ArrayList<>();
                        for (String url : row.mediaUrls) {
                            ProductMedia media = new ProductMedia();
                            media.setProduct(product);
                            media.setMediaUrl(url);
                            media.setMediaType(ProductMedia.MediaType.IMAGE); // Default to IMAGE
                            media.setStorageProvider(StorageProvider.LOCAL);
                            media.setValidationStatus(ValidationStatus.PENDING);
                            newMedia.add(media);
                        }
                        product.setMedia(newMedia);
                    }

                    // Save the product
                    productPort.save(product);

                    String action = isNewProduct ? "Created successfully" : "Updated successfully";
                    results.add(new com.souk.product.api.dto.BulkUpdateResult.RowResult(
                            rowNumber, row.sku, true, action
                    ));
                    successCount++;

                } catch (Exception e) {
                    results.add(new com.souk.product.api.dto.BulkUpdateResult.RowResult(
                            rowNumber, row.sku, false, "Error: " + e.getMessage()
                    ));
                    failureCount++;
                }
            }

            com.souk.product.api.dto.BulkUpdateResult result =
                    new com.souk.product.api.dto.BulkUpdateResult(
                            rows.size(), successCount, failureCount, results
                    );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new com.souk.product.api.dto.BulkUpdateResult(
                            0, 0, 0,
                            List.of(new com.souk.product.api.dto.BulkUpdateResult.RowResult(
                                    0, "", false, "Failed to parse Excel file: " + e.getMessage()
                            ))
                    )
            );
        }
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
                .orElseGet(() -> ResponseEntity.notFound().build());
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
