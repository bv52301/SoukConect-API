package com.souk.payment.gateway.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request object for payment gateway operations.
 * Contains all information needed to process a payment with external providers.
 */
public class GatewayRequest {

    // Payment identification
    private Long paymentId;
    private String idempotencyKey;

    // Amount & Currency
    private BigDecimal amount;
    private String currency;

    // Payment method details
    private String paymentMethodType;  // CARD, BANK_TRANSFER, etc.
    private String paymentToken;       // Token from client-side SDK
    private String paymentMethodId;    // Saved payment method ID

    // Card details (if provided directly - PCI compliance consideration)
    private String cardNumber;
    private String cardExpMonth;
    private String cardExpYear;
    private String cardCvc;
    private String cardHolderName;

    // 3D Secure
    private boolean require3DSecure;
    private String returnUrl;          // URL for 3DS redirect
    private String callbackUrl;        // Webhook callback URL

    // Gateway customer (for saved payment methods)
    private String gatewayCustomerId;

    // Customer info
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private String customerId;         // Gateway's customer ID if exists

    // Billing address
    private String billingLine1;
    private String billingLine2;
    private String billingCity;
    private String billingState;
    private String billingPostalCode;
    private String billingCountry;

    // Order/Description
    private String description;
    private String orderId;
    private String invoiceNumber;

    // Capture behavior
    private boolean captureImmediately;  // true = charge, false = authorize only

    // Metadata for gateway
    private Map<String, String> metadata;

    // --- Builder pattern for cleaner construction ---
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final GatewayRequest request = new GatewayRequest();

        public Builder paymentId(Long paymentId) { request.paymentId = paymentId; return this; }
        public Builder idempotencyKey(String key) { request.idempotencyKey = key; return this; }
        public Builder amount(BigDecimal amount) { request.amount = amount; return this; }
        public Builder currency(String currency) { request.currency = currency; return this; }
        public Builder paymentMethodType(String type) { request.paymentMethodType = type; return this; }
        public Builder paymentToken(String token) { request.paymentToken = token; return this; }
        public Builder paymentMethodId(String id) { request.paymentMethodId = id; return this; }
        public Builder cardNumber(String num) { request.cardNumber = num; return this; }
        public Builder cardExpMonth(String month) { request.cardExpMonth = month; return this; }
        public Builder cardExpYear(String year) { request.cardExpYear = year; return this; }
        public Builder cardCvc(String cvc) { request.cardCvc = cvc; return this; }
        public Builder cardHolderName(String name) { request.cardHolderName = name; return this; }
        public Builder require3DSecure(boolean require) { request.require3DSecure = require; return this; }
        public Builder require3DS(boolean require) { request.require3DSecure = require; return this; }
        public Builder returnUrl(String url) { request.returnUrl = url; return this; }
        public Builder callbackUrl(String url) { request.callbackUrl = url; return this; }
        public Builder gatewayCustomerId(String id) { request.gatewayCustomerId = id; return this; }
        public Builder customerEmail(String email) { request.customerEmail = email; return this; }
        public Builder customerPhone(String phone) { request.customerPhone = phone; return this; }
        public Builder customerName(String name) { request.customerName = name; return this; }
        public Builder customerId(String id) { request.customerId = id; return this; }
        public Builder billingLine1(String line) { request.billingLine1 = line; return this; }
        public Builder billingLine2(String line) { request.billingLine2 = line; return this; }
        public Builder billingCity(String city) { request.billingCity = city; return this; }
        public Builder billingState(String state) { request.billingState = state; return this; }
        public Builder billingPostalCode(String code) { request.billingPostalCode = code; return this; }
        public Builder billingCountry(String country) { request.billingCountry = country; return this; }
        public Builder description(String desc) { request.description = desc; return this; }
        public Builder orderId(String id) { request.orderId = id; return this; }
        public Builder invoiceNumber(String num) { request.invoiceNumber = num; return this; }
        public Builder captureImmediately(boolean capture) { request.captureImmediately = capture; return this; }
        public Builder metadata(Map<String, String> meta) { request.metadata = meta; return this; }

        public GatewayRequest build() { return request; }
    }

    // --- Getters & Setters ---
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethodType() { return paymentMethodType; }
    public void setPaymentMethodType(String paymentMethodType) { this.paymentMethodType = paymentMethodType; }

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }

    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardExpMonth() { return cardExpMonth; }
    public void setCardExpMonth(String cardExpMonth) { this.cardExpMonth = cardExpMonth; }

    public String getCardExpYear() { return cardExpYear; }
    public void setCardExpYear(String cardExpYear) { this.cardExpYear = cardExpYear; }

    public String getCardCvc() { return cardCvc; }
    public void setCardCvc(String cardCvc) { this.cardCvc = cardCvc; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public boolean isRequire3DSecure() { return require3DSecure; }
    public void setRequire3DSecure(boolean require3DSecure) { this.require3DSecure = require3DSecure; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public String getGatewayCustomerId() { return gatewayCustomerId; }
    public void setGatewayCustomerId(String gatewayCustomerId) { this.gatewayCustomerId = gatewayCustomerId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getBillingLine1() { return billingLine1; }
    public void setBillingLine1(String billingLine1) { this.billingLine1 = billingLine1; }

    public String getBillingLine2() { return billingLine2; }
    public void setBillingLine2(String billingLine2) { this.billingLine2 = billingLine2; }

    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }

    public String getBillingState() { return billingState; }
    public void setBillingState(String billingState) { this.billingState = billingState; }

    public String getBillingPostalCode() { return billingPostalCode; }
    public void setBillingPostalCode(String billingPostalCode) { this.billingPostalCode = billingPostalCode; }

    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public boolean isCaptureImmediately() { return captureImmediately; }
    public void setCaptureImmediately(boolean captureImmediately) { this.captureImmediately = captureImmediately; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
