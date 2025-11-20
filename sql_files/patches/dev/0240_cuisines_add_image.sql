-- Patch 0240: add optional image column to cuisines
ALTER TABLE `cuisines`
    ADD COLUMN `image` VARCHAR(300) NULL AFTER `region`;
