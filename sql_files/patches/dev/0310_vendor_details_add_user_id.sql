-- --------------------------------------------------------
-- PATCH: Add user_id to vendor_details table
-- Links vendors to authentication system
-- For new installations only (no data migration)
-- --------------------------------------------------------

-- Check if user_id column exists, only add if missing
SET @column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'vendor_details'
    AND COLUMN_NAME = 'user_id'
);

-- Add user_id column only if it doesn't exist
SET @sql_add_column = IF(@column_exists = 0,
  'ALTER TABLE vendor_details ADD COLUMN user_id BIGINT NULL AFTER vendor_id',
  'SELECT "user_id column already exists in vendor_details" AS Info'
);

PREPARE stmt FROM @sql_add_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if foreign key exists
SET @fk_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'vendor_details'
    AND CONSTRAINT_NAME = 'fk_vendor_user'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

-- Add foreign key constraint only if it doesn't exist
SET @sql_add_fk = IF(@fk_exists = 0,
  'ALTER TABLE vendor_details ADD CONSTRAINT fk_vendor_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT "Foreign key fk_vendor_user already exists" AS Info'
);

PREPARE stmt FROM @sql_add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if index exists
SET @idx_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'vendor_details'
    AND INDEX_NAME = 'idx_vendor_user'
);

-- Add index only if it doesn't exist
SET @sql_add_idx = IF(@idx_exists = 0,
  'CREATE INDEX idx_vendor_user ON vendor_details(user_id)',
  'SELECT "Index idx_vendor_user already exists" AS Info'
);

PREPARE stmt FROM @sql_add_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
