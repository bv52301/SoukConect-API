-- Patch 0220 (dev): update products.category_details check constraint
-- Requirement: Remove SubCategory and regionCategory from the enforced paths
-- Result: Only Cuisinename and Category must be present in category_details JSON

SET @schema := DATABASE();

-- Drop existing check constraint if present
SET @drop_stmt := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.TABLE_CONSTRAINTS tc
      WHERE tc.CONSTRAINT_SCHEMA = @schema
        AND tc.TABLE_NAME = 'products'
        AND tc.CONSTRAINT_TYPE = 'CHECK'
        AND tc.CONSTRAINT_NAME = 'chk_category_details_paths'
    ),
    'ALTER TABLE `products` DROP CHECK `chk_category_details_paths`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @drop_stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add the new (reduced) check constraint
ALTER TABLE `products`
  ADD CONSTRAINT `chk_category_details_paths`
  CHECK (
    JSON_CONTAINS_PATH(`category_details`, 'all',
      '$.Cuisinename', '$.Category'
    )
  );

