-- Add GPS coordinates to vendor_details table
-- These are required fields for the vendor's primary/default location

ALTER TABLE vendor_details
ADD COLUMN latitude DECIMAL(10, 8) NOT NULL DEFAULT 0.0 COMMENT 'GPS latitude coordinate for primary location',
ADD COLUMN longitude DECIMAL(11, 8) NOT NULL DEFAULT 0.0 COMMENT 'GPS longitude coordinate for primary location';

-- Note: Default 0.0 is temporary to allow adding NOT NULL column to existing rows
-- Vendors should update these coordinates to their actual location
