package com.souk.payment.api;

import com.souk.common.adapters.jpa.PaymentJpaAdapter;
import com.souk.common.domain.Customer;
import com.souk.common.domain.Order;
import com.souk.common.domain.Payment;
import com.souk.common.domain.Vendor;
import com.souk.common.port.DataAccessPort;
import com.souk.payment.api.dto.PaymentCreateRequest;
import com.souk.payment.api.dto.PaymentResponse;
import com.souk.payment.api.dto.PaymentUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentJpaAdapter paymentAdapter;
    private final DataAccessPort<Order, Long> orderPort;
    private final DataAccessPort<Customer, Long> customerPort;
    private final DataAccessPort<Vendor, Long> vendorPort;

    public PaymentController(PaymentJpaAdapter paymentAdapter,
                             DataAccessPort<Order, Long> orderPort,
                             DataAccessPort<Customer, Long> customerPort,
                             DataAccessPort<Vendor, Long> vendorPort) {
        this.paymentAdapter = paymentAdapter;
        this.orderPort = orderPort;
        this.customerPort = customerPort;
        this.vendorPort = vendorPort;
    }

    // ==================== LIST & SEARCH ====================

    @GetMapping
    public List<PaymentResponse> listAll() {
        return paymentAdapter.findAll().stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable @Min(1) Long id) {
        return paymentAdapter.findById(id)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentResponse> getByOrderId(@PathVariable @Min(1) Long orderId) {
        return paymentAdapter.findByOrderId(orderId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/customer/{customerId}")
    public List<PaymentResponse> getByCustomerId(@PathVariable @Min(1) Long customerId) {
        return paymentAdapter.findByCustomerId(customerId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/vendor/{vendorId}")
    public List<PaymentResponse> getByVendorId(@PathVariable @Min(1) Long vendorId) {
        return paymentAdapter.findByVendorId(vendorId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(@PathVariable String status) {
        Payment.PaymentStatus paymentStatus;
        try {
            paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status '" + status + "'. Valid values: " +
                            Arrays.toString(Payment.PaymentStatus.values())));
        }
        return ResponseEntity.ok(paymentAdapter.findByStatus(paymentStatus).stream()
                .map(PaymentResponse::from)
                .toList());
    }

    @GetMapping("/gateway/{gateway}")
    public List<PaymentResponse> getByGateway(@PathVariable String gateway) {
        return paymentAdapter.findByPaymentGateway(gateway).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/idempotency/{key}")
    public ResponseEntity<PaymentResponse> getByIdempotencyKey(@PathVariable String key) {
        return paymentAdapter.findByIdempotencyKey(key)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/external/{reference}")
    public ResponseEntity<PaymentResponse> getByExternalReference(@PathVariable String reference) {
        return paymentAdapter.findByExternalReference(reference)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/refunds/{parentPaymentId}")
    public List<PaymentResponse> getRefundsByParentPayment(@PathVariable @Min(1) Long parentPaymentId) {
        return paymentAdapter.findByParentPaymentId(parentPaymentId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/vendor/{vendorId}/pending-payout")
    public List<PaymentResponse> getPendingPayoutsForVendor(@PathVariable @Min(1) Long vendorId) {
        return paymentAdapter.findPendingPayoutsForVendor(vendorId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    // ==================== CREATE ====================

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PaymentCreateRequest req) {
        // Idempotency check
        if (req.idempotencyKey() != null) {
            Optional<Payment> existingPayment = paymentAdapter.findByIdempotencyKey(req.idempotencyKey());
            if (existingPayment.isPresent()) {
                return ResponseEntity.ok(PaymentResponse.from(existingPayment.get()));
            }
        }

        // Resolve relationships with validation
        Order order = null;
        if (req.orderId() != null) {
            order = orderPort.findById(req.orderId()).orElse(null);
            if (order == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Order not found: " + req.orderId()));
        }

        Customer customer = null;
        if (req.customerId() != null) {
            customer = customerPort.findById(req.customerId()).orElse(null);
            if (customer == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Customer not found: " + req.customerId()));
        }

        Vendor vendor = req.vendorId() != null ? vendorPort.findById(req.vendorId()).orElse(null) : null;

        Payment payment = req.toDomain(order, customer, vendor);
        Payment saved = paymentAdapter.save(payment);

        return ResponseEntity.created(URI.create("/api/v1/payments/" + saved.getId()))
                .body(PaymentResponse.from(saved));
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    public ResponseEntity<PaymentResponse> update(@PathVariable @Min(1) Long id,
                                                  @Valid @RequestBody PaymentUpdateRequest req) {
        return paymentAdapter.findById(id)
                .map(existing -> {
                    applyUpdates(existing, req);
                    Payment updated = paymentAdapter.save(existing);
                    return ResponseEntity.ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable @Min(1) Long id,
                                          @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        String reason = body.get("reason");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "status field is required"));
        }
        Payment.PaymentStatus status;
        try {
            status = Payment.PaymentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status '" + statusStr + "'. Valid values: " +
                            Arrays.toString(Payment.PaymentStatus.values())));
        }
        return paymentAdapter.findById(id)
                .map(existing -> {
                    existing.setStatus(status);
                    if (reason != null) existing.setStatusReason(reason);
                    Payment updated = paymentAdapter.save(existing);
                    return ResponseEntity.<Object>ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== PAYMENT ACTIONS ====================

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processPayment(@PathVariable @Min(1) Long id) {
        return paymentAdapter.findById(id)
                .map(payment -> {
                    if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
                        return ResponseEntity.badRequest().<Object>body(Map.of(
                                "error", "Payment cannot be processed in status: " + payment.getStatus() +
                                        ". Expected: PENDING"));
                    }

                    payment.setAttemptCount(payment.getAttemptCount() != null ? payment.getAttemptCount() + 1 : 1);
                    payment.setLastAttemptAt(LocalDateTime.now());
                    payment.setStatus(Payment.PaymentStatus.PROCESSING);
                    paymentAdapter.save(payment);

                    // TODO: Integrate with actual payment gateway (Stripe/CMI)
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setGatewayPaymentId("PAY-" + System.currentTimeMillis());
                    payment.setGatewayChargeId("CHG-" + System.currentTimeMillis());
                    payment.setProcessedAt(LocalDateTime.now());
                    payment.setCapturedAt(LocalDateTime.now());

                    Payment updated = paymentAdapter.save(payment);
                    return ResponseEntity.<Object>ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<?> capturePayment(@PathVariable @Min(1) Long id) {
        return paymentAdapter.findById(id)
                .map(payment -> {
                    if (payment.getStatus() != Payment.PaymentStatus.AUTHORIZED) {
                        return ResponseEntity.badRequest().<Object>body(Map.of(
                                "error", "Payment cannot be captured in status: " + payment.getStatus() +
                                        ". Expected: AUTHORIZED"));
                    }

                    // TODO: Call gateway to capture
                    payment.setStatus(Payment.PaymentStatus.CAPTURED);
                    payment.setCapturedAt(LocalDateTime.now());

                    Payment updated = paymentAdapter.save(payment);
                    return ResponseEntity.<Object>ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPayment(@PathVariable @Min(1) Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        return paymentAdapter.findById(id)
                .map(payment -> {
                    if (payment.getStatus() == Payment.PaymentStatus.COMPLETED ||
                        payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
                        return ResponseEntity.badRequest().<Object>body(Map.of(
                                "error", "Cannot cancel payment in status: " + payment.getStatus()));
                    }

                    payment.setStatus(Payment.PaymentStatus.CANCELLED);
                    if (body != null && body.get("reason") != null) {
                        payment.setStatusReason(body.get("reason"));
                    }

                    Payment updated = paymentAdapter.save(payment);
                    return ResponseEntity.<Object>ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mark payment as given up after exhausting all retries.
     * Called by BPM when max retry attempts reached.
     */
    @PostMapping("/{id}/giveup")
    public ResponseEntity<?> giveupPayment(@PathVariable @Min(1) Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        return paymentAdapter.findById(id)
                .map(payment -> {
                    if (payment.getStatus() == Payment.PaymentStatus.COMPLETED ||
                        payment.getStatus() == Payment.PaymentStatus.REFUNDED ||
                        payment.getStatus() == Payment.PaymentStatus.ABANDONED) {
                        return ResponseEntity.badRequest().<Object>body(Map.of(
                                "error", "Cannot give up payment in status: " + payment.getStatus()));
                    }

                    payment.setStatus(Payment.PaymentStatus.ABANDONED);
                    payment.setNextRetryAt(null);
                    if (body != null && body.get("reason") != null) {
                        payment.setStatusReason(body.get("reason"));
                    }

                    Payment updated = paymentAdapter.save(payment);
                    return ResponseEntity.<Object>ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable @Min(1) Long id,
                                           @RequestBody Map<String, Object> body) {
        return paymentAdapter.findById(id)
                .map(payment -> {
                    if (payment.getStatus() != Payment.PaymentStatus.COMPLETED &&
                        payment.getStatus() != Payment.PaymentStatus.CAPTURED) {
                        return ResponseEntity.badRequest().<Object>body(Map.of(
                                "error", "Cannot refund payment in status: " + payment.getStatus() +
                                        ". Expected: COMPLETED or CAPTURED"));
                    }

                    String reason = body.get("reason") != null ? body.get("reason").toString() : null;
                    Object amountObj = body.get("amount");
                    java.math.BigDecimal refundAmount = amountObj != null
                            ? new java.math.BigDecimal(amountObj.toString())
                            : payment.getAmount();

                    Payment refund = new Payment();
                    refund.setParentPayment(payment);
                    refund.setOrder(payment.getOrder());
                    refund.setCustomer(payment.getCustomer());
                    refund.setVendor(payment.getVendor());
                    refund.setPaymentType(refundAmount.compareTo(payment.getAmount()) < 0
                            ? Payment.PaymentType.PARTIAL_REFUND
                            : Payment.PaymentType.REFUND);
                    refund.setAmount(refundAmount.negate());
                    refund.setCurrency(payment.getCurrency());
                    refund.setPaymentMethod(payment.getPaymentMethod());
                    refund.setPaymentGateway(payment.getPaymentGateway());
                    refund.setStatus(Payment.PaymentStatus.COMPLETED);
                    refund.setDescription("Refund for payment #" + payment.getId());
                    if (reason != null) refund.setStatusReason(reason);
                    paymentAdapter.save(refund);

                    payment.setRefundedAmount(refundAmount);
                    payment.setRefundReason(reason);
                    payment.setRefundDate(LocalDateTime.now());
                    payment.setStatus(refundAmount.compareTo(payment.getAmount()) >= 0
                            ? Payment.PaymentStatus.REFUNDED
                            : Payment.PaymentStatus.PARTIALLY_REFUNDED);

                    Payment updated = paymentAdapter.save(payment);
                    return ResponseEntity.<Object>ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== VENDOR PAYOUT ====================

    @PatchMapping("/{id}/payout-status")
    public ResponseEntity<?> updatePayoutStatus(@PathVariable @Min(1) Long id,
                                                @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "status field is required"));
        }
        Payment.PayoutStatus payoutStatus;
        try {
            payoutStatus = Payment.PayoutStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid payout status '" + statusStr + "'. Valid values: " +
                            Arrays.toString(Payment.PayoutStatus.values())));
        }
        return paymentAdapter.findById(id)
                .map(existing -> {
                    existing.setVendorPayoutStatus(payoutStatus);
                    if (body.get("reference") != null) {
                        existing.setVendorPayoutReference(body.get("reference"));
                    }
                    if (payoutStatus == Payment.PayoutStatus.COMPLETED) {
                        existing.setVendorPayoutDate(LocalDateTime.now());
                    }
                    Payment updated = paymentAdapter.save(existing);
                    return ResponseEntity.<Object>ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        if (paymentAdapter.findById(id).isPresent()) {
            paymentAdapter.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ==================== HELPER METHODS ====================

    private void applyUpdates(Payment existing, PaymentUpdateRequest req) {
        if (req.status() != null) existing.setStatus(req.status());
        if (req.statusReason() != null) existing.setStatusReason(req.statusReason());
        if (req.gatewayPaymentId() != null) existing.setGatewayPaymentId(req.gatewayPaymentId());
        if (req.gatewayChargeId() != null) existing.setGatewayChargeId(req.gatewayChargeId());
        if (req.gatewayResponseCode() != null) existing.setGatewayResponseCode(req.gatewayResponseCode());
        if (req.gatewayResponseMessage() != null) existing.setGatewayResponseMessage(req.gatewayResponseMessage());
        if (req.cardBrand() != null) existing.setCardBrand(req.cardBrand());
        if (req.cardLastFour() != null) existing.setCardLastFour(req.cardLastFour());
        if (req.cardExpMonth() != null) existing.setCardExpMonth(req.cardExpMonth());
        if (req.cardExpYear() != null) existing.setCardExpYear(req.cardExpYear());
        if (req.cardHolderName() != null) existing.setCardHolderName(req.cardHolderName());
        if (req.cardFingerprint() != null) existing.setCardFingerprint(req.cardFingerprint());
        if (req.authStatus() != null) existing.setAuthStatus(req.authStatus());
        if (req.authValue() != null) existing.setAuthValue(req.authValue());
        if (req.authEci() != null) existing.setAuthEci(req.authEci());
        if (req.vendorPayoutStatus() != null) existing.setVendorPayoutStatus(req.vendorPayoutStatus());
        if (req.vendorPayoutReference() != null) existing.setVendorPayoutReference(req.vendorPayoutReference());
        if (req.vendorPayoutDate() != null) existing.setVendorPayoutDate(req.vendorPayoutDate());
        if (req.refundedAmount() != null) existing.setRefundedAmount(req.refundedAmount());
        if (req.refundReason() != null) existing.setRefundReason(req.refundReason());
        if (req.refundDate() != null) existing.setRefundDate(req.refundDate());
        if (req.settlementStatus() != null) existing.setSettlementStatus(req.settlementStatus());
        if (req.settlementReference() != null) existing.setSettlementReference(req.settlementReference());
        if (req.settlementDate() != null) existing.setSettlementDate(req.settlementDate());
        if (req.riskScore() != null) existing.setRiskScore(req.riskScore());
        if (req.fraudStatus() != null) existing.setFraudStatus(req.fraudStatus());
        if (req.fraudReason() != null) existing.setFraudReason(req.fraudReason());
        if (req.receiptUrl() != null) existing.setReceiptUrl(req.receiptUrl());
        if (req.internalNotes() != null) existing.setInternalNotes(req.internalNotes());
        if (req.failureReason() != null) existing.setFailureReason(req.failureReason());
        if (req.processedBy() != null) existing.setProcessedBy(req.processedBy());
        if (req.processedAt() != null) existing.setProcessedAt(req.processedAt());
        if (req.capturedAt() != null) existing.setCapturedAt(req.capturedAt());
    }
}
