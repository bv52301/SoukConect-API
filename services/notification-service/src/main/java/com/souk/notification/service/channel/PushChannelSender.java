package com.souk.notification.service.channel;

import com.souk.notification.dto.DeliveryResult;
import com.souk.notification.dto.DeliveryStatus;
import com.souk.notification.dto.DeviceToken;
import com.souk.notification.dto.NotificationChannel;
import com.souk.notification.dto.NotificationServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Task 5.11 — Send push notification to all active device tokens.
 * Returns SENT if at least one token succeeds; FAILED if all fail.
 */
@Component
public class PushChannelSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(PushChannelSender.class);

    @Value("${push.provider:fcm}")
    private String pushProvider;

    @Value("${push.fcm.server-key:}")
    private String fcmServerKey;

    @Override
    public DeliveryResult send(NotificationServiceRequest request) {
        if (request.recipientPreferences() == null
                || request.recipientPreferences().deviceTokens() == null
                || request.recipientPreferences().deviceTokens().isEmpty()) {
            return new DeliveryResult(NotificationChannel.PUSH, DeliveryStatus.FAILED,
                    "No device tokens available");
        }

        List<DeviceToken> tokens = request.recipientPreferences().deviceTokens();
        int successCount = 0;
        StringBuilder errors = new StringBuilder();

        for (DeviceToken deviceToken : tokens) {
            try {
                sendToToken(deviceToken, request);
                successCount++;
            } catch (Exception e) {
                log.warn("Push delivery failed for token={} device={}: {}",
                        deviceToken.token(), deviceToken.deviceType(), e.getMessage());
                errors.append(e.getMessage()).append("; ");
            }
        }

        if (successCount > 0) {
            return new DeliveryResult(NotificationChannel.PUSH, DeliveryStatus.SENT,
                    "Delivered to " + successCount + "/" + tokens.size() + " tokens");
        }
        return new DeliveryResult(NotificationChannel.PUSH, DeliveryStatus.FAILED,
                errors.toString().trim());
    }

    private void sendToToken(DeviceToken deviceToken, NotificationServiceRequest request) {
        // TODO: integrate with FCM / APNs provider using push.fcm.server-key or APNS credentials
        // Placeholder — throws RuntimeException to simulate failure if no key configured
        if (fcmServerKey == null || fcmServerKey.isBlank()) {
            throw new RuntimeException("Push provider not configured (push.fcm.server-key is empty)");
        }
        log.info("PUSH sent via {} to token={} title={}", pushProvider, deviceToken.token(), request.title());
    }
}
