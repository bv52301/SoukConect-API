package com.souk.order.api.dto;

import com.souk.common.domain.Order;
import java.time.LocalDate;
import java.time.LocalTime;

public record OrderUpdateRequest(
        Order.OrderStatus status,
        Order.PaymentMethod paymentMethod,
        LocalDate requestedDeliveryDate,
        Order.DeliveryFlexibility deliveryFlexibility,
        LocalTime deliverySlotStart,
        LocalTime deliverySlotEnd,
        String notes
) {}