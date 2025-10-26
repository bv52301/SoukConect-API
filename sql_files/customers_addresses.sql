CREATE TABLE customer_addresses (
  address_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  type ENUM('HOME', 'OFFICE', 'SHIPPING', 'BILLING', 'OTHER') DEFAULT 'HOME',
  street VARCHAR(255),
  unit VARCHAR(50),
  city VARCHAR(100),
  postal VARCHAR(20),
  country VARCHAR(50),
  is_default BOOLEAN DEFAULT FALSE,
  metadata JSON DEFAULT NULL,  -- Extra details like “delivery instructions”
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_customer_address_customer FOREIGN KEY (customer_id)
    REFERENCES customers(customer_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  INDEX idx_customer_address_customer (customer_id),
  INDEX idx_customer_address_default (is_default)
);
