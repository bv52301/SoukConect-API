package com.souk.common.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // --- Core fields ---
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // NOTE: subtotal is a generated column in DB, not directly modifiable
    @Column(name = "subtotal", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal subtotal;

    // --- Per-item delivery preference ---
    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_flexibility", length = 20)
    private DeliveryFlexibility deliveryFlexibility = DeliveryFlexibility.FLEXIBLE;

    @Column(name = "delivery_slot_start")
    private LocalTime deliverySlotStart;

    @Column(name = "delivery_slot_end")
    private LocalTime deliverySlotEnd;

    // --- ENUM ---
    public enum DeliveryFlexibility {
        STRICT, FLEXIBLE
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }

    public LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public void setRequestedDeliveryDate(LocalDate requestedDeliveryDate) { this.requestedDeliveryDate = requestedDeliveryDate; }

    public DeliveryFlexibility getDeliveryFlexibility() { return deliveryFlexibility; }
    public void setDeliveryFlexibility(DeliveryFlexibility deliveryFlexibility) { this.deliveryFlexibility = deliveryFlexibility; }

    public LocalTime getDeliverySlotStart() { return deliverySlotStart; }
    public void setDeliverySlotStart(LocalTime deliverySlotStart) { this.deliverySlotStart = deliverySlotStart; }

    public LocalTime getDeliverySlotEnd() { return deliverySlotEnd; }
    public void setDeliverySlotEnd(LocalTime deliverySlotEnd) { this.deliverySlotEnd = deliverySlotEnd; }
}
