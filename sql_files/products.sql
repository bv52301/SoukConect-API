-- Default Database: `soukconnect`
--
-- --------------------------------------------------------
--
-- Table structure for table `products`
--
CREATE TABLE `products` (
  `product_id` bigint(20) PRIMARY KEY AUTO_INCREMENT,
  `sku` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `vendor_id` bigint(20) NOT NULL,
  `category_details` longtext CHARACTER SET utf8mb4,
    /*
	  {
	 Cuisinename : "Asia",
	 Category : "Indian",
	 SubCategory : "SouthIndian",
	 regionCategory : "TN",
	  }
  */
  `product_image` longtext CHARACTER SET utf8mb4,
  /*
	{ "images" : [ "url1","url2"] }
  */
  `price` decimal(10,2) NOT NULL,
  `is_available` tinyint(1) NOT NULL DEFAULT 1,
  `schedule` longtext CHARACTER SET utf8mb4 ,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `schedule_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  /*
	  {
	   ""weekly_schedules"": [
		{
		 ""day_of_week"": [
		  ""Mon"",
		  ""Tue"",
		  ""Wed""
		 ],
		 ""start"": ""09:00"",
		 ""end"": ""17:00"",
		 ""stock"" : 100
		 ""tz"": ""Asia/Singapore""
		},
		{
		 ""day_of_week"": [
		  ""Fri""
		 ],
		 ""start"": ""09:00"",
		 ""end"": ""20:00"",
		 ""stock"" : 100,
		 ""tz"": ""Asia/Singapore""
		}
	   ],
	   ""dates"": [
		{
		 ""date"": ""2025-12-24"",
		 ""start"": ""10:00"",
		 ""end"": ""14:00"",
		 ""stock"" : 100
		 ""tz"": ""Asia/Singapore""
		},
		{
		 ""date"": ""2025-12-25"",
		 ""start"": ""10:00"",
		 ""end"": ""14:00"",
		 ""stock"" : 100
		 ""tz"": ""Asia/Singapore""
		}
	   ],
	   ""blackout"": [
		""2025-12-26"",
		""2025-12-27"",
		""2025-12-28""
	   ]
	 }
  */
  CONSTRAINT uq_product_sku UNIQUE (sku),
  CONSTRAINT chk_category_details_json_valid CHECK (JSON_VALID(category_details)),
  CONSTRAINT chk_product_image_json_valid     CHECK (product_image IS NULL OR JSON_VALID(product_image)),
  CONSTRAINT chk_schedule_json_valid          CHECK (JSON_VALID(schedule)),

  -- Minimal expected shape for category_details
  CONSTRAINT chk_category_details_paths CHECK (
    JSON_CONTAINS_PATH(category_details, 'all',
      '$.Cuisinename', '$.Category', '$.SubCategory', '$.regionCategory'
    )
  ),
  -- If you always expect product_image = {""""images"""":[...]} and that it's an array:
  CONSTRAINT chk_product_images_array CHECK (
    product_image IS NULL
    OR JSON_CONTAINS_PATH(product_image, 'one', '$.images')
       AND JSON_TYPE(JSON_EXTRACT(product_image,'$.images')) = 'ARRAY'
  ),

  -- Optional minimal shape for schedule:
  -- must have one of weekly_schedules/dates/blackout
  CONSTRAINT chk_schedule_has_any CHECK (
    JSON_CONTAINS_PATH(schedule, 'one',
      '$.weekly_schedules', '$.dates', '$.blackout'
    )
  ),

  CONSTRAINT fk_product_vendor
    FOREIGN KEY (vendor_id) REFERENCES vendor_details(vendor_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);