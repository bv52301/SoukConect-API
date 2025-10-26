CREATE TABLE orders (
  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  address_id BIGINT NULL,  -- Optional: if you store delivery address separately
  total_amount DECIMAL(10,2) NOT NULL,
  status ENUM('PENDING', 'CONFIRMED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
  payment_method ENUM('CASH', 'CARD', 'WALLET', 'BANK_TRANSFER','PAYNOW','OTHERS') DEFAULT 'CARD',
  notes TEXT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id)
    REFERENCES customers(customer_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_order_address FOREIGN KEY (address_id)
    REFERENCES customer_addresses(address_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
);

CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);
