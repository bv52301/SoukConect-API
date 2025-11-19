-- Patch 0230 (dev): Multi-valued index on vendor_details.supportedCategories (JSON array of strings)
-- MySQL >= 8.0.17 required for multivalued indexes
-- Assumes supportedCategories like ["Indian","Thai", ...]
-- Optimized for predicates such as: 'Indian' MEMBER OF (supportedCategories)

CREATE INDEX `idx_vendor_supported_categories_mvi`
  ON `vendor_details` ((CAST(`supportedCategories` AS CHAR(100) ARRAY)));
