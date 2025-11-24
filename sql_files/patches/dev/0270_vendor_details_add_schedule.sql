-- Patch 0270 (dev): Add schedule column to vendor_details table
-- This adds the same schedule functionality available in products to vendors
-- Schedule supports weekly_schedules, dates, and blackout periods

ALTER TABLE `vendor_details`
ADD COLUMN `schedule` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
ADD COLUMN `schedule_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp();

-- Add JSON validity constraint
ALTER TABLE `vendor_details`
ADD CONSTRAINT chk_vendor_schedule_json_valid CHECK (JSON_VALID(schedule));

-- Optional: Ensure schedule has at least one of the expected keys when present
ALTER TABLE `vendor_details`
ADD CONSTRAINT chk_vendor_schedule_has_any CHECK (
  schedule IS NULL OR
  JSON_CONTAINS_PATH(schedule, 'one',
    '$.weekly_schedules', '$.dates', '$.blackout'
  )
);

-- Schedule JSON structure example:
/*
{
    "weekly_schedules": [
        {
            "day_of_week": ["Mon", "Tue", "Wed"],
            "start": "09:00",
            "end": "17:00",
            "tz": "Asia/Singapore"
        }
    ],
    "dates": [
        {
            "date": "2025-12-24",
            "start": "10:00",
            "end": "14:00",
            "tz": "Asia/Singapore"
        }
    ],
    "blackout": ["2025-12-26", "2025-12-27"]
}
*/
