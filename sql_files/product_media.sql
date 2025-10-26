CREATE TABLE product_media (
  media_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  media_url VARCHAR(1000) NOT NULL,
  media_type ENUM('IMAGE', 'VIDEO') DEFAULT 'IMAGE',
  description TEXT,
  storage_provider ENUM('LOCAL', 'S3', 'CLOUDFLARE', 'GCP') DEFAULT 'LOCAL',
  mime_type VARCHAR(100),
  width INT,
  height INT,
  duration_seconds INT,
  resolution VARCHAR(50),
  size_kb INT,
  validation_status ENUM('PENDING', 'VALIDATED', 'REJECTED') DEFAULT 'PENDING',
  validation_error TEXT,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_media_product FOREIGN KEY (product_id)
    REFERENCES products(product_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  INDEX idx_product_media_product_id (product_id),
  INDEX idx_product_media_status (validation_status),
  INDEX idx_product_media_type (media_type)
);
