package com.souk.product.api;

import com.souk.common.domain.Product;
import com.souk.common.port.DataAccessPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.souk.product.api.dto.ProductCreateRequest;
import com.souk.product.api.dto.ProductUpdateRequest;
import com.souk.product.api.dto.ProductResponse;


import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final DataAccessPort<Product, Long> productPort;

    public ProductController(DataAccessPort<Product, Long> productPort) {
        this.productPort = productPort;
    }

    // --- Read all (simple) ---
    @GetMapping
    public List<ProductResponse> listAll() {
        return productPort.findAll().stream().map(ProductResponse::from).toList();
    }

    // --- Read one by id ---
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable @Min(1) Long id) {
        return productPort.findById(id)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Read one by SKU (optional helper endpoint) ---
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        // If you exposed findBySku on the port, call that.
        // If not, you can temporarily fall back to filtering (not ideal for large datasets).
        Optional<Product> p = productPort.findAll().stream()
                .filter(prod -> sku.equals(prod.getSku()))
                .findFirst();
        return p.map(ProductResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Create ---
    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductCreateRequest req) {
        Product toSave = req.toDomain();
        Product saved = productPort.save(toSave);
        return ResponseEntity
                .created(URI.create("/products/" + saved.getId()))
                .body(ProductResponse.from(saved));
    }

    // --- Update (full replace) ---
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable @Min(1) Long id,
                                                  @RequestBody @Valid ProductUpdateRequest req) {
        return productPort.findById(id)
                .map(existing -> {
                    // map fields
                    existing.setName(req.name());
                    existing.setPrice(req.price());
                    existing.setSku(req.sku());
                    existing.setVendorId(req.vendorId());
                    existing.setAvailable(req.available());
                    existing.setCategoryDetails(req.categoryDetails());
                    existing.setProductImage(req.productImage());
                    existing.setSchedule(req.schedule());
                    Product saved = productPort.save(existing);
                    return ResponseEntity.ok(ProductResponse.from(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        return productPort.findById(id)
                .map(p -> {
                    productPort.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().<Void>build());
    }

}