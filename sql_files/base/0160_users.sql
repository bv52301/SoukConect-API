-- --------------------------------------------------------
-- Table structure for table `users`
-- Central authentication table - supports multiple roles per user
-- --------------------------------------------------------

CREATE TABLE users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  phone VARCHAR(30),

  -- Password authentication (NULL if OAuth-only user)
  password_hash VARCHAR(255) NULL,

  -- Email verification
  email_verified BOOLEAN DEFAULT FALSE,
  email_verification_token VARCHAR(255),
  email_verification_expiry TIMESTAMP NULL,

  -- Multi-factor authentication
  mfa_enabled BOOLEAN DEFAULT FALSE,
  mfa_secret VARCHAR(255),

  -- Password reset
  password_reset_token VARCHAR(255),
  password_reset_expiry TIMESTAMP NULL,

  -- Security tracking
  last_login TIMESTAMP NULL,
  last_login_method ENUM('PASSWORD', 'GOOGLE', 'APPLE', 'FACEBOOK') NULL,
  account_locked BOOLEAN DEFAULT FALSE,
  failed_login_attempts INT DEFAULT 0,
  locked_until TIMESTAMP NULL,

  -- Profile (minimal, business data in customers/vendor_details)
  profile_picture_url VARCHAR(500),

  -- Audit fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  -- Indexes
  INDEX idx_user_email (email),
  INDEX idx_user_phone (phone),
  INDEX idx_email_verification_token (email_verification_token),
  INDEX idx_password_reset_token (password_reset_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
