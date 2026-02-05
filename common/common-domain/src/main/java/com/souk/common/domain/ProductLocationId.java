package com.souk.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProductLocationId implements Serializable {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "address_id")
    private Long addressId;

    // Constructors
    public ProductLocationId() {}

    public ProductLocationId(Long productId, Long addressId) {
        this.productId = productId;
        this.addressId = addressId;
    }

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductLocationId that = (ProductLocationId) o;
        return Objects.equals(productId, that.productId) &&
               Objects.equals(addressId, that.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, addressId);
    }
}
