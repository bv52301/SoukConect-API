package com.souk.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.souk.common.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        // --- Identity ---
        Long id,

        // --- Relationships ---
        Long orderId,
        Long customerId,
        Long vendorId,
        Long parentPaymentId,

        // --- Payment Type ---
        String paymentType,

        // --- Currency & Amounts ---
        String currency,
        BigDecimal amount,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal deliveryFee,
        BigDecimal serviceFee,

        // --- Platform/Vendor Split ---
        BigDecimal platformFee,
        BigDecimal platformFeePercent,
        BigDecimal vendorAmount,

        // --- Multi-currency ---
        BigDecimal exchangeRate,
        String settledCurrency,
        BigDecimal settledAmount,

        // --- Payment Method & Gateway ---
        String paymentMethod,
        String paymentGateway,
        String gatewayPaymentId,
        String gatewayChargeId,
        String gatewayResponseCode,
        String gatewayResponseMessage,

        // --- Card Details ---
        String cardBrand,
        String cardLastFour,
        String cardExpMonth,
        String cardExpYear,
        String cardHolderName,

        // --- 3D Secure / Authentication ---
        String authStatus,
        String authEci,

        // --- Status ---
        String status,
        String statusReason,

        // --- Vendor Payout ---
        String vendorPayoutStatus,
        String vendorPayoutReference,
        LocalDateTime vendorPayoutDate,

        // --- Refund ---
        BigDecimal refundedAmount,
        String refundReason,
        LocalDateTime refundDate,

        // --- Settlement ---
        String settlementStatus,
        String settlementReference,
        LocalDate settlementDate,

        // --- Fraud & Risk ---
        Integer riskScore,
        String fraudStatus,
        String fraudReason,

        // --- Payer Info ---
        String payerEmail,
        String payerPhone,
        String payerName,

        // --- Billing Address ---
        BillingAddressResponse billingAddress,

        // --- References ---
        String idempotencyKey,
        String externalReference,
        String invoiceNumber,
        String receiptUrl,

        // --- Metadata ---
        String description,
        String metadata,

        // --- Failure ---
        String failureReason,

        // --- Processing ---
        String processedBy,
        LocalDateTime processedAt,
        LocalDateTime capturedAt,

        // --- Retry ---
        Integer attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime nextRetryAt,

        // --- Timestamps ---
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(Payment p) {
        BillingAddressResponse billingAddr = null;
        if (p.getBillingAddressLine1() != null || p.getBillingCity() != null) {
            billingAddr = new BillingAddressResponse(
                    p.getBillingAddressLine1(),
                    p.getBillingAddressLine2(),
                    p.getBillingCity(),
                    p.getBillingState(),
                    p.getBillingPostalCode(),
                    p.getBillingCountry()
            );
        }

        return new PaymentResponse(
                p.getId(),
                p.getOrder() != null ? p.getOrder().getId() : null,
                p.getCustomer() != null ? p.getCustomer().getId() : null,
                p.getVendor() != null ? p.getVendor().getVendorId() : null,
                p.getParentPayment() != null ? p.getParentPayment().getId() : null,
                p.getPaymentType() != null ? p.getPaymentType().name() : null,
                p.getCurrency(),
                p.getAmount(),
                p.getOriginalAmount(),
                p.getDiscountAmount(),
                p.getTaxAmount(),
                p.getTipAmount(),
                p.getDeliveryFee(),
                p.getServiceFee(),
                p.getPlatformFee(),
                p.getPlatformFeePercent(),
                p.getVendorAmount(),
                p.getExchangeRate(),
                p.getSettledCurrency(),
                p.getSettledAmount(),
                p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null,
                p.getPaymentGateway(),
                p.getGatewayPaymentId(),
                p.getGatewayChargeId(),
                p.getGatewayResponseCode(),
                p.getGatewayResponseMessage(),
                p.getCardBrand(),
                p.getCardLastFour(),
                p.getCardExpMonth(),
                p.getCardExpYear(),
                p.getCardHolderName(),
                p.getAuthStatus() != null ? p.getAuthStatus().name() : null,
                p.getAuthEci(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getStatusReason(),
                p.getVendorPayoutStatus() != null ? p.getVendorPayoutStatus().name() : null,
                p.getVendorPayoutReference(),
                p.getVendorPayoutDate(),
                p.getRefundedAmount(),
                p.getRefundReason(),
                p.getRefundDate(),
                p.getSettlementStatus() != null ? p.getSettlementStatus().name() : null,
                p.getSettlementReference(),
                p.getSettlementDate(),
                p.getRiskScore(),
                p.getFraudStatus() != null ? p.getFraudStatus().name() : null,
                p.getFraudReason(),
                p.getPayerEmail(),
                p.getPayerPhone(),
                p.getPayerName(),
                billingAddr,
                p.getIdempotencyKey(),
                p.getExternalReference(),
                p.getInvoiceNumber(),
                p.getReceiptUrl(),
                p.getDescription(),
                p.getMetadata(),
                p.getFailureReason(),
                p.getProcessedBy(),
                p.getProcessedAt(),
                p.getCapturedAt(),
                p.getAttemptCount(),
                p.getLastAttemptAt(),
                p.getNextRetryAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BillingAddressResponse(
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {}
}
