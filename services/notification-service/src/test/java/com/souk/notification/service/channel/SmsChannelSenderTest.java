package com.souk.notification.service.channel;

import com.souk.notification.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 9.11 — SmsChannelSender with mocked providers
 */
class SmsChannelSenderTest {

    private SmsChannelSender sender(String accountSid) {
        SmsChannelSender s = new SmsChannelSender();
        ReflectionTestUtils.setField(s, "smsProvider", "twilio");
        ReflectionTestUtils.setField(s, "twilioAccountSid", accountSid);
        ReflectionTestUtils.setField(s, "twilioAuthToken", "token");
        ReflectionTestUtils.setField(s, "twilioFromNumber", "+15550000");
        return s;
    }

    @Test
    void noPhoneNumber_returnsFailed() {
        SmsChannelSender s = sender("ACtest");
        NotificationServiceRequest req = buildRequest(null);
        assertThat(s.send(req).status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void withPhoneAndConfiguredProvider_returnsSent() {
        SmsChannelSender s = sender("ACtest");
        NotificationServiceRequest req = buildRequest("+212600000000");
        assertThat(s.send(req).status()).isEqualTo(DeliveryStatus.SENT);
    }

    @Test
    void withoutConfiguredProvider_returnsFailed() {
        SmsChannelSender s = sender(""); // no account SID
        NotificationServiceRequest req = buildRequest("+212600000000");
        assertThat(s.send(req).status()).isEqualTo(DeliveryStatus.FAILED);
    }

    private NotificationServiceRequest buildRequest(String phone) {
        return new NotificationServiceRequest(
                "n1", "wf-n1", "c1", "CUSTOMER",
                List.of(NotificationChannel.SMS),
                DispatchStrategy.BROADCAST,
                NotificationPriority.NORMAL,
                "Sub", "Title", "Body",
                new RecipientPreferences(List.of(), null, phone, "en", List.of()));
    }
}
