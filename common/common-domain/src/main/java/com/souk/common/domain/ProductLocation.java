package com.souk.common.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_locations")
public class ProductLocation {

    @EmbeddedId
    private ProductLocationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("vendorLocationId")
    @JoinColumn(name = "vendor_location_id", nullable = false)
    private VendorLocation vendorLocation;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Boolean available = true;

    @Column
    private Integer stock = 0;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public ProductLocation() {}

    public ProductLocation(Product product, VendorLocation vendorLocation, String sku, Boolean available, Integer stock) {
        this.id = new ProductLocationId(product.getId(), vendorLocation.getId());
        this.product = product;
        this.vendorLocation = vendorLocation;
        this.sku = sku;
        this.available = available;
        this.stock = stock;
    }

    // Getters and Setters
    public ProductLocationId getId() { return id; }
    public void setId(ProductLocationId id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public VendorLocation getVendorLocation() { return vendorLocation; }
    public void setVendorLocation(VendorLocation vendorLocation) { this.vendorLocation = vendorLocation; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
