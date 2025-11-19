-- Patch 0200 (dev): add description to vendor_details
-- Keeps description bounded and index-friendly

-- MySQL-compatible idempotent add (works without ADD COLUMN IF NOT EXISTS)
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'vendor_details'
       AND COLUMN_NAME = 'description') = 0,
  'ALTER TABLE `vendor_details` ADD COLUMN `description` VARCHAR(1000) NULL COMMENT ''Vendor description''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
