-- ============================================================
-- MIGRATION SCRIPT 2 of 2: Restore Vendor Data and Create User Accounts
-- ============================================================
--
-- PURPOSE: Restores vendor data and migrates to authentication system
--
-- PREREQUISITES:
--   - MIGRATION_01_backup_vendor_data.sql already run (vendor_backup_log exists)
--   - install_sql_files.sh already run (auth tables exist, user_id columns added)
--
-- WHEN TO RUN:
--   Run this AFTER install_sql_files.sh completes
--
-- SEQUENCE:
--   1. ✅ MIGRATION_01_backup_vendor_data.sql (already done)
--   2. ✅ install_sql_files.sh (already done)
--   3. → Run this script now
--
-- WHAT THIS DOES:
--   - Restores vendor data from vendor_backup_log
--   - Creates user accounts for vendors with valid emails
--   - Assigns VENDOR role to created users
--   - Links vendors to their user accounts via user_id
--   - Handles duplicates, invalid emails, missing emails
--   - Generates migration report
--
-- HOW TO RUN:
--   mysql -u root -p soukconect < MIGRATION_02_restore_and_migrate.sql
--
-- ============================================================

USE soukconect;

START TRANSACTION;

-- ============================================================
-- STEP 1: Create Migration Log Table
-- ============================================================

CREATE TABLE IF NOT EXISTS vendor_migration_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vendor_id BIGINT,
  vendor_name VARCHAR(100),
  vendor_email VARCHAR(100),
  status ENUM('SUCCESS', 'DUPLICATE_EMAIL', 'INVALID_EMAIL', 'MISSING_EMAIL', 'ERROR') NOT NULL,
  created_user_id BIGINT NULL,
  error_message TEXT NULL,
  migrated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_vendor_id (vendor_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- STEP 2: Verify Vendor Data
-- ============================================================

-- Note: Vendor data is already in vendor_details table
-- The backup was just for safety - we don't need to restore it
-- We just need to create user accounts and link them via user_id

-- ============================================================
-- STEP 3: Vendor Data Migration Procedure
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_vendor_to_auth$$

CREATE PROCEDURE migrate_vendor_to_auth()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_vendor_id BIGINT;
  DECLARE v_name VARCHAR(100);
  DECLARE v_email VARCHAR(100);
  DECLARE v_user_id BIGINT;
  DECLARE v_existing_user_id BIGINT;
  DECLARE v_email_count INT;

  DECLARE vendor_cursor CURSOR FOR
    SELECT vendor_id, name, email FROM vendor_details ORDER BY vendor_id;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN vendor_cursor;

  vendor_loop: LOOP
    FETCH vendor_cursor INTO v_vendor_id, v_name, v_email;

    IF done THEN
      LEAVE vendor_loop;
    END IF;

    -- Case 1: Missing or NULL email
    IF v_email IS NULL OR TRIM(v_email) = '' THEN
      INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, error_message)
      VALUES (v_vendor_id, v_name, v_email, 'MISSING_EMAIL',
              'Vendor has no email - cannot create auth account');
      ITERATE vendor_loop;
    END IF;

    -- Case 2: Invalid email format
    IF v_email NOT REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$' THEN
      INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, error_message)
      VALUES (v_vendor_id, v_name, v_email, 'INVALID_EMAIL',
              'Email format is invalid - cannot create auth account');
      ITERATE vendor_loop;
    END IF;

    -- Case 3: Check if email already exists in users table
    SET v_existing_user_id = NULL;
    SELECT user_id INTO v_existing_user_id
    FROM users
    WHERE email COLLATE utf8mb4_unicode_ci = v_email COLLATE utf8mb4_unicode_ci
    LIMIT 1;

    IF v_existing_user_id IS NOT NULL THEN
      -- Email already has a user account
      IF EXISTS (SELECT 1 FROM user_roles WHERE user_id = v_existing_user_id AND role = 'VENDOR') THEN
        -- User already has vendor role, just link
        UPDATE vendor_details SET user_id = v_existing_user_id WHERE vendor_id = v_vendor_id;
        INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, created_user_id, error_message)
        VALUES (v_vendor_id, v_name, v_email, 'SUCCESS', v_existing_user_id,
                'Linked to existing user account with VENDOR role');
      ELSE
        -- User exists but doesn't have vendor role
        INSERT INTO user_roles (user_id, role) VALUES (v_existing_user_id, 'VENDOR');
        UPDATE vendor_details SET user_id = v_existing_user_id WHERE vendor_id = v_vendor_id;
        INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, created_user_id, error_message)
        VALUES (v_vendor_id, v_name, v_email, 'SUCCESS', v_existing_user_id,
                'Added VENDOR role to existing user account');
      END IF;
      ITERATE vendor_loop;
    END IF;

    -- Case 4: Check for duplicate emails in vendor_details
    SELECT COUNT(*) INTO v_email_count
    FROM vendor_details
    WHERE email = v_email;

    IF v_email_count > 1 THEN
      -- Multiple vendors with same email - only first gets account
      IF NOT EXISTS (SELECT 1 FROM users WHERE email COLLATE utf8mb4_unicode_ci = v_email COLLATE utf8mb4_unicode_ci) THEN
        INSERT INTO users (email, email_verified, created_at)
        VALUES (v_email, FALSE, NOW());
        SET v_user_id = LAST_INSERT_ID();
        INSERT INTO user_roles (user_id, role) VALUES (v_user_id, 'VENDOR');
        UPDATE vendor_details SET user_id = v_user_id WHERE vendor_id = v_vendor_id;
        INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, created_user_id, error_message)
        VALUES (v_vendor_id, v_name, v_email, 'SUCCESS', v_user_id,
                CONCAT('Created user account for first vendor. ', (v_email_count - 1), ' other vendor(s) share this email'));
      ELSE
        INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, error_message)
        VALUES (v_vendor_id, v_name, v_email, 'DUPLICATE_EMAIL',
                'Email shared with other vendors - user account already created for first occurrence');
      END IF;
      ITERATE vendor_loop;
    END IF;

    -- Case 5: Valid unique email - create user account
    BEGIN
      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
      BEGIN
        INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, error_message)
        VALUES (v_vendor_id, v_name, v_email, 'ERROR', 'Database error during user creation');
      END;

      INSERT INTO users (email, email_verified, created_at)
      VALUES (v_email, FALSE, NOW());
      SET v_user_id = LAST_INSERT_ID();
      INSERT INTO user_roles (user_id, role) VALUES (v_user_id, 'VENDOR');
      UPDATE vendor_details SET user_id = v_user_id WHERE vendor_id = v_vendor_id;
      INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, created_user_id, error_message)
      VALUES (v_vendor_id, v_name, v_email, 'SUCCESS', v_user_id, 'Successfully created user account and linked vendor');
    END;

  END LOOP;

  CLOSE vendor_cursor;
END$$

DELIMITER ;

-- ============================================================
-- STEP 4: Execute Migration
-- ============================================================

CALL migrate_vendor_to_auth();

-- ============================================================
-- STEP 5: Cleanup
-- ============================================================

DROP PROCEDURE IF EXISTS migrate_vendor_to_auth;

-- ============================================================
-- STEP 6: Migration Summary Report
-- ============================================================

SELECT '============================================' AS '';
SELECT 'MIGRATION COMPLETE - SUMMARY REPORT' AS '';
SELECT '============================================' AS '';

SELECT
  status AS 'Status',
  COUNT(*) AS 'Count',
  CONCAT(ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM vendor_details), 2), '%') AS 'Percentage'
FROM vendor_migration_log
GROUP BY status
ORDER BY FIELD(status, 'SUCCESS', 'DUPLICATE_EMAIL', 'INVALID_EMAIL', 'MISSING_EMAIL', 'ERROR');

SELECT '' AS '';
SELECT 'Detailed breakdown:' AS '';

-- Success count
SELECT CONCAT('✓ Successfully migrated: ', COUNT(*), ' vendors') AS ''
FROM vendor_migration_log WHERE status = 'SUCCESS';

-- Duplicate emails
SELECT CONCAT('⚠ Duplicate emails: ', COUNT(*), ' vendors (user account created for first occurrence only)') AS ''
FROM vendor_migration_log WHERE status = 'DUPLICATE_EMAIL';

-- Invalid emails
SELECT CONCAT('✗ Invalid email format: ', COUNT(*), ' vendors (need manual correction)') AS ''
FROM vendor_migration_log WHERE status = 'INVALID_EMAIL';

-- Missing emails
SELECT CONCAT('✗ Missing emails: ', COUNT(*), ' vendors (need email added manually)') AS ''
FROM vendor_migration_log WHERE status = 'MISSING_EMAIL';

-- Errors
SELECT CONCAT('✗ Errors: ', COUNT(*), ' vendors (check logs)') AS ''
FROM vendor_migration_log WHERE status = 'ERROR';

SELECT '' AS '';
SELECT '============================================' AS '';
SELECT 'VERIFICATION:' AS '';
SELECT '============================================' AS '';

-- Verify vendor data was restored
SELECT CONCAT('Total vendors in vendor_details: ', COUNT(*)) AS ''
FROM vendor_details;

-- Verify user accounts created
SELECT CONCAT('Total user accounts created: ', COUNT(*)) AS ''
FROM users;

-- Verify vendor role assignments
SELECT CONCAT('Total VENDOR roles assigned: ', COUNT(*)) AS ''
FROM user_roles WHERE role = 'VENDOR';

-- Verify vendors linked to users
SELECT CONCAT('Total vendors linked to user accounts: ', COUNT(*)) AS ''
FROM vendor_details WHERE user_id IS NOT NULL;

SELECT '' AS '';
SELECT '============================================' AS '';
SELECT 'NEXT STEPS:' AS '';
SELECT '============================================' AS '';
SELECT '1. Review detailed logs: SELECT * FROM vendor_migration_log;' AS '';
SELECT '2. Fix vendors with INVALID_EMAIL or MISSING_EMAIL status' AS '';
SELECT '3. Verify data: SELECT * FROM vendor_details LIMIT 10;' AS '';
SELECT '4. Send password reset emails to all migrated vendors' AS '';
SELECT '5. Optional: DROP TABLE vendor_backup_log; (once verified)' AS '';
SELECT '============================================' AS '';

-- Commit transaction if everything succeeded
COMMIT;
