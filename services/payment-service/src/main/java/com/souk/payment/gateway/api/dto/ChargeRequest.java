package com.souk.payment.gateway.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for charging a payment through a gateway.
 */
public record ChargeRequest(
        @NotNull(message = "Payment ID is required")
        Long paymentId,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        String paymentToken,
        String gatewayCustomerId,

        // Optional fields
        Long orderId,
        String description,
        String customerEmail,
        String customerName,
        String customerPhone,

        // Billing address
        String billingLine1,
        String billingLine2,
        String billingCity,
        String billingState,
        String billingPostalCode,
        String billingCountry,

        // 3DS
        String returnUrl,
        String callbackUrl,
        boolean require3DS
) {}
