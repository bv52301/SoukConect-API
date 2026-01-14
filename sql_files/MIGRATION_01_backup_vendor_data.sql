-- ============================================================
-- MIGRATION SCRIPT 1 of 2: Backup Existing Vendor Data
-- ============================================================
--
-- PURPOSE: Backs up existing vendor_details data before schema modification
--
-- WHEN TO RUN:
--   Run this BEFORE running install_sql_files.sh
--
-- SEQUENCE:
--   1. Run this script (MIGRATION_01_backup_vendor_data.sql)
--   2. Run install_sql_files.sh (adds user_id columns, creates auth tables)
--   3. Run MIGRATION_02_restore_and_migrate.sql (restores data + creates user accounts)
--
-- WHAT THIS DOES:
--   - Creates vendor_backup_log table
--   - Copies ALL existing vendor data to backup table
--   - Displays backup summary
--
-- HOW TO RUN:
--   mysql -u root -p soukconect < MIGRATION_01_backup_vendor_data.sql
--
-- ============================================================

USE soukconect;

-- ============================================================
-- Create Backup Table
-- ============================================================

-- Drop existing backup table if it exists (for re-runs)
DROP TABLE IF EXISTS vendor_backup_log;

-- Create backup table with same structure as vendor_details
CREATE TABLE vendor_backup_log (
  backup_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vendor_id BIGINT NOT NULL,
  name VARCHAR(100),
  supportedCategories LONGTEXT,
  image VARCHAR(300),
  address1 VARCHAR(100),
  address2 VARCHAR(100),
  state VARCHAR(100),
  landmark VARCHAR(255),
  pincode VARCHAR(15),
  contact_name VARCHAR(100),
  phone_number VARCHAR(20),
  email VARCHAR(100),
  created_at TIMESTAMP,
  backed_up_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_vendor_id (vendor_id),
  INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Copy All Vendor Data to Backup Table
-- ============================================================

INSERT INTO vendor_backup_log (
  vendor_id, name, supportedCategories, image,
  address1, address2, state, landmark, pincode,
  contact_name, phone_number, email, created_at
)
SELECT
  vendor_id, name, supportedCategories, image,
  address1, address2, state, landmark, pincode,
  contact_name, phone_number, email, created_at
FROM vendor_details;

-- ============================================================
-- Display Backup Summary
-- ============================================================

SELECT '============================================' AS '';
SELECT 'VENDOR DATA BACKUP COMPLETE' AS '';
SELECT '============================================' AS '';

SELECT COUNT(*) AS 'Total vendors backed up' FROM vendor_backup_log;

SELECT '' AS '';
SELECT 'Email analysis:' AS '';

SELECT
  COUNT(DISTINCT email) AS 'Unique emails',
  SUM(CASE WHEN email IS NULL OR TRIM(email) = '' THEN 1 ELSE 0 END) AS 'Missing emails',
  SUM(CASE WHEN email NOT REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'
           AND email IS NOT NULL AND TRIM(email) != '' THEN 1 ELSE 0 END) AS 'Invalid email format'
FROM vendor_backup_log;

SELECT '' AS '';
SELECT 'Duplicate emails:' AS '';

SELECT email, COUNT(*) AS vendor_count
FROM vendor_backup_log
WHERE email IS NOT NULL
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY vendor_count DESC;

SELECT '' AS '';
SELECT '============================================' AS '';
SELECT 'NEXT STEPS:' AS '';
SELECT '============================================' AS '';
SELECT '1. Review the backup: SELECT * FROM vendor_backup_log;' AS '';
SELECT '2. Run: ./install_sql_files.sh' AS '';
SELECT '3. Then run: MIGRATION_02_restore_and_migrate.sql' AS '';
SELECT '============================================' AS '';
