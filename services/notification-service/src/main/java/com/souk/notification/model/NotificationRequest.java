package com.souk.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Idempotency store: one row per notificationId.
 * Also used for signal retry — persists delivery results JSON before signalling BPM.
 */
@Entity
@Table(name = "notification_requests")
@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {

    @Id
    @Column(name = "notification_id", nullable = false, length = 64)
    private String notificationId;

    @Column(name = "workflow_id", nullable = false, length = 128)
    private String workflowId;

    @Column(name = "recipient_id", length = 64)
    private String recipientId;

    @Column(name = "recipient_type", length = 32)
    private String recipientType;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "delivery_results_json", columnDefinition = "TEXT")
    private String deliveryResultsJson;

    @Column(name = "signal_sent")
    private boolean signalSent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
