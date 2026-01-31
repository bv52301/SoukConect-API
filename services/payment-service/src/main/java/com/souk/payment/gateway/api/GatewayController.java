package com.souk.payment.gateway.api;

import com.souk.payment.gateway.GatewayService;
import com.souk.payment.gateway.PaymentGateway;
import com.souk.payment.gateway.IPaymentGatewayService;
import com.souk.payment.gateway.api.dto.CaptureRequest;
import com.souk.payment.gateway.api.dto.ChargeRequest;
import com.souk.payment.gateway.api.dto.RefundRequest;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for payment gateway operations.
 * These endpoints are called by BPM workers to process payments.
 *
 * All payment operations:
 * 1. Call external gateway (Stripe/CMI)
 * 2. Update Payment DB record with result
 * 3. Return response to BPM
 */
@RestController
@RequestMapping("/v1/gateway")
@Tag(name = "Payment Gateway", description = "Payment gateway operations for BPM integration")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final GatewayService gatewayService;
    private final IPaymentGatewayService paymentGatewayService;

    public GatewayController(GatewayService gatewayService, IPaymentGatewayService paymentGatewayService) {
        this.gatewayService = gatewayService;
        this.paymentGatewayService = paymentGatewayService;
    }

    // ==================== GATEWAY INFO ====================

    @GetMapping("/available")
    @Operation(summary = "List available gateways", description = "Returns all enabled payment gateways")
    public ResponseEntity<List<GatewayInfo>> getAvailableGateways() {
        List<GatewayInfo> gateways = gatewayService.getEnabledGateways().stream()
                .map(g -> new GatewayInfo(g.getGatewayId(), g.getDisplayName()))
                .toList();
        return ResponseEntity.ok(gateways);
    }

    @GetMapping("/{gatewayId}/supports")
    @Operation(summary = "Check gateway support", description = "Check if gateway supports payment method and currency")
    public ResponseEntity<Map<String, Boolean>> checkSupport(
            @PathVariable String gatewayId,
            @RequestParam String paymentMethod,
            @RequestParam String currency) {

        PaymentGateway gateway = gatewayService.getGateway(gatewayId);
        boolean supportsMethod = gateway.supports(paymentMethod);
        boolean supportsCurrency = gateway.supportsCurrency(currency);

        return ResponseEntity.ok(Map.of(
                "supportsMethod", supportsMethod,
                "supportsCurrency", supportsCurrency,
                "supported", supportsMethod && supportsCurrency
        ));
    }

    // ==================== PAYMENT OPERATIONS (with DB update) ====================

    @PostMapping("/{gatewayId}/charge")
    @Operation(summary = "Process payment",
               description = "Charge a payment through gateway and update DB with result")
    public ResponseEntity<GatewayResponse> charge(
            @PathVariable String gatewayId,
            @Valid @RequestBody ChargeRequest request) {

        log.info("Charge request - Gateway: {}, PaymentId: {}, Amount: {} {}",
                gatewayId, request.paymentId(), request.amount(), request.currency());

        GatewayRequest gatewayRequest = mapToGatewayRequest(request);
        GatewayResponse response = paymentGatewayService.charge(gatewayId, request.paymentId(), gatewayRequest);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gatewayId}/authorize")
    @Operation(summary = "Authorize payment",
               description = "Authorize (hold) payment and update DB with result")
    public ResponseEntity<GatewayResponse> authorize(
            @PathVariable String gatewayId,
            @Valid @RequestBody ChargeRequest request) {

        log.info("Authorize request - Gateway: {}, PaymentId: {}, Amount: {} {}",
                gatewayId, request.paymentId(), request.amount(), request.currency());

        GatewayRequest gatewayRequest = mapToGatewayRequest(request);
        GatewayResponse response = paymentGatewayService.authorize(gatewayId, request.paymentId(), gatewayRequest);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gatewayId}/capture")
    @Operation(summary = "Capture payment",
               description = "Capture authorized payment and update DB with result")
    public ResponseEntity<GatewayResponse> capture(
            @PathVariable String gatewayId,
            @Valid @RequestBody CaptureRequest request) {

        log.info("Capture request - Gateway: {}, PaymentId: {}, GatewayPaymentId: {}, Amount: {}",
                gatewayId, request.paymentId(), request.gatewayPaymentId(), request.amount());

        GatewayResponse response = paymentGatewayService.capture(
                gatewayId, request.paymentId(), request.gatewayPaymentId(), request.amount());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gatewayId}/cancel")
    @Operation(summary = "Cancel payment",
               description = "Cancel/void authorized payment and update DB")
    public ResponseEntity<GatewayResponse> cancel(
            @PathVariable String gatewayId,
            @RequestParam Long paymentId,
            @RequestParam String gatewayPaymentId) {

        log.info("Cancel request - Gateway: {}, PaymentId: {}, GatewayPaymentId: {}",
                gatewayId, paymentId, gatewayPaymentId);

        GatewayResponse response = paymentGatewayService.cancel(gatewayId, paymentId, gatewayPaymentId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gatewayId}/refund")
    @Operation(summary = "Refund payment",
               description = "Refund captured payment and update DB with result")
    public ResponseEntity<GatewayResponse> refund(
            @PathVariable String gatewayId,
            @Valid @RequestBody RefundRequest request) {

        log.info("Refund request - Gateway: {}, PaymentId: {}, GatewayPaymentId: {}, Amount: {}",
                gatewayId, request.paymentId(), request.gatewayPaymentId(), request.amount());

        GatewayResponse response = paymentGatewayService.refund(
                gatewayId, request.paymentId(), request.gatewayPaymentId(),
                request.amount(), request.reason());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gatewayId}/status/{gatewayPaymentId}")
    @Operation(summary = "Get payment status",
               description = "Get current status from gateway (read-only, no DB update)")
    public ResponseEntity<GatewayResponse> getStatus(
            @PathVariable String gatewayId,
            @PathVariable String gatewayPaymentId) {

        log.debug("Status request - Gateway: {}, GatewayPaymentId: {}", gatewayId, gatewayPaymentId);

        GatewayResponse response = gatewayService.getStatus(gatewayId, gatewayPaymentId);

        return ResponseEntity.ok(response);
    }

    // ==================== WEBHOOKS ====================

    @PostMapping("/{gatewayId}/webhook")
    @Operation(summary = "Handle webhook",
               description = "Process webhook from gateway and update DB")
    public ResponseEntity<GatewayResponse> handleWebhook(
            @PathVariable String gatewayId,
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestHeader(value = "X-CMI-Signature", required = false) String cmiSignature) {

        log.info("Webhook received from gateway: {}", gatewayId);

        String signature = switch (gatewayId.toUpperCase()) {
            case "STRIPE" -> stripeSignature;
            case "CMI" -> cmiSignature;
            default -> null;
        };

        GatewayResponse response = paymentGatewayService.handleWebhook(gatewayId, payload, signature);

        if (!response.isSuccess() && "INVALID_SIGNATURE".equals(response.getErrorCode())) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ==================== CUSTOMER MANAGEMENT ====================

    @PostMapping("/{gatewayId}/customer")
    @Operation(summary = "Create customer",
               description = "Create a customer at gateway for saved payment methods")
    public ResponseEntity<Map<String, String>> createCustomer(
            @PathVariable String gatewayId,
            @RequestParam String email,
            @RequestParam String name) {

        log.info("Create customer - Gateway: {}, Email: {}", gatewayId, email);

        String customerId = gatewayService.createCustomer(gatewayId, email, name);

        return ResponseEntity.ok(Map.of("gatewayCustomerId", customerId));
    }

    @PostMapping("/{gatewayId}/customer/{gatewayCustomerId}/payment-method")
    @Operation(summary = "Save payment method",
               description = "Save a payment method for a customer")
    public ResponseEntity<Map<String, String>> savePaymentMethod(
            @PathVariable String gatewayId,
            @PathVariable String gatewayCustomerId,
            @RequestParam String paymentToken) {

        log.info("Save payment method - Gateway: {}, CustomerId: {}", gatewayId, gatewayCustomerId);

        String paymentMethodId = gatewayService.savePaymentMethod(gatewayId, gatewayCustomerId, paymentToken);

        return ResponseEntity.ok(Map.of("paymentMethodId", paymentMethodId));
    }

    // ==================== HELPER METHODS ====================

    private GatewayRequest mapToGatewayRequest(ChargeRequest request) {
        return GatewayRequest.builder()
                .paymentId(request.paymentId())
                .idempotencyKey(request.idempotencyKey())
                .amount(request.amount())
                .currency(request.currency())
                .paymentToken(request.paymentToken())
                .gatewayCustomerId(request.gatewayCustomerId())
                .orderId(request.orderId() != null ? String.valueOf(request.orderId()) : null)
                .description(request.description())
                .customerEmail(request.customerEmail())
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .billingLine1(request.billingLine1())
                .billingLine2(request.billingLine2())
                .billingCity(request.billingCity())
                .billingState(request.billingState())
                .billingPostalCode(request.billingPostalCode())
                .billingCountry(request.billingCountry())
                .returnUrl(request.returnUrl())
                .callbackUrl(request.callbackUrl())
                .require3DS(request.require3DS())
                .build();
    }

    public record GatewayInfo(String id, String displayName) {}
}
