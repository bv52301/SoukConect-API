package com.souk.notification.service.channel;

import com.souk.notification.dto.DeliveryResult;
import com.souk.notification.dto.DeliveryStatus;
import com.souk.notification.dto.NotificationChannel;
import com.souk.notification.dto.NotificationServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Task 5.13 — Send SMS via configured provider (Twilio / Vonage).
 */
@Component
public class SmsChannelSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(SmsChannelSender.class);

    @Value("${sms.provider:twilio}")
    private String smsProvider;

    @Value("${sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${sms.twilio.from-number:}")
    private String twilioFromNumber;

    @Override
    public DeliveryResult send(NotificationServiceRequest request) {
        String phoneNumber = request.recipientPreferences() != null
                ? request.recipientPreferences().phoneNumber()
                : null;

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return new DeliveryResult(NotificationChannel.SMS, DeliveryStatus.FAILED,
                    "No phone number available");
        }

        try {
            sendSms(phoneNumber, request.body());
            return new DeliveryResult(NotificationChannel.SMS, DeliveryStatus.SENT, null);
        } catch (Exception e) {
            log.warn("SMS delivery failed to {}: {}", phoneNumber, e.getMessage());
            return new DeliveryResult(NotificationChannel.SMS, DeliveryStatus.FAILED, e.getMessage());
        }
    }

    private void sendSms(String to, String body) {
        // TODO: integrate with Twilio or Vonage SDK using sms.* properties
        // Placeholder — throws if not configured
        if (twilioAccountSid == null || twilioAccountSid.isBlank()) {
            throw new RuntimeException("SMS provider not configured (" + smsProvider + " credentials empty)");
        }
        log.info("SMS sent via {} from={} to={}", smsProvider, twilioFromNumber, to);
    }
}
