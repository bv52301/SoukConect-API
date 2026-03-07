package com.souk.notification.dto;

public record DeliveryResult(
        NotificationChannel channel,
        DeliveryStatus status,
        String reason
) {}
