-- --------------------------------------------------------
-- PATCH: Add FAILED status to orders table
-- Author: Claude
-- Date: 2026-01-25
--
-- Adds FAILED to the status ENUM for orders that fail
-- due to payment issues, inventory problems, etc.
-- --------------------------------------------------------

-- Check if FAILED already exists in the ENUM
SET @has_failed = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'status'
    AND COLUMN_TYPE LIKE '%FAILED%'
);

SET @sql = IF(@has_failed = 0,
  'ALTER TABLE orders MODIFY COLUMN status ENUM(''PENDING'',''CONFIRMED'',''PAID'',''SHIPPED'',''DELIVERED'',''CANCELLED'',''REFUNDED'',''FAILED'') DEFAULT ''PENDING''',
  'SELECT ''FAILED status already exists in orders.status ENUM'' AS Info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
