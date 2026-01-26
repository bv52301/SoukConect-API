package com.souk.payment.gateway;

import com.souk.payment.gateway.config.GatewayProperties;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gateway orchestration service.
 * Provides a unified interface to interact with different payment gateways.
 * Auto-discovers all PaymentGateway implementations via Spring DI.
 */
@Service
public class GatewayService {

    private static final Logger log = LoggerFactory.getLogger(GatewayService.class);

    private final Map<String, PaymentGateway> gateways;
    private final GatewayProperties properties;

    public GatewayService(List<PaymentGateway> gatewayList, GatewayProperties properties) {
        this.properties = properties;
        // Build map of gateway ID -> gateway implementation
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(
                        g -> g.getGatewayId().toUpperCase(),
                        Function.identity()
                ));

        log.info("Gateway service initialized with {} gateways: {}",
                gateways.size(), gateways.keySet());
    }

    // ==================== GATEWAY SELECTION ====================

    /**
     * Get the default gateway
     */
    public PaymentGateway getDefaultGateway() {
        return getGateway(properties.getDefaultGateway());
    }

    /**
     * Get a specific gateway by ID
     */
    public PaymentGateway getGateway(String gatewayId) {
        PaymentGateway gateway = gateways.get(gatewayId.toUpperCase());
        if (gateway == null) {
            throw new IllegalArgumentException("Unknown gateway: " + gatewayId);
        }
        if (!gateway.isEnabled()) {
            throw new IllegalStateException("Gateway not enabled: " + gatewayId);
        }
        return gateway;
    }

    /**
     * Find the best gateway for a payment method and currency
     */
    public Optional<PaymentGateway> findGateway(String paymentMethod, String currency) {
        return gateways.values().stream()
                .filter(PaymentGateway::isEnabled)
                .filter(g -> g.supports(paymentMethod))
                .filter(g -> g.supportsCurrency(currency))
                .findFirst();
    }

    /**
     * Get all enabled gateways
     */
    public List<PaymentGateway> getEnabledGateways() {
        return gateways.values().stream()
                .filter(PaymentGateway::isEnabled)
                .toList();
    }

    /**
     * Check if a gateway exists and is enabled
     */
    public boolean isGatewayAvailable(String gatewayId) {
        PaymentGateway gateway = gateways.get(gatewayId.toUpperCase());
        return gateway != null && gateway.isEnabled();
    }

    // ==================== PAYMENT OPERATIONS ====================

    /**
     * Process a charge using the specified gateway
     */
    public GatewayResponse charge(String gatewayId, GatewayRequest request) {
        log.info("Processing charge via {} - PaymentId: {}, Amount: {} {}",
                gatewayId, request.getPaymentId(), request.getAmount(), request.getCurrency());

        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.charge(request);
    }

    /**
     * Process a charge using the default gateway
     */
    public GatewayResponse charge(GatewayRequest request) {
        return charge(properties.getDefaultGateway(), request);
    }

    /**
     * Authorize (hold) a payment
     */
    public GatewayResponse authorize(String gatewayId, GatewayRequest request) {
        log.info("Authorizing via {} - PaymentId: {}, Amount: {} {}",
                gatewayId, request.getPaymentId(), request.getAmount(), request.getCurrency());

        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.authorize(request);
    }

    /**
     * Capture a previously authorized payment
     */
    public GatewayResponse capture(String gatewayId, String gatewayPaymentId, BigDecimal amount) {
        log.info("Capturing via {} - GatewayPaymentId: {}, Amount: {}",
                gatewayId, gatewayPaymentId, amount);

        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.capture(gatewayPaymentId, amount);
    }

    /**
     * Cancel/void an authorized payment
     */
    public GatewayResponse cancel(String gatewayId, String gatewayPaymentId) {
        log.info("Cancelling via {} - GatewayPaymentId: {}", gatewayId, gatewayPaymentId);

        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.cancel(gatewayPaymentId);
    }

    /**
     * Refund a captured payment
     */
    public GatewayResponse refund(String gatewayId, String gatewayPaymentId,
                                   BigDecimal amount, String reason) {
        log.info("Refunding via {} - GatewayPaymentId: {}, Amount: {}, Reason: {}",
                gatewayId, gatewayPaymentId, amount, reason);

        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.refund(gatewayPaymentId, amount, reason);
    }

    /**
     * Get payment status from gateway
     */
    public GatewayResponse getStatus(String gatewayId, String gatewayPaymentId) {
        log.debug("Getting status from {} - GatewayPaymentId: {}", gatewayId, gatewayPaymentId);

        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.getStatus(gatewayPaymentId);
    }

    // ==================== WEBHOOK HANDLING ====================

    /**
     * Verify webhook signature for a gateway
     */
    public boolean verifyWebhook(String gatewayId, String payload, String signature) {
        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.verifyWebhook(payload, signature);
    }

    /**
     * Parse webhook payload for a gateway
     */
    public GatewayResponse parseWebhook(String gatewayId, String payload) {
        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.parseWebhook(payload);
    }

    // ==================== CUSTOMER MANAGEMENT ====================

    /**
     * Create a customer at the gateway
     */
    public String createCustomer(String gatewayId, String email, String name) {
        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.createCustomer(email, name);
    }

    /**
     * Save a payment method for a customer
     */
    public String savePaymentMethod(String gatewayId, String gatewayCustomerId, String paymentToken) {
        PaymentGateway gateway = getGateway(gatewayId);
        return gateway.savePaymentMethod(gatewayCustomerId, paymentToken);
    }
}
