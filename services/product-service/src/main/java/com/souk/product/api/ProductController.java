package com.souk.product.api;

import com.souk.common.domain.Product;
import com.souk.common.domain.ProductImage;
import com.souk.common.domain.ProductImage.ValidationStatus;
import com.souk.common.domain.ProductImage.StorageProvider;
import com.souk.common.port.DataAccessPort;
import com.souk.product.api.dto.ProductCreateRequest;
import com.souk.product.api.dto.ProductResponse;
import com.souk.product.api.dto.ProductUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final DataAccessPort<Product, Long> productPort;
    private final DataAccessPort<ProductImage, Long> imagePort;

    public ProductController(DataAccessPort<Product, Long> productPort,
                             DataAccessPort<ProductImage, Long> imagePort) {
        this.productPort = productPort;
        this.imagePort = imagePort;
    }

    // --- Get all products ---
    @GetMapping
    public List<ProductResponse> listAll() {
        return productPort.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    // --- Get product by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable @Min(1) Long id) {
        return productPort.findById(id)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Get product by SKU ---
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        Optional<Product> p = productPort.findAll().stream()
                .filter(prod -> sku.equals(prod.getSku()))
                .findFirst();
        return p.map(ProductResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Create new product ---
    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductCreateRequest req) {
        Product toSave = req.toDomain();
        Product saved = productPort.save(toSave);
        return ResponseEntity
                .created(URI.create("/products/" + saved.getId()))
                .body(ProductResponse.from(saved));
    }

    // --- Update existing product ---
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable @Min(1) Long id,
                                                  @RequestBody @Valid ProductUpdateRequest req) {
        return productPort.findById(id)
                .map(existing -> {
                    existing.setName(req.name());
                    existing.setPrice(req.price());
                    existing.setSku(req.sku());
                    existing.setVendorId(req.vendorId());
                    existing.setAvailable(req.available());
                    existing.setCategoryDetails(req.categoryDetails());
                    existing.setSchedule(req.schedule());
                    Product saved = productPort.save(existing);
                    return ResponseEntity.ok(ProductResponse.from(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Delete product ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        return productPort.findById(id)
                .map(p -> {
                    productPort.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().<Void>build());
    }

    // --- Upload image(s) for a product ---
    @PostMapping("/{productId}/images")
    public ResponseEntity<ProductImage> uploadImage(
            @PathVariable @Min(1) Long productId,
            @RequestBody ProductImage uploadRequest
    ) {
        return productPort.findById(productId)
                .map(product -> {
                    ProductImage img = new ProductImage();
                    img.setProduct(product);
                    img.setImageUrl(uploadRequest.getImageUrl());
                    img.setMimeType(uploadRequest.getMimeType());
                    img.setWidth(uploadRequest.getWidth());
                    img.setHeight(uploadRequest.getHeight());
                    img.setSizeKb(uploadRequest.getSizeKb());
                    img.setStorageProvider(uploadRequest.getStorageProvider() != null
                            ? uploadRequest.getStorageProvider()
                            : StorageProvider.LOCAL);
                    img.setValidationStatus(ValidationStatus.PENDING);

                    ProductImage savedImage = imagePort.save(img);
                    return ResponseEntity
                            .created(URI.create("/products/" + productId + "/images/" + savedImage.getId()))
                            .body(savedImage);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Get all images for a product ---
    @GetMapping("/{productId}/images")
    public ResponseEntity<List<ProductImage>> listImages(@PathVariable @Min(1) Long productId) {
        return productPort.findById(productId)
                .map(product -> {
                    List<ProductImage> images = product.getImages();
                    return ResponseEntity.ok(images);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
