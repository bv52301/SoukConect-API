-- vendor_locations table for vendors with multiple physical locations
-- Each vendor can have multiple locations (branches) with GPS coordinates and optional schedules

CREATE TABLE IF NOT EXISTS vendor_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    location_name VARCHAR(255) NOT NULL COMMENT 'e.g., "Downtown Branch", "Airport Outlet"',
    latitude DECIMAL(10, 8) NOT NULL COMMENT 'GPS latitude coordinate',
    longitude DECIMAL(11, 8) NOT NULL COMMENT 'GPS longitude coordinate',
    address1 VARCHAR(255) NULL,
    address2 VARCHAR(255) NULL,
    state VARCHAR(100) NULL,
    pincode VARCHAR(20) NULL,
    landmark VARCHAR(255) NULL,
    schedule JSON NULL COMMENT 'Location-specific schedule overrides vendor schedule',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendor_locations_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendor_details(vendor_id)
        ON DELETE CASCADE,

    INDEX idx_vendor_locations_vendor_id (vendor_id),
    INDEX idx_vendor_locations_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
