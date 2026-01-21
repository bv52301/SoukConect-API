CREATE TABLE carts (
  cart_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NULL,
  session_id VARCHAR(100) NULL,

  status ENUM('ACTIVE','MERGED','CONVERTED','ABANDONED','EXPIRED') DEFAULT 'ACTIVE',

  expires_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id)
    REFERENCES customers(customer_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  INDEX idx_carts_customer   (customer_id),
  INDEX idx_carts_session    (session_id),
  INDEX idx_carts_status     (status),
  INDEX idx_carts_expires_at (expires_at)
);
