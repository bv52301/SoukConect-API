-- ============================================================
-- MIGRATION SCRIPT 3: Complete Remaining Vendor Migration
-- ============================================================
--
-- PURPOSE: Migrates the remaining 48 vendors that weren't processed
--
-- PREREQUISITES:
--   - MIGRATION_02 already run (only migrated 1 vendor)
--   - 48 vendors still have user_id = NULL
--
-- HOW TO RUN:
--   mysql -u root -p soukconect < MIGRATION_03_complete_vendor_migration.sql
--
-- ============================================================

USE soukconect;

-- Process remaining vendors without user_id
-- Using a simple INSERT...SELECT approach instead of cursor

-- For each vendor without user_id:
-- 1. Create user account
-- 2. Assign VENDOR role
-- 3. Link vendor to user via user_id
-- 4. Log the result

INSERT INTO users (email, email_verified, created_at)
SELECT DISTINCT email COLLATE utf8mb4_unicode_ci, FALSE, NOW()
FROM vendor_details
WHERE user_id IS NULL
  AND email IS NOT NULL
  AND TRIM(email) != ''
  AND email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'
  AND email COLLATE utf8mb4_unicode_ci NOT IN (SELECT email FROM users);

-- Assign VENDOR role to all new users
INSERT INTO user_roles (user_id, role)
SELECT u.user_id, 'VENDOR'
FROM users u
WHERE u.user_id NOT IN (SELECT user_id FROM user_roles WHERE role = 'VENDOR');

-- Link vendors to their user accounts
UPDATE vendor_details vd
INNER JOIN users u ON u.email COLLATE utf8mb4_unicode_ci = vd.email COLLATE utf8mb4_unicode_ci
SET vd.user_id = u.user_id
WHERE vd.user_id IS NULL;

-- Log the newly migrated vendors
INSERT INTO vendor_migration_log (vendor_id, vendor_name, vendor_email, status, created_user_id, error_message)
SELECT vd.vendor_id, vd.name, vd.email, 'SUCCESS', vd.user_id, 'Migrated in batch completion script'
FROM vendor_details vd
WHERE vd.user_id IS NOT NULL
  AND vd.vendor_id NOT IN (SELECT vendor_id FROM vendor_migration_log WHERE vendor_id IS NOT NULL);

-- Display results
SELECT '============================================' AS '';
SELECT 'BATCH MIGRATION COMPLETE' AS '';
SELECT '============================================' AS '';

SELECT COUNT(*) as total_vendors_now_linked
FROM vendor_details
WHERE user_id IS NOT NULL;

SELECT COUNT(*) as total_user_accounts
FROM users;

SELECT COUNT(*) as total_vendor_roles
FROM user_roles
WHERE role = 'VENDOR';

SELECT COUNT(*) as vendors_still_unlinked
FROM vendor_details
WHERE user_id IS NULL;

SELECT '============================================' AS '';
