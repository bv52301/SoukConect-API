-- --------------------------------------------------------
-- Patch 0430: device_tokens
-- Stores push notification device tokens linked to users.
-- One user may have multiple active tokens (multiple devices).
-- --------------------------------------------------------

CREATE TABLE device_tokens (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  device_token VARCHAR(512) NOT NULL,
  device_type ENUM('ANDROID', 'IOS', 'WEB') NOT NULL,
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  last_seen_at TIMESTAMP NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uq_device_token (device_token),
  INDEX idx_device_tokens_user_id (user_id),
  INDEX idx_device_tokens_user_active (user_id, active),

  CONSTRAINT fk_device_tokens_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
