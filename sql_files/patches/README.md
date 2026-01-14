# Database Patches Directory

## Overview
This directory contains database migration patches organized by environment. Patches are SQL scripts that modify existing database schema or data.

## IMPORTANT: Patch Naming Convention

**ALL patches MUST follow the numerical sequence naming pattern:**

```
0XXX_descriptive_name.sql
```

### Rules:
1. **Start with 4-digit number** (e.g., 0200, 0300, 0310)
2. **Use increments of 10** to allow inserting patches between existing ones
3. **Use underscore-separated lowercase description** after the number
4. **Always use .sql extension**

### Examples:
- ✅ `0300_customers_add_user_id.sql`
- ✅ `0310_vendor_details_add_user_id.sql`
- ✅ `0320_products_add_inventory_tracking.sql`
- ❌ `PATCH_add_user_id.sql` (missing number prefix)
- ❌ `add_column.sql` (missing number prefix)
- ❌ `300_add_column.sql` (needs leading zero: 0300)

## Why Numerical Sequence?

The `install_sql_files.sh` script applies patches in **lexical order**. Without numerical prefixes, patches will execute in alphabetical order, which may break dependencies.

**Example of what goes wrong:**
```bash
# Without numbers (WRONG - alphabetical order):
PATCH_add_user_id_to_customers.sql      # Runs first
PATCH_add_user_id_to_vendor_details.sql # Runs second
add_foreign_keys.sql                     # Runs third (breaks ordering!)

# With numbers (CORRECT - sequential order):
0300_customers_add_user_id.sql          # Runs first
0310_vendor_details_add_user_id.sql     # Runs second
0320_add_foreign_keys.sql               # Runs third (correct order!)
```

## Directory Structure

```
sql_files/patches/
├── README.md (this file)
├── dev/          # Development environment patches
│   ├── 0200_vendor_details_add_description.sql
│   ├── 0210_products_add_description.sql
│   ├── 0300_customers_add_user_id.sql
│   └── 0310_vendor_details_add_user_id.sql
├── uat/          # UAT environment patches (cumulative: dev + uat)
│   └── ...
└── prod/         # Production environment patches (cumulative: dev + uat + prod)
    └── ...
```

## Environment Strategy

- **dev/**: All new patches start here
- **uat/**: Promoted patches from dev after testing
- **prod/**: Production-ready patches only

When building the db-installer with `-Dbundle.env=prod`, all three directories are included (dev + uat + prod).

## Finding the Next Patch Number

1. Check the highest number in your target environment directory:
   ```bash
   ls sql_files/patches/dev/ | sort | tail -1
   # Example output: 0310_vendor_details_add_user_id.sql
   ```

2. Add 10 for the next patch number:
   ```
   Last: 0310
   Next: 0320
   ```

3. Create your patch:
   ```bash
   touch sql_files/patches/dev/0320_your_description_here.sql
   ```

## Patch Writing Guidelines

### 1. Make Patches Idempotent
Patches should be safe to re-run. Use conditional checks:

```sql
-- Check if column exists before adding
SET @column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'your_table'
    AND COLUMN_NAME = 'your_column'
);

SET @sql = IF(@column_exists = 0,
  'ALTER TABLE your_table ADD COLUMN your_column VARCHAR(100)',
  'SELECT "Column already exists" AS Info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 2. Add Comments
Always document what the patch does and why:

```sql
-- --------------------------------------------------------
-- PATCH: Add inventory tracking to products
-- JIRA: SOUK-123
-- Author: John Doe
-- Date: 2026-01-13
--
-- Adds inventory_count and low_stock_threshold columns
-- to support inventory management feature
-- --------------------------------------------------------
```

### 3. Avoid Destructive Operations
- Don't drop tables or columns unless absolutely necessary
- Use `ALTER TABLE ... MODIFY COLUMN` instead of drop/recreate
- Back up data before destructive changes

### 4. Test Before Committing
```bash
# Test on local database first
mysql -u root -p soukconnect < sql_files/patches/dev/0320_your_patch.sql

# Verify changes
mysql -u root -p soukconnect -e "DESCRIBE your_table;"
```

## Common Mistakes to Avoid

### ❌ Using Invalid MySQL Syntax
```sql
-- WRONG (not supported in MySQL):
ALTER TABLE customers ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- CORRECT (use prepared statements):
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_NAME='customers' AND COLUMN_NAME='user_id');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE customers ADD COLUMN user_id BIGINT',
              'SELECT "Already exists"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

### ❌ Forgetting Foreign Key Dependencies
```sql
-- WRONG (will fail if user_id column doesn't exist):
ALTER TABLE customers
ADD CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users(user_id);

-- CORRECT (check if column exists first):
-- [Use conditional check as shown above]
```

### ❌ Missing Numerical Prefix
```sql
-- WRONG:
patches/dev/add_user_column.sql

-- CORRECT:
patches/dev/0320_add_user_column.sql
```

## Special Files

### MIGRATION_add_authentication_system.sql
Located in `sql_files/` (not in patches/), this is a **one-time migration script** for existing databases with data. It should NOT be run via `install_sql_files.sh`.

**Usage:**
```bash
# For existing databases with vendor data:
mysql -u root -p soukconnect < sql_files/MIGRATION_add_authentication_system.sql

# For new installations:
./install_sql_files.sh  # Uses base + patches instead
```

## Checklist for Adding a New Patch

- [ ] Find the next available number (last + 10)
- [ ] Create file with correct naming: `0XXX_description.sql`
- [ ] Place in correct environment directory (`dev/` for new patches)
- [ ] Add header comments explaining the change
- [ ] Make patch idempotent (safe to re-run)
- [ ] Test on local database
- [ ] Verify no syntax errors
- [ ] Commit to version control
- [ ] Update this README if adding new conventions

## AI Tool Instructions

**When creating new database patches:**
1. Always check existing patch numbers first
2. Use the next sequential number (increment by 10)
3. Follow the naming pattern: `0XXX_description.sql`
4. Never use non-numerical prefixes like "PATCH_"
5. Place new patches in `sql_files/patches/dev/` directory
6. Make patches idempotent using INFORMATION_SCHEMA checks
7. Test the syntax before committing

## Questions?

If you're unsure about:
- Which number to use → Check the last patch in your target directory
- Where to place a patch → Start with `dev/` directory
- How to make it idempotent → See examples in existing patches like `0300_customers_add_user_id.sql`

---

Last Updated: 2026-01-13
Maintained by: SoukConnect Development Team
