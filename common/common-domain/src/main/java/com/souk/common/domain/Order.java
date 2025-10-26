package com.souk.common.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer", columnList = "customer_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private CustomerAddress address;

    // --- Financial fields ---
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // --- Enums for status and payment ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.CARD;

    // --- Delivery preferences ---
    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_flexibility", length = 20)
    private DeliveryFlexibility deliveryFlexibility = DeliveryFlexibility.FLEXIBLE;

    @Column(name = "delivery_slot_start")
    private LocalTime deliverySlotStart;

    @Column(name = "delivery_slot_end")
    private LocalTime deliverySlotEnd;

    // --- Notes ---
    @Column(columnDefinition = "TEXT")
    private String notes;

    // --- Audit info ---
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // --- Relationship to order items ---
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    // --- ENUMS ---
    public enum OrderStatus {
        PENDING, CONFIRMED, PAID, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }

    public enum PaymentMethod {
        CASH, CARD, WALLET, BANK_TRANSFER, PAYNOW, OTHERS
    }

    public enum DeliveryFlexibility {
        STRICT, FLEXIBLE
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public CustomerAddress getAddress() { return address; }
    public void setAddress(CustomerAddress address) { this.address = address; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public void setRequestedDeliveryDate(LocalDate requestedDeliveryDate) { this.requestedDeliveryDate = requestedDeliveryDate; }

    public DeliveryFlexibility getDeliveryFlexibility() { return deliveryFlexibility; }
    public void setDeliveryFlexibility(DeliveryFlexibility deliveryFlexibility) { this.deliveryFlexibility = deliveryFlexibility; }

    public LocalTime getDeliverySlotStart() { return deliverySlotStart; }
    public void setDeliverySlotStart(LocalTime deliverySlotStart) { this.deliverySlotStart = deliverySlotStart; }

    public LocalTime getDeliverySlotEnd() { return deliverySlotEnd; }
    public void setDeliverySlotEnd(LocalTime deliverySlotEnd) { this.deliverySlotEnd = deliverySlotEnd; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}