-- --------------------------------------------------------
-- Patch 0440: notification_requests + notification_delivery_log
--
-- notification_requests: idempotency key per notificationId + signal retry store.
--   signalled = FALSE means delivery results are persisted but the Temporal signal
--   was not yet sent (e.g. service crashed); SignalRetryRunner retries on startup.
--
-- notification_delivery_log: per-channel audit trail written by trackDelivery.
-- --------------------------------------------------------

CREATE TABLE notification_requests (
  notification_id    VARCHAR(64)  PRIMARY KEY,
  workflow_id        VARCHAR(128) NOT NULL,
  recipient_id       VARCHAR(64)  NOT NULL,
  recipient_type     VARCHAR(32)  NOT NULL,
  status             VARCHAR(32)  NOT NULL DEFAULT 'ACCEPTED',
  delivery_results_json TEXT NULL,
  signal_sent        BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_notif_req_signal_sent (signal_sent, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_delivery_log (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  notification_id VARCHAR(64)  NOT NULL,
  channel         VARCHAR(16)  NOT NULL,
  status          VARCHAR(16)  NOT NULL,
  reason          VARCHAR(512) NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_delivery_log_notification_id (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
