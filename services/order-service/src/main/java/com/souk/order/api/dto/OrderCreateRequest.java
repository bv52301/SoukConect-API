package com.souk.order.api.dto;

import com.souk.common.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record OrderCreateRequest(
        Long customerId,
        Long addressId,
        BigDecimal totalAmount,
        Order.PaymentMethod paymentMethod,
        LocalDate requestedDeliveryDate,
        Order.DeliveryFlexibility deliveryFlexibility,
        LocalTime deliverySlotStart,
        LocalTime deliverySlotEnd,
        String notes,
        List<OrderItemRequest> items
) {
    public Order toDomain(Customer customer, CustomerAddress address) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setAddress(address);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : Order.PaymentMethod.CARD);
        order.setRequestedDeliveryDate(requestedDeliveryDate);
        order.setDeliveryFlexibility(deliveryFlexibility != null
                ? deliveryFlexibility
                : Order.DeliveryFlexibility.FLEXIBLE);
        order.setDeliverySlotStart(deliverySlotStart);
        order.setDeliverySlotEnd(deliverySlotEnd);
        order.setNotes(notes);

        if (items != null && !items.isEmpty()) {
            List<OrderItem> orderItems = items.stream().map(i -> {
                OrderItem item = new OrderItem();
                item.setProduct(i.toProduct());
                item.setQuantity(i.quantity());
                item.setUnitPrice(i.unitPrice());
                item.setRequestedDeliveryDate(i.requestedDeliveryDate());
                item.setDeliveryFlexibility(i.deliveryFlexibility());
                item.setDeliverySlotStart(i.deliverySlotStart());
                item.setDeliverySlotEnd(i.deliverySlotEnd());
                item.setOrder(order);
                return item;
            }).toList();
            order.setItems(orderItems);
        }

        return order;
    }

    public record OrderItemRequest(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            LocalDate requestedDeliveryDate,
            OrderItem.DeliveryFlexibility deliveryFlexibility,
            LocalTime deliverySlotStart,
            LocalTime deliverySlotEnd
    ) {
        public Product toProduct() {
            Product p = new Product();
            p.setId(productId);
            return p;
        }
    }
}