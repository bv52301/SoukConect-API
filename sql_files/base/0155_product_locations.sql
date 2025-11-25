-- product_locations junction table for many-to-many product-location relationship
-- Allows products to be available at multiple vendor locations with location-specific attributes

CREATE TABLE IF NOT EXISTS product_locations (
    product_id BIGINT NOT NULL,
    vendor_location_id BIGINT NOT NULL,
    sku VARCHAR(255) NOT NULL COMMENT 'Location-specific SKU identifier',
    available BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Product availability at this location',
    stock INT NULL DEFAULT 0 COMMENT 'Inventory quantity at this location',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (product_id, vendor_location_id),

    -- Ensure SKU is unique per location
    UNIQUE KEY unique_location_sku (vendor_location_id, sku),

    CONSTRAINT fk_product_locations_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_product_locations_vendor_location
        FOREIGN KEY (vendor_location_id) REFERENCES vendor_locations(id)
        ON DELETE CASCADE,

    INDEX idx_product_locations_product_id (product_id),
    INDEX idx_product_locations_vendor_location_id (vendor_location_id),
    INDEX idx_product_locations_available (available)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
