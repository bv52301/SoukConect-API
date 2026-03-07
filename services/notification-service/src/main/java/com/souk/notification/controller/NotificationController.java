package com.souk.notification.controller;

import com.souk.notification.dto.NotificationServiceRequest;
import com.souk.notification.model.NotificationDeliveryLog;
import com.souk.notification.repository.NotificationDeliveryLogRepository;
import com.souk.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Task 5.6 — POST /api/notifications/send
 * Accepts NotificationServiceRequest, returns 202 immediately, delegates to NotificationService on background thread.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final NotificationDeliveryLogRepository deliveryLogRepository;

    public NotificationController(NotificationService notificationService,
                                   NotificationDeliveryLogRepository deliveryLogRepository) {
        this.notificationService = notificationService;
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> send(@RequestBody NotificationServiceRequest request) {
        log.debug("Received notification request notificationId={} workflowId={}",
                request.notificationId(), request.workflowId());

        // Task 5.8 — Idempotency: skip re-processing if already handled
        if (notificationService.isAlreadyProcessed(request.notificationId())) {
            log.info("Duplicate notification request ignored notificationId={}", request.notificationId());
            return ResponseEntity.accepted().build();
        }

        // Persist accepted state for idempotency
        notificationService.accept(request);

        // Dispatch asynchronously; controller returns 202 immediately
        notificationService.process(request);

        return ResponseEntity.accepted().build();
    }

    /**
     * Receives delivery status updates from trackDelivery activity (or domain services).
     * Persists each outcome to notification_delivery_log.
     */
    @PostMapping("/delivery-status")
    public ResponseEntity<Void> recordDeliveryStatus(@RequestBody Map<String, Object> body) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setNotificationId((String) body.get("notificationId"));
        log.setChannel((String) body.get("channel"));
        log.setStatus((String) body.get("status"));
        log.setReason((String) body.get("reason"));
        deliveryLogRepository.save(log);
        return ResponseEntity.ok().build();
    }
}
