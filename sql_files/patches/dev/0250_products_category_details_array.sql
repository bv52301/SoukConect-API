-- Patch 0250 (dev): enforce category_details as an ARRAY of objects
-- Per element rules:
--   - must contain Cuisinename and Category
--   - Category must be a non-empty ARRAY
--   - SubCategory is optional; if present it must be an ARRAY (can be empty)

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

-- Add the new array-friendly check constraint
ALTER TABLE `products`
  ADD CONSTRAINT `chk_category_details_paths`
  CHECK (
    JSON_TYPE(`category_details`) = 'ARRAY'
    AND JSON_LENGTH(`category_details`) > 0
    AND JSON_CONTAINS_PATH(`category_details`, 'one', '$[*].Cuisinename', '$[*].Category')
    AND JSON_TYPE(JSON_EXTRACT(`category_details`, '$[0].Category')) = 'ARRAY'
    AND JSON_LENGTH(JSON_EXTRACT(`category_details`, '$[0].Category')) > 0
    AND (
      NOT JSON_CONTAINS_PATH(`category_details`, 'one', '$[0].SubCategory')
      OR JSON_TYPE(JSON_EXTRACT(`category_details`, '$[0].SubCategory')) = 'ARRAY'
    )
  );
