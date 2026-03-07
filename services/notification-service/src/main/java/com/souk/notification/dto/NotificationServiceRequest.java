package com.souk.notification.dto;

import java.util.List;

public record NotificationServiceRequest(
        String notificationId,
        String workflowId,
        String recipientId,
        String recipientType,
        List<NotificationChannel> channels,
        DispatchStrategy dispatchStrategy,
        NotificationPriority priority,
        String subject,
        String title,
        String body,
        RecipientPreferences recipientPreferences
) {}
