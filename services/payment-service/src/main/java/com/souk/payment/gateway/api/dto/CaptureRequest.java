package com.souk.payment.gateway.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for capturing an authorized payment.
 */
public record CaptureRequest(
        @NotNull(message = "Payment ID is required")
        Long paymentId,

        @NotBlank(message = "Gateway payment ID is required")
        String gatewayPaymentId,

        BigDecimal amount  // Optional: if null, captures full authorized amount
) {}
