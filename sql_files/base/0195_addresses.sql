CREATE TABLE IF NOT EXISTS addresses (
    address_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL, -- customer_id or vendor_id
    owner_type ENUM('CUSTOMER', 'VENDOR') NOT NULL,

    -- Labeling
    label VARCHAR(255), -- e.g., "Home", "Downtown Branch"
    address_type VARCHAR(50), -- e.g., 'HOME', 'OFFICE', 'SHIPPING', 'BILLING', 'BRANCH', 'PRIMARY'

    -- Physical Address
    street VARCHAR(255),
    unit VARCHAR(255), -- secondary address line
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'Morocco',
    landmark VARCHAR(255),

    -- Geospatial
    latitude DECIMAL(10, 8) DEFAULT NULL,
    longitude DECIMAL(11, 8) DEFAULT NULL,

    -- Status & Flags
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,

    -- Metadata (e.g., specific instructions, vendor schedule)
    metadata JSON DEFAULT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_address_owner (owner_id, owner_type),
    INDEX idx_address_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
