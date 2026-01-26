-- ============================================================================
-- Payments Table - Comprehensive payment processing for multi-vendor marketplace
-- Supports: Customer payments, vendor payouts, refunds, chargebacks, settlements
-- ============================================================================

CREATE TABLE IF NOT EXISTS payments (
    -- Primary Key
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- ==================== RELATIONSHIPS ====================
    -- Order (optional - payments can exist without orders for tips, fees, etc.)
    order_id BIGINT NULL,

    -- Customer making the payment
    customer_id BIGINT NULL,

    -- Vendor receiving the payment (for marketplace multi-vendor support)
    vendor_id BIGINT NULL,

    -- Parent payment (for refunds/chargebacks linked to original payment)
    parent_payment_id BIGINT NULL,

    -- ==================== PAYMENT TYPE ====================
    payment_type ENUM(
        'PURCHASE',         -- Standard purchase
        'REFUND',           -- Full refund of previous payment
        'PARTIAL_REFUND',   -- Partial refund
        'CHARGEBACK',       -- Disputed/chargeback
        'PAYOUT',           -- Payout to vendor
        'TRANSFER',         -- Internal transfer
        'ADJUSTMENT',       -- Manual adjustment
        'FEE',              -- Fee collection
        'TIP'               -- Tip payment
    ) NOT NULL DEFAULT 'PURCHASE',

    -- ==================== CURRENCY & AMOUNTS ====================
    currency VARCHAR(3) NOT NULL DEFAULT 'MAD',
    amount DECIMAL(12,2) NOT NULL,
    original_amount DECIMAL(12,2) NULL,
    discount_amount DECIMAL(12,2) NULL,
    tax_amount DECIMAL(12,2) NULL,
    tip_amount DECIMAL(12,2) NULL,
    delivery_fee DECIMAL(12,2) NULL,
    service_fee DECIMAL(12,2) NULL,

    -- ==================== PLATFORM/VENDOR SPLIT ====================
    platform_fee DECIMAL(12,2) NULL,
    platform_fee_percent DECIMAL(5,2) NULL,
    vendor_amount DECIMAL(12,2) NULL,

    -- ==================== MULTI-CURRENCY SUPPORT ====================
    exchange_rate DECIMAL(12,6) NULL,
    settled_currency VARCHAR(3) NULL,
    settled_amount DECIMAL(12,2) NULL,

    -- ==================== PAYMENT METHOD & GATEWAY ====================
    payment_method ENUM(
        'CASH',
        'CARD',
        'DEBIT_CARD',
        'CREDIT_CARD',
        'WALLET',
        'BANK_TRANSFER',
        'DIRECT_DEBIT',
        'PAYNOW',
        'APPLE_PAY',
        'GOOGLE_PAY',
        'PAYPAL',
        'COD',              -- Cash on delivery
        'VOUCHER',
        'STORE_CREDIT',
        'CRYPTO',
        'OTHERS'
    ) NOT NULL,
    payment_gateway VARCHAR(50) NULL,
    gateway_payment_id VARCHAR(255) NULL,
    gateway_charge_id VARCHAR(255) NULL,
    gateway_response_code VARCHAR(50) NULL,
    gateway_response_message VARCHAR(500) NULL,

    -- ==================== CARD DETAILS (masked/tokenized) ====================
    card_brand VARCHAR(30) NULL,
    card_last_four VARCHAR(4) NULL,
    card_exp_month VARCHAR(2) NULL,
    card_exp_year VARCHAR(4) NULL,
    card_holder_name VARCHAR(255) NULL,
    card_fingerprint VARCHAR(255) NULL,

    -- ==================== 3D SECURE / AUTHENTICATION ====================
    auth_status ENUM(
        'NOT_REQUIRED',
        'PENDING',
        'SUCCESSFUL',
        'FAILED',
        'ATTEMPTED',
        'UNAVAILABLE'
    ) NULL,
    auth_value VARCHAR(255) NULL,
    auth_eci VARCHAR(10) NULL,

    -- ==================== PAYMENT STATUS ====================
    status ENUM(
        'PENDING',            -- Awaiting processing
        'REQUIRES_ACTION',    -- Requires customer action (3DS, etc.)
        'PROCESSING',         -- Being processed
        'AUTHORIZED',         -- Authorized but not captured
        'CAPTURED',           -- Successfully captured
        'COMPLETED',          -- Fully completed
        'FAILED',             -- Failed
        'CANCELLED',          -- Cancelled
        'REFUNDED',           -- Fully refunded
        'PARTIALLY_REFUNDED', -- Partially refunded
        'DISPUTED',           -- Under dispute
        'EXPIRED'             -- Authorization expired
    ) NOT NULL DEFAULT 'PENDING',
    status_reason VARCHAR(500) NULL,

    -- ==================== VENDOR PAYOUT TRACKING ====================
    vendor_payout_status ENUM(
        'PENDING',
        'SCHEDULED',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'ON_HOLD',
        'CANCELLED'
    ) NULL,
    vendor_payout_reference VARCHAR(255) NULL,
    vendor_payout_date TIMESTAMP NULL,

    -- ==================== REFUND TRACKING ====================
    refunded_amount DECIMAL(12,2) NULL,
    refund_reason VARCHAR(500) NULL,
    refund_date TIMESTAMP NULL,

    -- ==================== SETTLEMENT ====================
    settlement_status ENUM(
        'PENDING',
        'PROCESSING',
        'SETTLED',
        'FAILED'
    ) NULL,
    settlement_reference VARCHAR(255) NULL,
    settlement_date DATE NULL,

    -- ==================== FRAUD & RISK ====================
    risk_score INT NULL,
    fraud_status ENUM(
        'NOT_CHECKED',
        'APPROVED',
        'REVIEW',
        'REJECTED',
        'BLOCKED'
    ) NULL,
    fraud_reason VARCHAR(500) NULL,

    -- ==================== PAYER INFO ====================
    payer_email VARCHAR(255) NULL,
    payer_phone VARCHAR(50) NULL,
    payer_name VARCHAR(255) NULL,

    -- ==================== BILLING ADDRESS ====================
    billing_address_line1 VARCHAR(255) NULL,
    billing_address_line2 VARCHAR(255) NULL,
    billing_city VARCHAR(100) NULL,
    billing_state VARCHAR(100) NULL,
    billing_postal_code VARCHAR(20) NULL,
    billing_country VARCHAR(2) NULL,

    -- ==================== DEVICE & SESSION INFO (fraud detection) ====================
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    device_fingerprint VARCHAR(255) NULL,
    session_id VARCHAR(255) NULL,

    -- ==================== IDEMPOTENCY & REFERENCES ====================
    idempotency_key VARCHAR(255) NULL,
    external_reference VARCHAR(255) NULL,
    invoice_number VARCHAR(100) NULL,
    receipt_url VARCHAR(500) NULL,

    -- ==================== METADATA & NOTES ====================
    description VARCHAR(500) NULL,
    metadata JSON NULL,
    internal_notes TEXT NULL,
    failure_reason TEXT NULL,

    -- ==================== PROCESSING INFO ====================
    processed_by VARCHAR(100) NULL,
    processed_at TIMESTAMP NULL,
    captured_at TIMESTAMP NULL,

    -- ==================== RETRY & ATTEMPTS ====================
    attempt_count INT DEFAULT 0,
    last_attempt_at TIMESTAMP NULL,
    next_retry_at TIMESTAMP NULL,

    -- ==================== AUDIT TIMESTAMPS ====================
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- ==================== FOREIGN KEY CONSTRAINTS ====================
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id) ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_payment_vendor FOREIGN KEY (vendor_id)
        REFERENCES vendor_details(vendor_id) ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_payment_parent FOREIGN KEY (parent_payment_id)
        REFERENCES payments(payment_id) ON DELETE SET NULL ON UPDATE CASCADE,

    -- ==================== INDEXES ====================
    -- Primary lookups
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_customer (customer_id),
    INDEX idx_payments_vendor (vendor_id),
    INDEX idx_payments_parent (parent_payment_id),

    -- Status queries
    INDEX idx_payments_status (status),
    INDEX idx_payments_payout_status (vendor_payout_status),
    INDEX idx_payments_settlement_status (settlement_status),

    -- Gateway lookups
    INDEX idx_payments_gateway (payment_gateway),
    INDEX idx_payments_gateway_payment_id (gateway_payment_id),

    -- Idempotency & references
    UNIQUE INDEX idx_payments_idempotency_key (idempotency_key),
    INDEX idx_payments_external_reference (external_reference),

    -- Date-based queries
    INDEX idx_payments_created_at (created_at),
    INDEX idx_payments_processed_at (processed_at),
    INDEX idx_payments_settlement_date (settlement_date),

    -- Compound indexes for common queries
    INDEX idx_payments_vendor_status (vendor_id, status),
    INDEX idx_payments_customer_status (customer_id, status),
    INDEX idx_payments_vendor_payout (vendor_id, vendor_payout_status)
);

-- ============================================================================
-- Payment Audit Log - Track all payment state changes
-- ============================================================================

CREATE TABLE IF NOT EXISTS payment_audit_log (
    audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,
    old_values JSON NULL,
    new_values JSON NULL,
    changed_by VARCHAR(100) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_payment FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id) ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_payment_audit_payment (payment_id),
    INDEX idx_payment_audit_action (action),
    INDEX idx_payment_audit_created (created_at)
);
