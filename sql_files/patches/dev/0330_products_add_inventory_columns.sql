-- Migration: Add inventory tracking columns to products table
-- Purpose: Support stock reservation for order processing

ALTER TABLE products
ADD COLUMN stock_quantity INT DEFAULT 0 AFTER use_vendor_schedule,
ADD COLUMN reserved_quantity INT DEFAULT 0 AFTER stock_quantity;

-- Set initial stock for existing products (adjust as needed)
UPDATE products SET stock_quantity = 100, reserved_quantity = 0 WHERE stock_quantity IS NULL;
