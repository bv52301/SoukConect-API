package com.souk.notification.dto;

public record DeviceToken(
        String token,
        DeviceType deviceType
) {}
