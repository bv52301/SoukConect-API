-- --------------------------------------------------------
-- Table structure for table `staff_users`
-- Additional metadata for ADMIN and SUPPORT roles
-- One-to-one relationship with users table
-- --------------------------------------------------------

CREATE TABLE staff_users (
  staff_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,

  -- Staff-specific information
  employee_id VARCHAR(50) UNIQUE,
  department VARCHAR(100),
  job_title VARCHAR(100),

  -- Permissions stored as JSON for flexibility
  -- Example: {"canManageUsers": true, "canManageVendors": true, "canViewReports": true}
  permissions JSON,

  -- Work status
  is_active BOOLEAN DEFAULT TRUE,
  hire_date DATE,
  termination_date DATE NULL,

  -- Audit fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,  -- Which admin created this staff account

  CONSTRAINT fk_staff_user FOREIGN KEY (user_id)
    REFERENCES users(user_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_staff_created_by FOREIGN KEY (created_by)
    REFERENCES users(user_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,

  INDEX idx_staff_user (user_id),
  INDEX idx_staff_employee_id (employee_id),
  INDEX idx_staff_department (department),
  INDEX idx_staff_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
