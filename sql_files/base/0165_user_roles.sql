-- --------------------------------------------------------
-- Table structure for table `user_roles`
-- Many-to-many relationship: users can have multiple roles
-- Example: A vendor can also be a customer
-- --------------------------------------------------------

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role ENUM('CUSTOMER', 'VENDOR', 'ADMIN', 'SUPPORT') NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (user_id, role),

  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id)
    REFERENCES users(user_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  INDEX idx_user_role_role (role),
  INDEX idx_user_role_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
