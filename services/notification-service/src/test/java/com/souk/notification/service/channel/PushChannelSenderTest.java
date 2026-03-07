package com.souk.notification.service.channel;

import com.souk.notification.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 9.10 — PushChannelSender: sends to all active device tokens; SENT if at least one succeeds
 */
class PushChannelSenderTest {

    private PushChannelSender sender() {
        PushChannelSender s = new PushChannelSender();
        // Inject a non-blank server key so provider is considered configured
        ReflectionTestUtils.setField(s, "fcmServerKey", "test-fcm-key");
        ReflectionTestUtils.setField(s, "pushProvider", "fcm");
        return s;
    }

    @Test
    void noDeviceTokens_returnsFailed() {
        PushChannelSender s = sender();
        NotificationServiceRequest req = buildRequest(List.of());
        DeliveryResult result = s.send(req);
        assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(result.channel()).isEqualTo(NotificationChannel.PUSH);
    }

    @Test
    void withDeviceTokens_sentIfAtLeastOneSucceeds() {
        PushChannelSender s = sender();
        List<DeviceToken> tokens = List.of(
                new DeviceToken("token-android", DeviceType.ANDROID),
                new DeviceToken("token-ios", DeviceType.IOS));
        NotificationServiceRequest req = buildRequest(tokens);
        DeliveryResult result = s.send(req);
        // With a configured key, all tokens succeed (stub logs, no real HTTP call)
        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
    }

    private NotificationServiceRequest buildRequest(List<DeviceToken> tokens) {
        return new NotificationServiceRequest(
                "n1", "wf-n1", "c1", "CUSTOMER",
                List.of(NotificationChannel.PUSH),
                DispatchStrategy.BROADCAST,
                NotificationPriority.NORMAL,
                "Sub", "Title", "Body",
                new RecipientPreferences(tokens, null, null, "en", List.of()));
    }
}
