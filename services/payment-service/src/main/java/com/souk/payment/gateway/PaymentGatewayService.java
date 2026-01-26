package com.souk.payment.gateway;

import com.souk.common.adapters.jpa.PaymentJpaAdapter;
import com.souk.common.domain.Payment;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import com.souk.payment.gateway.model.GatewayResponse.GatewayStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service that combines gateway operations with DB updates.
 * Ensures atomic operation - gateway result is always persisted.
 *
 * Flow:
 * 1. Call external gateway (Stripe/CMI)
 * 2. Update Payment record with result (success or failure)
 * 3. Return response to BPM for workflow decisions
 */
@Service
public class PaymentGatewayService implements IPaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);

    private final GatewayService gatewayService;
    private final PaymentJpaAdapter paymentAdapter;

    public PaymentGatewayService(GatewayService gatewayService, PaymentJpaAdapter paymentAdapter) {
        this.gatewayService = gatewayService;
        this.paymentAdapter = paymentAdapter;
    }

    /**
     * Process a charge: call gateway and update DB with result
     */
    @Transactional
    public GatewayResponse charge(String gatewayId, Long paymentId, GatewayRequest request) {
        log.info("Processing charge - Gateway: {}, PaymentId: {}", gatewayId, paymentId);

        Payment payment = getPayment(paymentId);

        // Call gateway
        GatewayResponse response = gatewayService.charge(gatewayId, request);

        // Update DB with result (success or failure)
        updatePaymentFromResponse(payment, response);

        return response;
    }

    /**
     * Authorize (hold funds): call gateway and update DB
     */
    @Transactional
    public GatewayResponse authorize(String gatewayId, Long paymentId, GatewayRequest request) {
        log.info("Processing authorize - Gateway: {}, PaymentId: {}", gatewayId, paymentId);

        Payment payment = getPayment(paymentId);

        GatewayResponse response = gatewayService.authorize(gatewayId, request);

        updatePaymentFromResponse(payment, response);

        return response;
    }

    /**
     * Capture authorized payment: call gateway and update DB
     */
    @Transactional
    public GatewayResponse capture(String gatewayId, Long paymentId, String gatewayPaymentId, BigDecimal amount) {
        log.info("Processing capture - Gateway: {}, PaymentId: {}, GatewayPaymentId: {}",
                gatewayId, paymentId, gatewayPaymentId);

        Payment payment = getPayment(paymentId);

        GatewayResponse response = gatewayService.capture(gatewayId, gatewayPaymentId, amount);

        updatePaymentFromResponse(payment, response);

        return response;
    }

    /**
     * Cancel/void authorized payment: call gateway and update DB
     */
    @Transactional
    public GatewayResponse cancel(String gatewayId, Long paymentId, String gatewayPaymentId) {
        log.info("Processing cancel - Gateway: {}, PaymentId: {}, GatewayPaymentId: {}",
                gatewayId, paymentId, gatewayPaymentId);

        Payment payment = getPayment(paymentId);

        GatewayResponse response = gatewayService.cancel(gatewayId, gatewayPaymentId);

        if (response.isSuccess() || response.getStatus() == GatewayStatus.CANCELLED) {
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            payment.setStatusReason("Cancelled via gateway");
        } else {
            payment.setGatewayResponseCode(response.getErrorCode());
            payment.setGatewayResponseMessage(response.getErrorMessage());
        }
        paymentAdapter.save(payment);

        return response;
    }

    /**
     * Refund payment: call gateway and update DB
     */
    @Transactional
    public GatewayResponse refund(String gatewayId, Long paymentId, String gatewayPaymentId,
                                   BigDecimal amount, String reason) {
        log.info("Processing refund - Gateway: {}, PaymentId: {}, Amount: {}",
                gatewayId, paymentId, amount);

        Payment payment = getPayment(paymentId);

        GatewayResponse response = gatewayService.refund(gatewayId, gatewayPaymentId, amount, reason);

        if (response.isSuccess()) {
            BigDecimal currentRefunded = payment.getRefundedAmount() != null ?
                    payment.getRefundedAmount() : BigDecimal.ZERO;
            payment.setRefundedAmount(currentRefunded.add(amount));

            if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
            }
            payment.setRefundDate(LocalDateTime.now());
            payment.setRefundReason(reason);
        } else {
            payment.setGatewayResponseCode(response.getErrorCode());
            payment.setGatewayResponseMessage(response.getErrorMessage());
        }

        paymentAdapter.save(payment);

        return response;
    }

    /**
     * Mark payment as given up after exhausting retries.
     * Called by BPM when max retries reached.
     */
    @Transactional
    public Payment giveup(Long paymentId, String reason) {
        log.info("Giving up on payment - PaymentId: {}, Reason: {}", paymentId, reason);

        Payment payment = getPayment(paymentId);

        payment.setStatus(Payment.PaymentStatus.ABANDONED);
        payment.setNextRetryAt(null);  // No more retries
        if (reason != null) {
            payment.setStatusReason(reason);
        }

        return paymentAdapter.save(payment);
    }

    /**
     * Handle webhook: update DB based on async gateway event
     */
    @Transactional
    public GatewayResponse handleWebhook(String gatewayId, String payload, String signature) {
        log.info("Processing webhook from gateway: {}", gatewayId);

        // Verify webhook
        if (signature != null && !gatewayService.verifyWebhook(gatewayId, payload, signature)) {
            log.warn("Webhook verification failed for gateway: {}", gatewayId);
            return GatewayResponse.failure(gatewayId, "INVALID_SIGNATURE", "Webhook signature verification failed");
        }

        // Parse webhook
        GatewayResponse response = gatewayService.parseWebhook(gatewayId, payload);

        // Find and update payment by gateway payment ID
        if (response.getGatewayPaymentId() != null) {
            paymentAdapter.findByGatewayPaymentId(response.getGatewayPaymentId())
                    .ifPresent(payment -> {
                        updatePaymentFromResponse(payment, response);
                        log.info("Updated payment {} from webhook", payment.getId());
                    });
        }

        return response;
    }

    // ==================== HELPER METHODS ====================

    private Payment getPayment(Long paymentId) {
        return paymentAdapter.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    private void updatePaymentFromResponse(Payment payment, GatewayResponse response) {
        // Gateway identifiers
        if (response.getGatewayPaymentId() != null) {
            payment.setGatewayPaymentId(response.getGatewayPaymentId());
        }
        if (response.getGatewayChargeId() != null) {
            payment.setGatewayChargeId(response.getGatewayChargeId());
        }
        if (response.getGatewayName() != null) {
            payment.setPaymentGateway(response.getGatewayName());
        }

        // Status mapping
        payment.setStatus(mapGatewayStatus(response.getStatus()));

        // Card details
        if (response.getCardBrand() != null) {
            payment.setCardBrand(response.getCardBrand());
        }
        if (response.getCardLastFour() != null) {
            payment.setCardLastFour(response.getCardLastFour());
        }
        if (response.getCardExpMonth() != null) {
            payment.setCardExpMonth(response.getCardExpMonth());
        }
        if (response.getCardExpYear() != null) {
            payment.setCardExpYear(response.getCardExpYear());
        }

        // 3DS / Authentication
        if (response.getAuthStatus() != null) {
            payment.setAuthStatus(mapAuthStatus(response.getAuthStatus()));
        }
        if (response.getAuthUrl() != null) {
            payment.setMetadata("{\"authUrl\":\"" + response.getAuthUrl() + "\"}");
        }

        // Risk/Fraud
        if (response.getRiskScore() != null) {
            payment.setRiskScore(response.getRiskScore());
        }

        // Receipt
        if (response.getReceiptUrl() != null) {
            payment.setReceiptUrl(response.getReceiptUrl());
        }

        // Timestamps
        if (response.isSuccess()) {
            payment.setProcessedAt(LocalDateTime.now());
        }
        if (response.getCapturedAt() != null) {
            payment.setCapturedAt(LocalDateTime.now());
        }

        // Error handling for failures
        if (!response.isSuccess() && response.getStatus() == GatewayStatus.FAILED) {
            payment.setGatewayResponseCode(response.getErrorCode());
            payment.setGatewayResponseMessage(response.getErrorMessage());
            payment.setFailureReason(response.getErrorMessage());
            payment.setAttemptCount(payment.getAttemptCount() + 1);
        }

        paymentAdapter.save(payment);

        log.info("Updated payment {} - Status: {}, GatewayPaymentId: {}",
                payment.getId(), payment.getStatus(), payment.getGatewayPaymentId());
    }

    private Payment.PaymentStatus mapGatewayStatus(GatewayStatus gatewayStatus) {
        if (gatewayStatus == null) {
            return Payment.PaymentStatus.PENDING;
        }
        return switch (gatewayStatus) {
            case SUCCEEDED -> Payment.PaymentStatus.COMPLETED;
            case PENDING -> Payment.PaymentStatus.PENDING;
            case REQUIRES_ACTION -> Payment.PaymentStatus.REQUIRES_ACTION;
            case REQUIRES_CAPTURE -> Payment.PaymentStatus.AUTHORIZED;
            case FAILED -> Payment.PaymentStatus.FAILED;
            case CANCELLED -> Payment.PaymentStatus.CANCELLED;
            case REFUNDED -> Payment.PaymentStatus.REFUNDED;
            case PARTIALLY_REFUNDED -> Payment.PaymentStatus.PARTIALLY_REFUNDED;
        };
    }

    private Payment.AuthenticationStatus mapAuthStatus(GatewayResponse.AuthenticationStatus authStatus) {
        if (authStatus == null) {
            return null;
        }
        return switch (authStatus) {
            case NOT_REQUIRED -> Payment.AuthenticationStatus.NOT_REQUIRED;
            case PENDING -> Payment.AuthenticationStatus.PENDING;
            case SUCCEEDED -> Payment.AuthenticationStatus.SUCCESSFUL;
            case FAILED -> Payment.AuthenticationStatus.FAILED;
            case ATTEMPTED -> Payment.AuthenticationStatus.ATTEMPTED;
        };
    }
}
