package com.souk.payment.gateway.impl;

import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import com.souk.payment.gateway.model.GatewayResponse.GatewayStatus;
import com.souk.payment.gateway.model.GatewayResponse.AuthenticationStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mock implementation of Stripe Gateway for testing/dev environments.
 * Replaces the real StripeGateway when 'payment.gateway.stripe.enabled' is false.
 */
@Component
@ConditionalOnProperty(
    prefix = "payment.gateway.stripe", name = "enabled", havingValue = "false"
)
public class MockStripeGateway extends AbstractPaymentGateway {

    private static final String GATEWAY_ID = "STRIPE";

    @Override
    public String getGatewayId() {
        return GATEWAY_ID;
    }

    @Override
    public String getDisplayName() {
        return "Mock Stripe (Test Mode)";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supports(String paymentMethodType) {
        return true; // Support everything in mock
    }

    @Override
    public boolean supportsCurrency(String currency) {
        return true; // Support all currencies
    }

    @Override
    public GatewayResponse charge(GatewayRequest request) {
        validateRequest(request);
        logOperation("MOCK_CHARGE", request);

        // Simulate success
        return GatewayResponse.builder()
                .success(true)
                .status(GatewayStatus.SUCCEEDED)
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId("ch_mock_" + UUID.randomUUID().toString())
                .gatewayChargeId("py_mock_" + UUID.randomUUID().toString())
                .amountCharged(request.getAmount())
                .currency(request.getCurrency())
                .processedAt(Instant.now())
                .cardBrand("VISA")
                .cardLastFour("4242")
                .build();
    }

    @Override
    public GatewayResponse authorize(GatewayRequest request) {
        validateRequest(request);
        logOperation("MOCK_AUTHORIZE", request);

        return GatewayResponse.builder()
                .success(true)
                .status(GatewayStatus.REQUIRES_CAPTURE)
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId("pi_mock_auth_" + UUID.randomUUID().toString())
                .amountCharged(request.getAmount())
                .currency(request.getCurrency())
                .processedAt(Instant.now())
                .build();
    }

    @Override
    public GatewayResponse capture(String gatewayPaymentId, BigDecimal amount) {
        return GatewayResponse.builder()
                .success(true)
                .status(GatewayStatus.SUCCEEDED)
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId(gatewayPaymentId)
                .capturedAt(Instant.now())
                .build();
    }

    @Override
    public GatewayResponse cancel(String gatewayPaymentId) {
        return GatewayResponse.builder()
                .success(true)
                .status(GatewayStatus.CANCELLED)
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId(gatewayPaymentId)
                .processedAt(Instant.now())
                .build();
    }

    @Override
    public GatewayResponse refund(String gatewayPaymentId, BigDecimal amount, String reason) {
        return GatewayResponse.builder()
                .success(true)
                .status(GatewayStatus.REFUNDED)
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId("re_mock_" + UUID.randomUUID().toString())
                .amountRefunded(amount)
                .processedAt(Instant.now())
                .build();
    }

    @Override
    public GatewayResponse getStatus(String gatewayPaymentId) {
        return GatewayResponse.builder()
                .success(true)
                .status(GatewayStatus.SUCCEEDED)
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId(gatewayPaymentId)
                .build();
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        return true;
    }

    @Override
    public GatewayResponse parseWebhook(String payload) {
        return GatewayResponse.failure(GATEWAY_ID, "NOT_IMPLEMENTED", "Mock webhook parsing not implemented");
    }

    @Override
    public String createCustomer(String email, String name) {
        return "cus_mock_" + UUID.randomUUID().toString();
    }

    @Override
    public String savePaymentMethod(String gatewayCustomerId, String paymentToken) {
        return "pm_mock_" + UUID.randomUUID().toString();
    }
}
