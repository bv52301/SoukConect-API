package com.souk.payment.gateway;

import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;

import java.math.BigDecimal;

/**
 * Abstract interface for payment gateway integrations.
 * Each payment provider (Stripe, CMI, etc.) implements this interface.
 */
public interface PaymentGateway {

    /**
     * Get the unique identifier for this gateway (e.g., "STRIPE", "CMI")
     */
    String getGatewayId();

    /**
     * Get display name for this gateway
     */
    String getDisplayName();

    /**
     * Check if this gateway is enabled/configured
     */
    boolean isEnabled();

    /**
     * Check if this gateway supports the given payment method
     */
    boolean supports(String paymentMethodType);

    /**
     * Check if this gateway supports the given currency
     */
    boolean supportsCurrency(String currency);

    // ==================== PAYMENT OPERATIONS ====================

    /**
     * Process a payment (authorize and optionally capture)
     * @param request The payment request details
     * @return Gateway response with transaction details
     */
    GatewayResponse charge(GatewayRequest request);

    /**
     * Authorize a payment without capturing (hold funds)
     * @param request The payment request details
     * @return Gateway response with authorization details
     */
    GatewayResponse authorize(GatewayRequest request);

    /**
     * Capture a previously authorized payment
     * @param gatewayPaymentId The gateway's payment/authorization ID
     * @param amount Amount to capture (can be less than authorized)
     * @return Gateway response with capture details
     */
    GatewayResponse capture(String gatewayPaymentId, BigDecimal amount);

    /**
     * Cancel/void an authorized payment (before capture)
     * @param gatewayPaymentId The gateway's payment/authorization ID
     * @return Gateway response with cancellation details
     */
    GatewayResponse cancel(String gatewayPaymentId);

    /**
     * Refund a captured payment (full or partial)
     * @param gatewayPaymentId The gateway's payment ID
     * @param amount Amount to refund
     * @param reason Reason for refund
     * @return Gateway response with refund details
     */
    GatewayResponse refund(String gatewayPaymentId, BigDecimal amount, String reason);

    // ==================== STATUS & VERIFICATION ====================

    /**
     * Get the current status of a payment at the gateway
     * @param gatewayPaymentId The gateway's payment ID
     * @return Gateway response with current status
     */
    GatewayResponse getStatus(String gatewayPaymentId);

    /**
     * Verify a webhook signature from this gateway
     * @param payload The webhook payload
     * @param signature The signature header
     * @return true if signature is valid
     */
    boolean verifyWebhook(String payload, String signature);

    /**
     * Parse a webhook event from this gateway
     * @param payload The webhook payload
     * @return Gateway response representing the event
     */
    GatewayResponse parseWebhook(String payload);

    // ==================== CUSTOMER MANAGEMENT ====================

    /**
     * Create a customer at the gateway (for saved payment methods)
     * @param email Customer email
     * @param name Customer name
     * @return Gateway customer ID
     */
    default String createCustomer(String email, String name) {
        throw new UnsupportedOperationException("Customer management not supported by this gateway");
    }

    /**
     * Save a payment method for a customer
     * @param gatewayCustomerId Gateway's customer ID
     * @param paymentToken Payment method token
     * @return Gateway payment method ID
     */
    default String savePaymentMethod(String gatewayCustomerId, String paymentToken) {
        throw new UnsupportedOperationException("Payment method saving not supported by this gateway");
    }
}
