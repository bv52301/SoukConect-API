-- Patch 0260 (dev): add multi-valued indexes on products.category_details for Category and SubCategory
-- Supports array-of-objects payload (Category/SubCategory are arrays inside each object).

SET @schema := DATABASE();

-- Drop existing MVIs if present (old paths)
SET @drop_idx1 := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE TABLE_SCHEMA = @schema
        AND TABLE_NAME = 'products'
        AND INDEX_NAME = 'idx_products_category_mvi'
    ),
    'ALTER TABLE `products` DROP INDEX `idx_products_category_mvi`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @drop_idx1; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @drop_idx2 := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE TABLE_SCHEMA = @schema
        AND TABLE_NAME = 'products'
        AND INDEX_NAME = 'idx_products_subcategory_mvi'
    ),
    'ALTER TABLE `products` DROP INDEX `idx_products_subcategory_mvi`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @drop_idx2; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Create MVIs on nested arrays within array-of-objects
CREATE INDEX `idx_products_category_mvi`
  ON `products` ((CAST(JSON_EXTRACT(`category_details`, '$[*].Category[*]') AS CHAR(100) ARRAY)));

CREATE INDEX `idx_products_subcategory_mvi`
  ON `products` ((CAST(JSON_EXTRACT(`category_details`, '$[*].SubCategory[*]') AS CHAR(100) ARRAY)));
