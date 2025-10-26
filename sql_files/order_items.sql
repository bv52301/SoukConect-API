CREATE TABLE order_items (
  order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id  BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,

  -- Per-item delivery preference
  requested_delivery_date DATE NULL,
  delivery_flexibility ENUM('STRICT','FLEXIBLE') DEFAULT 'FLEXIBLE',
  delivery_slot_start TIME NULL,
  delivery_slot_end TIME NULL,

  FOREIGN KEY (order_id)  REFERENCES orders(order_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
);
