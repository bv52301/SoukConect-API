package com.souk.notification.dto;

import java.util.List;

public record RecipientPreferences(
        List<DeviceToken> deviceTokens,
        String emailAddress,
        String phoneNumber,
        String locale,
        List<NotificationChannel> optOuts
) {}
