package com.souk.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProductLocationId implements Serializable {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "vendor_location_id")
    private Long vendorLocationId;

    // Constructors
    public ProductLocationId() {}

    public ProductLocationId(Long productId, Long vendorLocationId) {
        this.productId = productId;
        this.vendorLocationId = vendorLocationId;
    }

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getVendorLocationId() { return vendorLocationId; }
    public void setVendorLocationId(Long vendorLocationId) { this.vendorLocationId = vendorLocationId; }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductLocationId that = (ProductLocationId) o;
        return Objects.equals(productId, that.productId) &&
               Objects.equals(vendorLocationId, that.vendorLocationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, vendorLocationId);
    }
}
