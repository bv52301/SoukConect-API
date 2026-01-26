package com.souk.payment.gateway.impl;

import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import com.souk.payment.gateway.model.GatewayResponse.GatewayStatus;
import com.souk.payment.gateway.model.GatewayResponse.AuthenticationStatus;
import com.souk.payment.gateway.config.GatewayProperties;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stripe payment gateway implementation.
 * Handles all Stripe API interactions for payments.
 */
@Component
public class StripeGateway extends AbstractPaymentGateway {

    private static final String GATEWAY_ID = "STRIPE";
    private static final String DISPLAY_NAME = "Stripe";

    private static final Set<String> SUPPORTED_METHODS = Set.of(
            "CARD", "CREDIT_CARD", "DEBIT_CARD"
    );

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "MAD", "CAD", "AUD", "JPY", "CHF", "SEK", "NOK", "DKK"
    );

    private final GatewayProperties properties;

    public StripeGateway(GatewayProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            Stripe.apiKey = properties.getStripe().getSecretKey();
            log.info("Stripe gateway initialized");
        }
    }

    @Override
    public String getGatewayId() {
        return GATEWAY_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean isEnabled() {
        return properties.getStripe() != null && properties.getStripe().isEnabled();
    }

    @Override
    public boolean supports(String paymentMethodType) {
        return SUPPORTED_METHODS.contains(paymentMethodType.toUpperCase());
    }

    @Override
    public boolean supportsCurrency(String currency) {
        return SUPPORTED_CURRENCIES.contains(currency.toUpperCase());
    }

    // ==================== PAYMENT OPERATIONS ====================

    @Override
    public GatewayResponse charge(GatewayRequest request) {
        validateRequest(request);
        logOperation("CHARGE", request);

        try {
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(request.getAmount(), request.getCurrency()))
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    );

            // Set payment method if provided
            if (request.getPaymentToken() != null) {
                paramsBuilder.setPaymentMethod(request.getPaymentToken());
            }

            // Set customer if provided
            if (request.getGatewayCustomerId() != null) {
                paramsBuilder.setCustomer(request.getGatewayCustomerId());
            }

            // Set idempotency key
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.getIdempotencyKey())
                    .build();

            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("payment_id", String.valueOf(request.getPaymentId()));
            if (request.getOrderId() != null) {
                metadata.put("order_id", String.valueOf(request.getOrderId()));
            }
            paramsBuilder.putAllMetadata(metadata);

            // Set description
            if (request.getDescription() != null) {
                paramsBuilder.setDescription(request.getDescription());
            }

            // Set return URL for 3DS
            if (request.getReturnUrl() != null) {
                paramsBuilder.setReturnUrl(request.getReturnUrl());
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build(), options);

            GatewayResponse response = mapPaymentIntentToResponse(paymentIntent);
            logResult("CHARGE", response);
            return response;

        } catch (StripeException e) {
            return handleStripeException("CHARGE", e);
        } catch (Exception e) {
            return handleException("CHARGE", e);
        }
    }

    @Override
    public GatewayResponse authorize(GatewayRequest request) {
        validateRequest(request);
        logOperation("AUTHORIZE", request);

        try {
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(request.getAmount(), request.getCurrency()))
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    );

            if (request.getPaymentToken() != null) {
                paramsBuilder.setPaymentMethod(request.getPaymentToken());
            }

            if (request.getGatewayCustomerId() != null) {
                paramsBuilder.setCustomer(request.getGatewayCustomerId());
            }

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.getIdempotencyKey())
                    .build();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("payment_id", String.valueOf(request.getPaymentId()));
            paramsBuilder.putAllMetadata(metadata);

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build(), options);

            GatewayResponse response = mapPaymentIntentToResponse(paymentIntent);
            logResult("AUTHORIZE", response);
            return response;

        } catch (StripeException e) {
            return handleStripeException("AUTHORIZE", e);
        } catch (Exception e) {
            return handleException("AUTHORIZE", e);
        }
    }

    @Override
    public GatewayResponse capture(String gatewayPaymentId, BigDecimal amount) {
        log.info("[{}] CAPTURE - PaymentId: {}, Amount: {}", GATEWAY_ID, gatewayPaymentId, amount);

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(gatewayPaymentId);

            PaymentIntentCaptureParams.Builder paramsBuilder = PaymentIntentCaptureParams.builder();
            if (amount != null) {
                String currency = paymentIntent.getCurrency();
                paramsBuilder.setAmountToCapture(toMinorUnits(amount, currency));
            }

            paymentIntent = paymentIntent.capture(paramsBuilder.build());

            GatewayResponse response = mapPaymentIntentToResponse(paymentIntent);
            response.setCapturedAt(Instant.now());
            logResult("CAPTURE", response);
            return response;

        } catch (StripeException e) {
            return handleStripeException("CAPTURE", e);
        } catch (Exception e) {
            return handleException("CAPTURE", e);
        }
    }

    @Override
    public GatewayResponse cancel(String gatewayPaymentId) {
        log.info("[{}] CANCEL - PaymentId: {}", GATEWAY_ID, gatewayPaymentId);

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(gatewayPaymentId);
            paymentIntent = paymentIntent.cancel();

            GatewayResponse response = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayStatus.CANCELLED)
                    .gatewayName(GATEWAY_ID)
                    .gatewayPaymentId(paymentIntent.getId())
                    .processedAt(Instant.now())
                    .build();

            logResult("CANCEL", response);
            return response;

        } catch (StripeException e) {
            return handleStripeException("CANCEL", e);
        } catch (Exception e) {
            return handleException("CANCEL", e);
        }
    }

    @Override
    public GatewayResponse refund(String gatewayPaymentId, BigDecimal amount, String reason) {
        log.info("[{}] REFUND - PaymentId: {}, Amount: {}, Reason: {}",
                GATEWAY_ID, gatewayPaymentId, amount, reason);

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(gatewayPaymentId);
            String currency = paymentIntent.getCurrency();

            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(gatewayPaymentId);

            if (amount != null) {
                paramsBuilder.setAmount(toMinorUnits(amount, currency));
            }

            if (reason != null) {
                paramsBuilder.putMetadata("reason", reason);
            }

            Refund refund = Refund.create(paramsBuilder.build());

            GatewayResponse response = GatewayResponse.builder()
                    .success("succeeded".equals(refund.getStatus()))
                    .status("succeeded".equals(refund.getStatus()) ?
                            GatewayStatus.REFUNDED : GatewayStatus.PENDING)
                    .gatewayName(GATEWAY_ID)
                    .gatewayPaymentId(refund.getId())
                    .amountRefunded(fromMinorUnits(refund.getAmount(), currency))
                    .currency(currency.toUpperCase())
                    .processedAt(Instant.now())
                    .build();

            logResult("REFUND", response);
            return response;

        } catch (StripeException e) {
            return handleStripeException("REFUND", e);
        } catch (Exception e) {
            return handleException("REFUND", e);
        }
    }

    // ==================== STATUS & VERIFICATION ====================

    @Override
    public GatewayResponse getStatus(String gatewayPaymentId) {
        log.info("[{}] GET_STATUS - PaymentId: {}", GATEWAY_ID, gatewayPaymentId);

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(gatewayPaymentId);
            return mapPaymentIntentToResponse(paymentIntent);

        } catch (StripeException e) {
            return handleStripeException("GET_STATUS", e);
        } catch (Exception e) {
            return handleException("GET_STATUS", e);
        }
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        try {
            com.stripe.net.Webhook.constructEvent(
                    payload,
                    signature,
                    properties.getStripe().getWebhookSecret()
            );
            return true;
        } catch (Exception e) {
            log.warn("Webhook verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GatewayResponse parseWebhook(String payload) {
        try {
            // Parse webhook event using Stripe's ApiResource
            Event event = com.stripe.net.ApiResource.GSON.fromJson(payload, Event.class);

            if (event != null && event.getDataObjectDeserializer().getObject().isPresent()) {
                StripeObject stripeObject = event.getDataObjectDeserializer().getObject().get();

                if (stripeObject instanceof PaymentIntent paymentIntent) {
                    GatewayResponse response = mapPaymentIntentToResponse(paymentIntent);
                    response.setRawResponse(payload);
                    return response;
                }
            }

            return GatewayResponse.failure(GATEWAY_ID, "UNKNOWN_EVENT", "Unable to parse webhook event");

        } catch (Exception e) {
            return handleException("PARSE_WEBHOOK", e);
        }
    }

    // ==================== CUSTOMER MANAGEMENT ====================

    @Override
    public String createCustomer(String email, String name) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .build();

            Customer customer = Customer.create(params);
            log.info("[{}] Customer created: {}", GATEWAY_ID, customer.getId());
            return customer.getId();

        } catch (StripeException e) {
            log.error("[{}] Failed to create customer: {}", GATEWAY_ID, e.getMessage());
            throw new RuntimeException("Failed to create Stripe customer", e);
        }
    }

    @Override
    public String savePaymentMethod(String gatewayCustomerId, String paymentToken) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentToken);

            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                    .setCustomer(gatewayCustomerId)
                    .build();

            paymentMethod = paymentMethod.attach(params);
            log.info("[{}] Payment method saved: {}", GATEWAY_ID, paymentMethod.getId());
            return paymentMethod.getId();

        } catch (StripeException e) {
            log.error("[{}] Failed to save payment method: {}", GATEWAY_ID, e.getMessage());
            throw new RuntimeException("Failed to save payment method", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private GatewayResponse mapPaymentIntentToResponse(PaymentIntent pi) {
        GatewayResponse.Builder builder = GatewayResponse.builder()
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId(pi.getId())
                .currency(pi.getCurrency().toUpperCase())
                .amountCharged(fromMinorUnits(pi.getAmount(), pi.getCurrency()))
                .processedAt(Instant.ofEpochSecond(pi.getCreated()));

        // Map status
        switch (pi.getStatus()) {
            case "succeeded" -> builder.success(true).status(GatewayStatus.SUCCEEDED);
            case "processing" -> builder.success(false).status(GatewayStatus.PENDING);
            case "requires_action" -> {
                builder.success(false).status(GatewayStatus.REQUIRES_ACTION);
                if (pi.getNextAction() != null && pi.getNextAction().getRedirectToUrl() != null) {
                    builder.authUrl(pi.getNextAction().getRedirectToUrl().getUrl());
                }
                builder.authStatus(AuthenticationStatus.PENDING);
            }
            case "requires_capture" -> builder.success(true).status(GatewayStatus.REQUIRES_CAPTURE);
            case "canceled" -> builder.success(false).status(GatewayStatus.CANCELLED);
            default -> builder.success(false).status(GatewayStatus.FAILED)
                    .errorMessage("Unknown status: " + pi.getStatus());
        }

        // Extract card details if available
        if (pi.getLatestCharge() != null) {
            try {
                Charge charge = Charge.retrieve(pi.getLatestCharge());
                builder.gatewayChargeId(charge.getId());

                if (charge.getPaymentMethodDetails() != null &&
                    charge.getPaymentMethodDetails().getCard() != null) {
                    var card = charge.getPaymentMethodDetails().getCard();
                    builder.cardBrand(card.getBrand())
                            .cardLastFour(card.getLast4())
                            .cardExpMonth(String.valueOf(card.getExpMonth()))
                            .cardExpYear(String.valueOf(card.getExpYear()));
                }

                if (charge.getReceiptUrl() != null) {
                    builder.receiptUrl(charge.getReceiptUrl());
                }

                // Risk assessment
                if (charge.getOutcome() != null) {
                    builder.riskLevel(charge.getOutcome().getRiskLevel());
                    builder.riskScore(charge.getOutcome().getRiskScore() != null ?
                            charge.getOutcome().getRiskScore().intValue() : null);
                }

            } catch (StripeException e) {
                log.warn("Could not retrieve charge details: {}", e.getMessage());
            }
        }

        // Extract customer ID
        if (pi.getCustomer() != null) {
            builder.gatewayCustomerId(pi.getCustomer());
        }

        return builder.build();
    }

    private GatewayResponse handleStripeException(String operation, StripeException e) {
        log.error("[{}] {} Stripe error: {} - {}", GATEWAY_ID, operation, e.getCode(), e.getMessage());

        String errorCode = e.getCode() != null ? e.getCode() : "STRIPE_ERROR";

        return GatewayResponse.builder()
                .success(false)
                .status(GatewayStatus.FAILED)
                .gatewayName(GATEWAY_ID)
                .errorCode(errorCode)
                .errorMessage(e.getMessage())
                .processedAt(Instant.now())
                .build();
    }
}
