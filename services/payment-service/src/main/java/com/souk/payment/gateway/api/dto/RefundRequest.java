package com.souk.payment.gateway.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for refunding a payment.
 */
public record RefundRequest(
        @NotNull(message = "Payment ID is required")
        Long paymentId,

        @NotBlank(message = "Gateway payment ID is required")
        String gatewayPaymentId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        String reason
) {}
