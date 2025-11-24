-- Patch 0280 (dev): Add use_vendor_schedule flag to products table
-- This allows products to link to their vendor's schedule or maintain independent schedules
-- When use_vendor_schedule is true, the product uses the vendor's schedule
-- When false, the product uses its own schedule field

ALTER TABLE `products`
ADD COLUMN `use_vendor_schedule` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'If true, product uses vendor schedule; if false, uses own schedule';

-- Note: When use_vendor_schedule is true, the product's schedule field is ignored
-- and the vendor's schedule is used instead. When cloning from vendor,
-- use_vendor_schedule is set to false and the schedule is copied to the product.
