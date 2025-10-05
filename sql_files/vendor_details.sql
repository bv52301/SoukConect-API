--
-- Database: `soukconnect`
--

-- --------------------------------------------------------

--
-- Table structure for table `vendor_details`
--

CREATE TABLE `vendor_details` (
  `vendor_id` bigint(20) PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `supportedCategories` longtext CHARACTER SET utf8mb4,
  /*
	  {
	  [ 
	   {
		Cuisinename : ""Asia"",
		Category : ""Indian"",
		SubCategory : ""SouthIndian"",
		regionCategory : ""TN"",
	   },
	   {
		Cuisinename : ""Asia"",
		Category : ""Indian"",
		SubCategory : ""NorthIndian"",
		regionCategory : ""Marathi"",
	   },
	  ]
	 }
  */

  `image` varchar(300) DEFAULT NULL,
  `address1` varchar(100) DEFAULT NULL,
  `address2` varchar(100) DEFAULT NULL,
  `state` varchar(100) DEFAULT NULL,
  `landmark` varchar(255) DEFAULT NULL,
  `pincode` varchar(15) DEFAULT NULL,
  `contact_name` varchar(100) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  -- Basic JSON validity checks (enforced in MySQL 8.0.16+)
  CONSTRAINT chk_supported_json_valid CHECK (JSON_VALID(supportedCategories)),
  CONSTRAINT chk_supported_is_array   CHECK (JSON_TYPE(supportedCategories) = 'ARRAY')
);
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
							
