-- Patch 0420 (dev): Drop legacy address tables and columns after migration to unified addresses table
-- IMPORTANT: Only run this AFTER verifying 0410_migrate_addresses_data.sql completed successfully

DELIMITER //

-- Helper procedure to drop FK if exists
DROP PROCEDURE IF EXISTS drop_fk_if_exists//
CREATE PROCEDURE drop_fk_if_exists(IN tableName VARCHAR(64), IN fkName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND CONSTRAINT_NAME = fkName
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' DROP FOREIGN KEY ', fkName);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

-- Helper procedure to drop column if exists
DROP PROCEDURE IF EXISTS drop_column_if_exists//
CREATE PROCEDURE drop_column_if_exists(IN tableName VARCHAR(64), IN colName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND COLUMN_NAME = colName
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' DROP COLUMN ', colName);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

-- Helper procedure to drop index if exists
DROP PROCEDURE IF EXISTS drop_index_if_exists//
CREATE PROCEDURE drop_index_if_exists(IN tableName VARCHAR(64), IN indexName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' DROP INDEX ', indexName);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

-- 1. Drop foreign key constraint from orders table that references customer_addresses
CALL drop_fk_if_exists('orders', 'fk_order_address');

-- 2. Drop legacy customer_addresses table
DROP TABLE IF EXISTS customer_addresses;

-- 3. Migrate product_locations from vendor_location_id to address_id
-- 3a. Add address_id column (only if not exists)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'product_locations'
                   AND COLUMN_NAME = 'address_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE product_locations ADD COLUMN address_id BIGINT NULL AFTER vendor_location_id',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3b. Update address_id by looking up the migrated address from vendor_locations
UPDATE product_locations pl
JOIN vendor_locations vl ON pl.vendor_location_id = vl.id
JOIN addresses a ON a.owner_id = vl.vendor_id
                 AND a.owner_type = 'VENDOR'
                 AND a.label = vl.location_name
SET pl.address_id = a.address_id
WHERE pl.address_id IS NULL;

-- 3c. Drop old FK constraint
CALL drop_fk_if_exists('product_locations', 'fk_product_locations_vendor_location');

-- 3d. Drop old primary key (check if vendor_location_id column exists first)
SET @has_vendor_loc = (SELECT COUNT(*) FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'product_locations'
                       AND COLUMN_NAME = 'vendor_location_id');
SET @sql = IF(@has_vendor_loc > 0, 'ALTER TABLE product_locations DROP PRIMARY KEY', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3e. Drop vendor_location_id column
CALL drop_column_if_exists('product_locations', 'vendor_location_id');

-- 3f. Add new primary key with address_id (check if not already exists)
SET @pk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                  WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'product_locations'
                  AND CONSTRAINT_TYPE = 'PRIMARY KEY');
SET @sql = IF(@pk_exists = 0, 'ALTER TABLE product_locations ADD PRIMARY KEY (product_id, address_id)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3g. Add FK to addresses (if not exists)
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                  WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'product_locations'
                  AND CONSTRAINT_NAME = 'fk_product_locations_address');
SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE product_locations ADD CONSTRAINT fk_product_locations_address FOREIGN KEY (address_id) REFERENCES addresses(address_id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3h. Update unique constraint for SKU
CALL drop_index_if_exists('product_locations', 'unique_location_sku');
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'product_locations'
                   AND INDEX_NAME = 'unique_address_sku');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE product_locations ADD UNIQUE KEY unique_address_sku (address_id, sku)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. Drop legacy vendor_locations table
DROP TABLE IF EXISTS vendor_locations;

-- 5. Drop deprecated address columns from vendor_details
CALL drop_column_if_exists('vendor_details', 'address1');
CALL drop_column_if_exists('vendor_details', 'address2');
CALL drop_column_if_exists('vendor_details', 'state');
CALL drop_column_if_exists('vendor_details', 'landmark');
CALL drop_column_if_exists('vendor_details', 'pincode');
CALL drop_column_if_exists('vendor_details', 'latitude');
CALL drop_column_if_exists('vendor_details', 'longitude');

-- Cleanup helper procedures
DROP PROCEDURE IF EXISTS drop_fk_if_exists;
DROP PROCEDURE IF EXISTS drop_column_if_exists;
DROP PROCEDURE IF EXISTS drop_index_if_exists;
