package com.souk.common.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order", columnList = "order_id"),
                @Index(name = "idx_payments_customer", columnList = "customer_id"),
                @Index(name = "idx_payments_vendor", columnList = "vendor_id"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_payout_status", columnList = "vendor_payout_status"),
                @Index(name = "idx_payments_gateway", columnList = "payment_gateway"),
                @Index(name = "idx_payments_created_at", columnList = "created_at"),
                @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key"),
                @Index(name = "idx_payments_gateway_payment_id", columnList = "gateway_payment_id"),
                @Index(name = "idx_payments_parent", columnList = "parent_payment_id")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    // Parent payment for refunds/chargebacks
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_payment_id")
    private Payment parentPayment;

    // --- Payment Type ---
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType = PaymentType.PURCHASE;

    // --- Amount Fields ---
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "MAD";

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "original_amount", precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "tip_amount", precision = 12, scale = 2)
    private BigDecimal tipAmount;

    @Column(name = "delivery_fee", precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "service_fee", precision = 12, scale = 2)
    private BigDecimal serviceFee;

    // --- Platform/Vendor Split ---
    @Column(name = "platform_fee", precision = 12, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "platform_fee_percent", precision = 5, scale = 2)
    private BigDecimal platformFeePercent;

    @Column(name = "vendor_amount", precision = 12, scale = 2)
    private BigDecimal vendorAmount;

    // --- Multi-currency Support ---
    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "settled_currency", length = 3)
    private String settledCurrency;

    @Column(name = "settled_amount", precision = 12, scale = 2)
    private BigDecimal settledAmount;

    // --- Payment Method & Gateway ---
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @Column(name = "gateway_payment_id", length = 255)
    private String gatewayPaymentId;

    @Column(name = "gateway_charge_id", length = 255)
    private String gatewayChargeId;

    @Column(name = "gateway_response_code", length = 50)
    private String gatewayResponseCode;

    @Column(name = "gateway_response_message", length = 500)
    private String gatewayResponseMessage;

    // --- Card Details (for card payments) ---
    @Column(name = "card_brand", length = 30)
    private String cardBrand;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_exp_month", length = 2)
    private String cardExpMonth;

    @Column(name = "card_exp_year", length = 4)
    private String cardExpYear;

    @Column(name = "card_holder_name", length = 255)
    private String cardHolderName;

    @Column(name = "card_fingerprint", length = 255)
    private String cardFingerprint;

    // --- 3D Secure / Authentication ---
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_status", length = 30)
    private AuthenticationStatus authStatus;

    @Column(name = "auth_value", length = 255)
    private String authValue;

    @Column(name = "auth_eci", length = 10)
    private String authEci;

    // --- Payment Status ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    // --- Vendor Payout Tracking ---
    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_payout_status", length = 30)
    private PayoutStatus vendorPayoutStatus;

    @Column(name = "vendor_payout_reference", length = 255)
    private String vendorPayoutReference;

    @Column(name = "vendor_payout_date")
    private LocalDateTime vendorPayoutDate;

    // --- Refund Tracking ---
    @Column(name = "refunded_amount", precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    // --- Settlement ---
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 30)
    private SettlementStatus settlementStatus;

    @Column(name = "settlement_reference", length = 255)
    private String settlementReference;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    // --- Fraud & Risk ---
    @Column(name = "risk_score")
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", length = 30)
    private FraudStatus fraudStatus;

    @Column(name = "fraud_reason", length = 500)
    private String fraudReason;

    // --- Customer/Payer Info ---
    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    @Column(name = "payer_phone", length = 50)
    private String payerPhone;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    // --- Billing Address ---
    @Column(name = "billing_address_line1", length = 255)
    private String billingAddressLine1;

    @Column(name = "billing_address_line2", length = 255)
    private String billingAddressLine2;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Column(name = "billing_country", length = 2)
    private String billingCountry;

    // --- Device & Session Info (for fraud detection) ---
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    // --- Idempotency & References ---
    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    // --- Metadata & Notes ---
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // --- Processing Info ---
    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    // --- Retry & Attempts ---
    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    // --- Audit Timestamps ---
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // --- ENUMS ---
    public enum PaymentType {
        PURCHASE,           // Standard purchase
        REFUND,             // Refund of previous payment
        PARTIAL_REFUND,     // Partial refund
        CHARGEBACK,         // Disputed/chargeback
        PAYOUT,             // Payout to vendor
        TRANSFER,           // Internal transfer
        ADJUSTMENT,         // Manual adjustment
        FEE,                // Fee collection
        TIP                 // Tip payment
    }

    public enum PaymentStatus {
        PENDING,            // Awaiting processing
        REQUIRES_ACTION,    // Requires customer action (3DS, etc.)
        PROCESSING,         // Being processed
        AUTHORIZED,         // Authorized but not captured
        CAPTURED,           // Successfully captured
        COMPLETED,          // Fully completed
        FAILED,             // Failed (can retry)
        ABANDONED,          // Retries exhausted, given up (user can try different card/method)
        CANCELLED,          // Cancelled
        REFUNDED,           // Fully refunded
        PARTIALLY_REFUNDED, // Partially refunded
        DISPUTED,           // Under dispute
        EXPIRED             // Authorization expired
    }

    public enum PaymentMethod {
        CASH,
        CARD,
        DEBIT_CARD,
        CREDIT_CARD,
        WALLET,
        BANK_TRANSFER,
        DIRECT_DEBIT,
        PAYNOW,
        APPLE_PAY,
        GOOGLE_PAY,
        PAYPAL,
        COD,                // Cash on delivery
        VOUCHER,
        STORE_CREDIT,
        CRYPTO,
        OTHERS
    }

    public enum PayoutStatus {
        PENDING,
        SCHEDULED,
        PROCESSING,
        COMPLETED,
        FAILED,
        ON_HOLD,
        CANCELLED
    }

    public enum SettlementStatus {
        PENDING,
        PROCESSING,
        SETTLED,
        FAILED
    }

    public enum AuthenticationStatus {
        NOT_REQUIRED,
        PENDING,
        SUCCESSFUL,
        FAILED,
        ATTEMPTED,
        UNAVAILABLE
    }

    public enum FraudStatus {
        NOT_CHECKED,
        APPROVED,
        REVIEW,
        REJECTED,
        BLOCKED
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public Payment getParentPayment() { return parentPayment; }
    public void setParentPayment(Payment parentPayment) { this.parentPayment = parentPayment; }

    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTipAmount() { return tipAmount; }
    public void setTipAmount(BigDecimal tipAmount) { this.tipAmount = tipAmount; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }

    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }

    public BigDecimal getPlatformFeePercent() { return platformFeePercent; }
    public void setPlatformFeePercent(BigDecimal platformFeePercent) { this.platformFeePercent = platformFeePercent; }

    public BigDecimal getVendorAmount() { return vendorAmount; }
    public void setVendorAmount(BigDecimal vendorAmount) { this.vendorAmount = vendorAmount; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public String getSettledCurrency() { return settledCurrency; }
    public void setSettledCurrency(String settledCurrency) { this.settledCurrency = settledCurrency; }

    public BigDecimal getSettledAmount() { return settledAmount; }
    public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }

    public String getGatewayChargeId() { return gatewayChargeId; }
    public void setGatewayChargeId(String gatewayChargeId) { this.gatewayChargeId = gatewayChargeId; }

    public String getGatewayResponseCode() { return gatewayResponseCode; }
    public void setGatewayResponseCode(String gatewayResponseCode) { this.gatewayResponseCode = gatewayResponseCode; }

    public String getGatewayResponseMessage() { return gatewayResponseMessage; }
    public void setGatewayResponseMessage(String gatewayResponseMessage) { this.gatewayResponseMessage = gatewayResponseMessage; }

    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }

    public String getCardExpMonth() { return cardExpMonth; }
    public void setCardExpMonth(String cardExpMonth) { this.cardExpMonth = cardExpMonth; }

    public String getCardExpYear() { return cardExpYear; }
    public void setCardExpYear(String cardExpYear) { this.cardExpYear = cardExpYear; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getCardFingerprint() { return cardFingerprint; }
    public void setCardFingerprint(String cardFingerprint) { this.cardFingerprint = cardFingerprint; }

    public AuthenticationStatus getAuthStatus() { return authStatus; }
    public void setAuthStatus(AuthenticationStatus authStatus) { this.authStatus = authStatus; }

    public String getAuthValue() { return authValue; }
    public void setAuthValue(String authValue) { this.authValue = authValue; }

    public String getAuthEci() { return authEci; }
    public void setAuthEci(String authEci) { this.authEci = authEci; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }

    public PayoutStatus getVendorPayoutStatus() { return vendorPayoutStatus; }
    public void setVendorPayoutStatus(PayoutStatus vendorPayoutStatus) { this.vendorPayoutStatus = vendorPayoutStatus; }

    public String getVendorPayoutReference() { return vendorPayoutReference; }
    public void setVendorPayoutReference(String vendorPayoutReference) { this.vendorPayoutReference = vendorPayoutReference; }

    public LocalDateTime getVendorPayoutDate() { return vendorPayoutDate; }
    public void setVendorPayoutDate(LocalDateTime vendorPayoutDate) { this.vendorPayoutDate = vendorPayoutDate; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public LocalDateTime getRefundDate() { return refundDate; }
    public void setRefundDate(LocalDateTime refundDate) { this.refundDate = refundDate; }

    public SettlementStatus getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(SettlementStatus settlementStatus) { this.settlementStatus = settlementStatus; }

    public String getSettlementReference() { return settlementReference; }
    public void setSettlementReference(String settlementReference) { this.settlementReference = settlementReference; }

    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public FraudStatus getFraudStatus() { return fraudStatus; }
    public void setFraudStatus(FraudStatus fraudStatus) { this.fraudStatus = fraudStatus; }

    public String getFraudReason() { return fraudReason; }
    public void setFraudReason(String fraudReason) { this.fraudReason = fraudReason; }

    public String getPayerEmail() { return payerEmail; }
    public void setPayerEmail(String payerEmail) { this.payerEmail = payerEmail; }

    public String getPayerPhone() { return payerPhone; }
    public void setPayerPhone(String payerPhone) { this.payerPhone = payerPhone; }

    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }

    public String getBillingAddressLine1() { return billingAddressLine1; }
    public void setBillingAddressLine1(String billingAddressLine1) { this.billingAddressLine1 = billingAddressLine1; }

    public String getBillingAddressLine2() { return billingAddressLine2; }
    public void setBillingAddressLine2(String billingAddressLine2) { this.billingAddressLine2 = billingAddressLine2; }

    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }

    public String getBillingState() { return billingState; }
    public void setBillingState(String billingState) { this.billingState = billingState; }

    public String getBillingPostalCode() { return billingPostalCode; }
    public void setBillingPostalCode(String billingPostalCode) { this.billingPostalCode = billingPostalCode; }

    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
