package com.souk.payment.api.dto;

import com.souk.common.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentUpdateRequest(
        // --- Status ---
        Payment.PaymentStatus status,
        String statusReason,

        // --- Gateway response ---
        String gatewayPaymentId,
        String gatewayChargeId,
        String gatewayResponseCode,
        String gatewayResponseMessage,

        // --- Card details (from gateway response) ---
        String cardBrand,
        String cardLastFour,
        String cardExpMonth,
        String cardExpYear,
        String cardHolderName,
        String cardFingerprint,

        // --- 3D Secure / Authentication ---
        Payment.AuthenticationStatus authStatus,
        String authValue,
        String authEci,

        // --- Vendor payout ---
        Payment.PayoutStatus vendorPayoutStatus,
        String vendorPayoutReference,
        LocalDateTime vendorPayoutDate,

        // --- Refund ---
        BigDecimal refundedAmount,
        String refundReason,
        LocalDateTime refundDate,

        // --- Settlement ---
        Payment.SettlementStatus settlementStatus,
        String settlementReference,
        LocalDate settlementDate,

        // --- Fraud ---
        Integer riskScore,
        Payment.FraudStatus fraudStatus,
        String fraudReason,

        // --- Receipt ---
        String receiptUrl,

        // --- Notes ---
        String internalNotes,
        String failureReason,

        // --- Processing ---
        String processedBy,
        LocalDateTime processedAt,
        LocalDateTime capturedAt
) {
}
