package com.souk.payment.gateway.impl;

import com.souk.payment.gateway.PaymentGateway;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Abstract base class for payment gateway implementations.
 * Provides common functionality like logging, validation, and error handling.
 */
public abstract class AbstractPaymentGateway implements PaymentGateway {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Validate request before processing
     */
    protected void validateRequest(GatewayRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Gateway request cannot be null");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
    }

    /**
     * Log gateway operation
     */
    protected void logOperation(String operation, GatewayRequest request) {
        log.info("[{}] {} - Amount: {} {}, PaymentId: {}, IdempotencyKey: {}",
                getGatewayId(), operation,
                request.getAmount(), request.getCurrency(),
                request.getPaymentId(), request.getIdempotencyKey());
    }

    /**
     * Log gateway operation result
     */
    protected void logResult(String operation, GatewayResponse response) {
        if (response.isSuccess()) {
            log.info("[{}] {} SUCCESS - GatewayPaymentId: {}, Status: {}",
                    getGatewayId(), operation,
                    response.getGatewayPaymentId(), response.getStatus());
        } else {
            log.warn("[{}] {} FAILED - Error: {} - {}",
                    getGatewayId(), operation,
                    response.getErrorCode(), response.getErrorMessage());
        }
    }

    /**
     * Create a failure response with standard error handling
     */
    protected GatewayResponse handleException(String operation, Exception e) {
        log.error("[{}] {} exception: {}", getGatewayId(), operation, e.getMessage(), e);
        return GatewayResponse.failure(
                getGatewayId(),
                "GATEWAY_ERROR",
                e.getMessage()
        );
    }

    /**
     * Convert amount to gateway-specific format (e.g., cents for Stripe)
     */
    protected long toMinorUnits(BigDecimal amount, String currency) {
        // Most currencies use 2 decimal places
        int decimalPlaces = getDecimalPlaces(currency);
        return amount.movePointRight(decimalPlaces).longValue();
    }

    /**
     * Convert from gateway minor units back to BigDecimal
     */
    protected BigDecimal fromMinorUnits(long amount, String currency) {
        int decimalPlaces = getDecimalPlaces(currency);
        return BigDecimal.valueOf(amount).movePointLeft(decimalPlaces);
    }

    /**
     * Get decimal places for currency
     */
    protected int getDecimalPlaces(String currency) {
        // Zero-decimal currencies
        return switch (currency.toUpperCase()) {
            case "JPY", "KRW", "VND", "BIF", "CLP", "DJF", "GNF", "ISK",
                 "KMF", "PYG", "RWF", "UGX", "VUV", "XAF", "XOF", "XPF" -> 0;
            case "BHD", "JOD", "KWD", "OMR", "TND" -> 3; // Three-decimal currencies
            default -> 2;
        };
    }
}
