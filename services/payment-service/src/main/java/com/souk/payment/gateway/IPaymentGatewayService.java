package com.souk.payment.gateway;

import com.souk.common.domain.Payment;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;

import java.math.BigDecimal;

/**
 * Interface for PaymentGatewayService to enable mocking in unit tests.
 * This separates the contract from the implementation which depends on PaymentJpaAdapter.
 */
public interface IPaymentGatewayService {

    /**
     * Process a charge: call gateway and update DB with result
     */
    GatewayResponse charge(String gatewayId, Long paymentId, GatewayRequest request);

    /**
     * Authorize (hold funds): call gateway and update DB
     */
    GatewayResponse authorize(String gatewayId, Long paymentId, GatewayRequest request);

    /**
     * Capture authorized payment: call gateway and update DB
     */
    GatewayResponse capture(String gatewayId, Long paymentId, String gatewayPaymentId, BigDecimal amount);

    /**
     * Cancel/void authorized payment: call gateway and update DB
     */
    GatewayResponse cancel(String gatewayId, Long paymentId, String gatewayPaymentId);

    /**
     * Refund payment: call gateway and update DB
     */
    GatewayResponse refund(String gatewayId, Long paymentId, String gatewayPaymentId,
                           BigDecimal amount, String reason);

    /**
     * Mark payment as given up after exhausting retries.
     */
    Payment giveup(Long paymentId, String reason);

    /**
     * Handle webhook: update DB based on async gateway event
     */
    GatewayResponse handleWebhook(String gatewayId, String payload, String signature);
}
