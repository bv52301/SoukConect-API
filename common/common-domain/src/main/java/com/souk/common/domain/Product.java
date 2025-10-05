package com.souk.common.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;





@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "is_available", nullable = false)
    private Boolean available;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_details", columnDefinition = "json")

    private JsonNode categoryDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_image", columnDefinition = "json")
    private JsonNode productImage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schedule", columnDefinition = "json")
    private JsonNode schedule;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "schedule_updated", insertable = false, updatable = false)
    private LocalDateTime scheduleUpdated;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public JsonNode getCategoryDetails() { return categoryDetails; }
    public void setCategoryDetails(JsonNode categoryDetails) { this.categoryDetails = categoryDetails; }

    public JsonNode getProductImage() { return productImage; }
    public void setProductImage(JsonNode productImage) { this.productImage = productImage; }

    public JsonNode getSchedule() { return schedule; }
    public void setSchedule(JsonNode schedule) { this.schedule = schedule; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getScheduleUpdated() { return scheduleUpdated; }
}
