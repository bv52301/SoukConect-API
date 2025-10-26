package com.souk.order.api.dto;

import com.souk.common.domain.Order;
import com.souk.common.domain.OrderItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        Long id,
        Long customerId,
        Long addressId,
        BigDecimal totalAmount,
        String status,
        String paymentMethod,
        LocalDate requestedDeliveryDate,
        String deliveryFlexibility,
        LocalTime deliverySlotStart,
        LocalTime deliverySlotEnd,
        String notes,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(OrderItemResponse::from).toList()
                : null;

        return new OrderResponse(
                order.getId(),
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getAddress() != null ? order.getAddress().getId() : null,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getPaymentMethod().name(),
                order.getRequestedDeliveryDate(),
                order.getDeliveryFlexibility().name(),
                order.getDeliverySlotStart(),
                order.getDeliverySlotEnd(),
                order.getNotes(),
                itemResponses
        );
    }

    public record OrderItemResponse(
            Long id,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            LocalDate requestedDeliveryDate,
            String deliveryFlexibility,
            LocalTime deliverySlotStart,
            LocalTime deliverySlotEnd
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getProduct() != null ? item.getProduct().getId() : null,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal(),
                    item.getRequestedDeliveryDate(),
                    item.getDeliveryFlexibility() != null ? item.getDeliveryFlexibility().name() : null,
                    item.getDeliverySlotStart(),
                    item.getDeliverySlotEnd()
            );
        }
    }
}