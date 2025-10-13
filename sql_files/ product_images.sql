CREATE TABLE product_images (
  image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,

  -- Basic file info
  image_url VARCHAR(1000) NOT NULL,  -- Extended to 1000 for long signed URLs
  storage_provider ENUM('LOCAL', 'S3', 'CLOUDFLARE', 'GCP') DEFAULT 'LOCAL',
  mime_type VARCHAR(100),

  -- Metadata for validation / analytics
  width INT,
  height INT,
  size_kb INT,

  -- Validation workflow
  validation_status ENUM('PENDING', 'VALIDATED', 'REJECTED') DEFAULT 'PENDING',
  validation_error TEXT NULL,   -- Optional message from validation service

  -- Audit info
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  -- Foreign key relationship
  CONSTRAINT fk_image_product FOREIGN KEY (product_id)
    REFERENCES products(product_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  -- Indexes for performance
  INDEX idx_product_images_product_id (product_id),
  INDEX idx_product_images_status (validation_status),
  INDEX idx_product_images_uploaded_at (uploaded_at)
);
