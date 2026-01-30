package com.souk.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Response object from payment gateway operations.
 * Standardized response regardless of which gateway was used.
 */
public class GatewayResponse {

    // Status
    private boolean success;
    private GatewayStatus status;
    private String errorCode;
    private String errorMessage;

    // Gateway identifiers
    private String gatewayName;           // STRIPE, CMI, etc.
    private String gatewayPaymentId;      // Gateway's payment/transaction ID
    private String gatewayChargeId;       // Gateway's charge ID (if different)
    private String gatewayCustomerId;     // Gateway's customer ID

    // Amount details
    private BigDecimal amountCharged;
    private BigDecimal amountRefunded;
    private String currency;
    private BigDecimal fee;               // Gateway processing fee

    // Card details (masked)
    private String cardBrand;
    private String cardLastFour;
    private String cardExpMonth;
    private String cardExpYear;
    private String cardFingerprint;

    // 3D Secure
    private AuthenticationStatus authStatus;
    private String authUrl;               // URL for 3DS redirect if needed
    private String authEci;

    // Risk/Fraud
    private Integer riskScore;
    private String riskLevel;             // LOW, MEDIUM, HIGH
    private String fraudMessage;

    // Receipt
    private String receiptUrl;
    private String receiptNumber;

    // Timestamps
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant processedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant capturedAt;

    // Raw response (for debugging/logging)
    private String rawResponse;
    private Map<String, Object> additionalData;

    // --- Enums ---
    public enum GatewayStatus {
        SUCCEEDED,          // Payment completed successfully
        PENDING,            // Payment is processing
        REQUIRES_ACTION,    // Needs customer action (3DS, redirect, etc.)
        REQUIRES_CAPTURE,   // Authorized but needs capture
        FAILED,             // Payment failed
        CANCELLED,          // Payment was cancelled
        REFUNDED,           // Payment was refunded
        PARTIALLY_REFUNDED  // Partial refund applied
    }

    public enum AuthenticationStatus {
        NOT_REQUIRED,
        PENDING,
        SUCCEEDED,
        FAILED,
        ATTEMPTED
    }

    // --- Static factory methods ---
    public static GatewayResponse success(String gatewayName, String gatewayPaymentId) {
        GatewayResponse response = new GatewayResponse();
        response.success = true;
        response.status = GatewayStatus.SUCCEEDED;
        response.gatewayName = gatewayName;
        response.gatewayPaymentId = gatewayPaymentId;
        response.processedAt = Instant.now();
        return response;
    }

    public static GatewayResponse requiresAction(String gatewayName, String gatewayPaymentId, String authUrl) {
        GatewayResponse response = new GatewayResponse();
        response.success = false;
        response.status = GatewayStatus.REQUIRES_ACTION;
        response.gatewayName = gatewayName;
        response.gatewayPaymentId = gatewayPaymentId;
        response.authUrl = authUrl;
        response.authStatus = AuthenticationStatus.PENDING;
        return response;
    }

    public static GatewayResponse failure(String gatewayName, String errorCode, String errorMessage) {
        GatewayResponse response = new GatewayResponse();
        response.success = false;
        response.status = GatewayStatus.FAILED;
        response.gatewayName = gatewayName;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        response.processedAt = Instant.now();
        return response;
    }

    // --- Builder for complex responses ---
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final GatewayResponse response = new GatewayResponse();

        public Builder success(boolean success) { response.success = success; return this; }
        public Builder status(GatewayStatus status) { response.status = status; return this; }
        public Builder errorCode(String code) { response.errorCode = code; return this; }
        public Builder errorMessage(String msg) { response.errorMessage = msg; return this; }
        public Builder gatewayName(String name) { response.gatewayName = name; return this; }
        public Builder gatewayPaymentId(String id) { response.gatewayPaymentId = id; return this; }
        public Builder gatewayChargeId(String id) { response.gatewayChargeId = id; return this; }
        public Builder gatewayCustomerId(String id) { response.gatewayCustomerId = id; return this; }
        public Builder amountCharged(BigDecimal amount) { response.amountCharged = amount; return this; }
        public Builder amountRefunded(BigDecimal amount) { response.amountRefunded = amount; return this; }
        public Builder currency(String currency) { response.currency = currency; return this; }
        public Builder fee(BigDecimal fee) { response.fee = fee; return this; }
        public Builder cardBrand(String brand) { response.cardBrand = brand; return this; }
        public Builder cardLastFour(String last4) { response.cardLastFour = last4; return this; }
        public Builder cardExpMonth(String month) { response.cardExpMonth = month; return this; }
        public Builder cardExpYear(String year) { response.cardExpYear = year; return this; }
        public Builder cardFingerprint(String fp) { response.cardFingerprint = fp; return this; }
        public Builder authStatus(AuthenticationStatus status) { response.authStatus = status; return this; }
        public Builder authUrl(String url) { response.authUrl = url; return this; }
        public Builder authEci(String eci) { response.authEci = eci; return this; }
        public Builder riskScore(Integer score) { response.riskScore = score; return this; }
        public Builder riskLevel(String level) { response.riskLevel = level; return this; }
        public Builder fraudMessage(String msg) { response.fraudMessage = msg; return this; }
        public Builder receiptUrl(String url) { response.receiptUrl = url; return this; }
        public Builder receiptNumber(String num) { response.receiptNumber = num; return this; }
        public Builder processedAt(Instant time) { response.processedAt = time; return this; }
        public Builder capturedAt(Instant time) { response.capturedAt = time; return this; }
        public Builder rawResponse(String raw) { response.rawResponse = raw; return this; }
        public Builder additionalData(Map<String, Object> data) { response.additionalData = data; return this; }

        public GatewayResponse build() { return response; }
    }

    // --- Getters & Setters ---
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public GatewayStatus getStatus() { return status; }
    public void setStatus(GatewayStatus status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getGatewayName() { return gatewayName; }
    public void setGatewayName(String gatewayName) { this.gatewayName = gatewayName; }

    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }

    public String getGatewayChargeId() { return gatewayChargeId; }
    public void setGatewayChargeId(String gatewayChargeId) { this.gatewayChargeId = gatewayChargeId; }

    public String getGatewayCustomerId() { return gatewayCustomerId; }
    public void setGatewayCustomerId(String gatewayCustomerId) { this.gatewayCustomerId = gatewayCustomerId; }

    public BigDecimal getAmountCharged() { return amountCharged; }
    public void setAmountCharged(BigDecimal amountCharged) { this.amountCharged = amountCharged; }

    public BigDecimal getAmountRefunded() { return amountRefunded; }
    public void setAmountRefunded(BigDecimal amountRefunded) { this.amountRefunded = amountRefunded; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }

    public String getCardExpMonth() { return cardExpMonth; }
    public void setCardExpMonth(String cardExpMonth) { this.cardExpMonth = cardExpMonth; }

    public String getCardExpYear() { return cardExpYear; }
    public void setCardExpYear(String cardExpYear) { this.cardExpYear = cardExpYear; }

    public String getCardFingerprint() { return cardFingerprint; }
    public void setCardFingerprint(String cardFingerprint) { this.cardFingerprint = cardFingerprint; }

    public AuthenticationStatus getAuthStatus() { return authStatus; }
    public void setAuthStatus(AuthenticationStatus authStatus) { this.authStatus = authStatus; }

    public String getAuthUrl() { return authUrl; }
    public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }

    public String getAuthEci() { return authEci; }
    public void setAuthEci(String authEci) { this.authEci = authEci; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getFraudMessage() { return fraudMessage; }
    public void setFraudMessage(String fraudMessage) { this.fraudMessage = fraudMessage; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }

    // --- Helper methods ---
    public boolean requiresCustomerAction() {
        return status == GatewayStatus.REQUIRES_ACTION;
    }

    public boolean requiresCapture() {
        return status == GatewayStatus.REQUIRES_CAPTURE;
    }
}
