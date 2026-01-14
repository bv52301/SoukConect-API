-- --------------------------------------------------------
-- Table structure for table `user_oauth_connections`
-- Stores OAuth provider connections for each user
-- Users can link multiple OAuth providers (Google, Apple, Facebook)
-- --------------------------------------------------------

CREATE TABLE user_oauth_connections (
  connection_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider ENUM('GOOGLE', 'APPLE', 'FACEBOOK') NOT NULL,
  provider_user_id VARCHAR(255) NOT NULL,  -- OAuth provider's unique user ID

  -- OAuth tokens (encrypted in production)
  access_token TEXT,
  refresh_token TEXT,
  token_expiry TIMESTAMP NULL,

  -- Profile data from provider
  provider_email VARCHAR(255),
  provider_name VARCHAR(255),
  provider_picture_url VARCHAR(500),

  -- Metadata
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_used TIMESTAMP NULL,

  -- Composite unique constraint: one provider connection per user per provider
  UNIQUE KEY uk_user_provider (user_id, provider),

  -- Unique constraint: provider's user ID is unique across our system
  UNIQUE KEY uk_provider_user_id (provider, provider_user_id),

  CONSTRAINT fk_oauth_user FOREIGN KEY (user_id)
    REFERENCES users(user_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  INDEX idx_oauth_user (user_id),
  INDEX idx_oauth_provider (provider),
  INDEX idx_oauth_provider_user_id (provider_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
