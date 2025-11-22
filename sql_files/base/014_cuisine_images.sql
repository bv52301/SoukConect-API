-- Cuisine images table for storing images by type (CUISINE, CATEGORY, SUBCATEGORY)
CREATE TABLE IF NOT EXISTS cuisine_images (
    cuisine_image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('CUISINE', 'CATEGORY', 'SUBCATEGORY') NOT NULL,
    name VARCHAR(200) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cuisine_image_type_name (type, name)
);
