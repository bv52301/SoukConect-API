-- Patch 0210 (dev): add description to products
-- Keeps description bounded and index-friendly

-- MySQL-compatible idempotent add (works without ADD COLUMN IF NOT EXISTS)
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'products'
       AND COLUMN_NAME = 'description') = 0,
  'ALTER TABLE `products` ADD COLUMN `description` VARCHAR(1000) NULL COMMENT ''Product description''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
