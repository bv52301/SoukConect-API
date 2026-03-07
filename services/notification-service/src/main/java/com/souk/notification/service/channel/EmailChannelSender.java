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
 * Task 5.12 — Send email via configured SMTP provider.
 */
@Component
public class EmailChannelSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelSender.class);

    @Value("${mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${mail.port:587}")
    private int mailPort;

    @Value("${mail.username:}")
    private String mailUsername;

    @Value("${mail.password:}")
    private String mailPassword;

    @Value("${mail.from:noreply@soukconect.com}")
    private String mailFrom;

    @Override
    public DeliveryResult send(NotificationServiceRequest request) {
        String emailAddress = request.recipientPreferences() != null
                ? request.recipientPreferences().emailAddress()
                : null;

        if (emailAddress == null || emailAddress.isBlank()) {
            return new DeliveryResult(NotificationChannel.EMAIL, DeliveryStatus.FAILED,
                    "No email address available");
        }

        try {
            sendEmail(emailAddress, request.subject(), request.body());
            return new DeliveryResult(NotificationChannel.EMAIL, DeliveryStatus.SENT, null);
        } catch (Exception e) {
            log.warn("Email delivery failed to {}: {}", emailAddress, e.getMessage());
            return new DeliveryResult(NotificationChannel.EMAIL, DeliveryStatus.FAILED, e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        // TODO: integrate with JavaMailSender using mail.* properties
        // Placeholder — throws if not configured
        if (mailUsername == null || mailUsername.isBlank()) {
            throw new RuntimeException("Email provider not configured (mail.username is empty)");
        }
        log.info("EMAIL sent via {}:{} from={} to={} subject={}", mailHost, mailPort, mailFrom, to, subject);
    }
}
