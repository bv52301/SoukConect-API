package com.souk.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Per-channel delivery audit trail written by trackDelivery (via POST /api/notifications/delivery-status).
 */
@Entity
@Table(name = "notification_delivery_log")
@Getter
@Setter
@NoArgsConstructor
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false, length = 64)
    private String notificationId;

    @Column(name = "channel", nullable = false, length = 16)
    private String channel;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
