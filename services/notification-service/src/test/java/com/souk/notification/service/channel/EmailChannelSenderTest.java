package com.souk.notification.service.channel;

import com.souk.notification.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 9.11 — EmailChannelSender with mocked providers
 */
class EmailChannelSenderTest {

    private EmailChannelSender sender(String mailUsername) {
        EmailChannelSender s = new EmailChannelSender();
        ReflectionTestUtils.setField(s, "mailHost", "smtp.test.com");
        ReflectionTestUtils.setField(s, "mailPort", 587);
        ReflectionTestUtils.setField(s, "mailUsername", mailUsername);
        ReflectionTestUtils.setField(s, "mailPassword", "pw");
        ReflectionTestUtils.setField(s, "mailFrom", "noreply@test.com");
        return s;
    }

    @Test
    void noEmailAddress_returnsFailed() {
        EmailChannelSender s = sender("user@test.com");
        NotificationServiceRequest req = buildRequest(null);
        assertThat(s.send(req).status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void withEmailAndConfiguredProvider_returnsSent() {
        EmailChannelSender s = sender("user@test.com");
        NotificationServiceRequest req = buildRequest("recipient@example.com");
        DeliveryResult result = s.send(req);
        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(result.channel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void withoutConfiguredProvider_returnsFailed() {
        EmailChannelSender s = sender(""); // no mail username
        NotificationServiceRequest req = buildRequest("recipient@example.com");
        assertThat(s.send(req).status()).isEqualTo(DeliveryStatus.FAILED);
    }

    private NotificationServiceRequest buildRequest(String email) {
        return new NotificationServiceRequest(
                "n1", "wf-n1", "c1", "CUSTOMER",
                List.of(NotificationChannel.EMAIL),
                DispatchStrategy.BROADCAST,
                NotificationPriority.NORMAL,
                "Subject", "Title", "Body",
                new RecipientPreferences(List.of(), email, null, "en", List.of()));
    }
}
