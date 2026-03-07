package com.souk.vendor.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Task 7.2 — Stub delivery-status endpoint in vendor-service.
 * Persists per-channel delivery outcome to notification_delivery_log via notificationId.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationDeliveryController {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryController.class);

    @PostMapping("/delivery-status")
    public ResponseEntity<Void> recordDeliveryStatus(@RequestBody Map<String, Object> body) {
        String notificationId = (String) body.get("notificationId");
        String channel = (String) body.get("channel");
        String status = (String) body.get("status");
        String reason = (String) body.get("reason");
        log.info("Delivery status recorded: notificationId={} channel={} status={} reason={}",
                notificationId, channel, status, reason);
        // TODO: persist to notification_delivery_log table when full implementation is done
        return ResponseEntity.ok().build();
    }
}
