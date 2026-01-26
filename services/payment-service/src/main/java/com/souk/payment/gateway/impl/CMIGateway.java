package com.souk.payment.gateway.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import com.souk.payment.gateway.model.GatewayResponse.GatewayStatus;
import com.souk.payment.gateway.model.GatewayResponse.AuthenticationStatus;
import com.souk.payment.gateway.config.GatewayProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CMI (Centre Monétique Interbancaire) payment gateway implementation.
 * Handles Morocco's interbank payment network API interactions.
 */
@Component
public class CMIGateway extends AbstractPaymentGateway {

    private static final String GATEWAY_ID = "CMI";
    private static final String DISPLAY_NAME = "CMI (Morocco)";

    private static final Set<String> SUPPORTED_METHODS = Set.of(
            "CARD", "CREDIT_CARD", "DEBIT_CARD", "CMI_CARD"
    );

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "MAD", "USD", "EUR"
    );

    private final GatewayProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CMIGateway(GatewayProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            log.info("CMI gateway initialized - Endpoint: {}", properties.getCmi().getApiUrl());
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
        return properties.getCmi() != null && properties.getCmi().isEnabled();
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
            Map<String, String> params = buildPaymentParams(request, "PAYMENT");
            String response = sendRequest("/payment/process", params);
            GatewayResponse gatewayResponse = parsePaymentResponse(response);
            logResult("CHARGE", gatewayResponse);
            return gatewayResponse;

        } catch (Exception e) {
            return handleException("CHARGE", e);
        }
    }

    @Override
    public GatewayResponse authorize(GatewayRequest request) {
        validateRequest(request);
        logOperation("AUTHORIZE", request);

        try {
            Map<String, String> params = buildPaymentParams(request, "AUTHORIZATION");
            String response = sendRequest("/payment/authorize", params);
            GatewayResponse gatewayResponse = parsePaymentResponse(response);
            logResult("AUTHORIZE", gatewayResponse);
            return gatewayResponse;

        } catch (Exception e) {
            return handleException("AUTHORIZE", e);
        }
    }

    @Override
    public GatewayResponse capture(String gatewayPaymentId, BigDecimal amount) {
        log.info("[{}] CAPTURE - PaymentId: {}, Amount: {}", GATEWAY_ID, gatewayPaymentId, amount);

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("merchantId", properties.getCmi().getMerchantId());
            params.put("transactionId", gatewayPaymentId);
            params.put("amount", formatAmount(amount));
            params.put("currency", "504"); // MAD currency code
            params.put("timestamp", getTimestamp());

            String signature = generateSignature(params);
            params.put("signature", signature);

            String response = sendRequest("/payment/capture", params);
            GatewayResponse gatewayResponse = parsePaymentResponse(response);
            gatewayResponse.setCapturedAt(Instant.now());
            logResult("CAPTURE", gatewayResponse);
            return gatewayResponse;

        } catch (Exception e) {
            return handleException("CAPTURE", e);
        }
    }

    @Override
    public GatewayResponse cancel(String gatewayPaymentId) {
        log.info("[{}] CANCEL - PaymentId: {}", GATEWAY_ID, gatewayPaymentId);

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("merchantId", properties.getCmi().getMerchantId());
            params.put("transactionId", gatewayPaymentId);
            params.put("timestamp", getTimestamp());

            String signature = generateSignature(params);
            params.put("signature", signature);

            String response = sendRequest("/payment/cancel", params);
            GatewayResponse gatewayResponse = parsePaymentResponse(response);
            logResult("CANCEL", gatewayResponse);
            return gatewayResponse;

        } catch (Exception e) {
            return handleException("CANCEL", e);
        }
    }

    @Override
    public GatewayResponse refund(String gatewayPaymentId, BigDecimal amount, String reason) {
        log.info("[{}] REFUND - PaymentId: {}, Amount: {}, Reason: {}",
                GATEWAY_ID, gatewayPaymentId, amount, reason);

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("merchantId", properties.getCmi().getMerchantId());
            params.put("originalTransactionId", gatewayPaymentId);
            params.put("amount", formatAmount(amount));
            params.put("currency", "504");
            params.put("reason", reason != null ? reason : "Customer request");
            params.put("timestamp", getTimestamp());

            String signature = generateSignature(params);
            params.put("signature", signature);

            String response = sendRequest("/payment/refund", params);
            GatewayResponse gatewayResponse = parsePaymentResponse(response);
            logResult("REFUND", gatewayResponse);
            return gatewayResponse;

        } catch (Exception e) {
            return handleException("REFUND", e);
        }
    }

    // ==================== STATUS & VERIFICATION ====================

    @Override
    public GatewayResponse getStatus(String gatewayPaymentId) {
        log.info("[{}] GET_STATUS - PaymentId: {}", GATEWAY_ID, gatewayPaymentId);

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("merchantId", properties.getCmi().getMerchantId());
            params.put("transactionId", gatewayPaymentId);
            params.put("timestamp", getTimestamp());

            String signature = generateSignature(params);
            params.put("signature", signature);

            String response = sendRequest("/payment/status", params);
            return parsePaymentResponse(response);

        } catch (Exception e) {
            return handleException("GET_STATUS", e);
        }
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        try {
            String expectedSignature = calculateHmac(payload, properties.getCmi().getSecretKey());
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.warn("CMI webhook verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GatewayResponse parseWebhook(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);

            String transactionId = json.path("transactionId").asText();
            String status = json.path("status").asText();
            String responseCode = json.path("responseCode").asText();

            GatewayResponse.Builder builder = GatewayResponse.builder()
                    .gatewayName(GATEWAY_ID)
                    .gatewayPaymentId(transactionId)
                    .rawResponse(payload)
                    .processedAt(Instant.now());

            mapCMIStatus(status, responseCode, builder);

            if (json.has("amount")) {
                builder.amountCharged(new BigDecimal(json.path("amount").asText()));
            }
            if (json.has("currency")) {
                builder.currency(mapCurrencyCode(json.path("currency").asText()));
            }

            return builder.build();

        } catch (Exception e) {
            return handleException("PARSE_WEBHOOK", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private Map<String, String> buildPaymentParams(GatewayRequest request, String transactionType) {
        Map<String, String> params = new LinkedHashMap<>();

        params.put("merchantId", properties.getCmi().getMerchantId());
        params.put("terminalId", properties.getCmi().getTerminalId());
        params.put("transactionType", transactionType);
        params.put("amount", formatAmount(request.getAmount()));
        params.put("currency", mapToCurrencyCode(request.getCurrency()));
        params.put("orderId", String.valueOf(request.getPaymentId()));
        params.put("timestamp", getTimestamp());

        // Card details (if direct integration)
        if (request.getPaymentToken() != null) {
            params.put("token", request.getPaymentToken());
        }

        // Customer info
        if (request.getCustomerEmail() != null) {
            params.put("email", request.getCustomerEmail());
        }
        if (request.getCustomerName() != null) {
            params.put("customerName", request.getCustomerName());
        }

        // Billing address
        if (request.getBillingCity() != null) {
            params.put("city", request.getBillingCity());
        }
        if (request.getBillingCountry() != null) {
            params.put("country", request.getBillingCountry());
        }

        // 3DS return URL
        if (request.getReturnUrl() != null) {
            params.put("returnUrl", request.getReturnUrl());
        }
        if (request.getCallbackUrl() != null) {
            params.put("callbackUrl", request.getCallbackUrl());
        }

        // Generate signature
        String signature = generateSignature(params);
        params.put("signature", signature);

        return params;
    }

    private String sendRequest(String endpoint, Map<String, String> params) {
        String url = properties.getCmi().getApiUrl() + endpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Merchant-Id", properties.getCmi().getMerchantId());

        try {
            String jsonBody = objectMapper.writeValueAsString(params);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("CMI API request failed: {}", e.getMessage());
            throw new RuntimeException("CMI API request failed", e);
        } catch (Exception e) {
            log.error("Error building CMI request: {}", e.getMessage());
            throw new RuntimeException("Error building CMI request", e);
        }
    }

    private GatewayResponse parsePaymentResponse(String responseBody) throws Exception {
        JsonNode json = objectMapper.readTree(responseBody);

        String transactionId = json.path("transactionId").asText(null);
        String status = json.path("status").asText();
        String responseCode = json.path("responseCode").asText();
        String responseMessage = json.path("responseMessage").asText(null);

        GatewayResponse.Builder builder = GatewayResponse.builder()
                .gatewayName(GATEWAY_ID)
                .gatewayPaymentId(transactionId)
                .rawResponse(responseBody)
                .processedAt(Instant.now());

        mapCMIStatus(status, responseCode, builder);

        if (!"00".equals(responseCode) && !"000".equals(responseCode)) {
            builder.errorCode(responseCode);
            builder.errorMessage(responseMessage);
        }

        // Amount
        if (json.has("amount")) {
            builder.amountCharged(new BigDecimal(json.path("amount").asText()));
        }

        // Currency
        if (json.has("currency")) {
            builder.currency(mapCurrencyCode(json.path("currency").asText()));
        }

        // Card details (masked)
        if (json.has("cardNumber")) {
            String maskedCard = json.path("cardNumber").asText();
            if (maskedCard.length() >= 4) {
                builder.cardLastFour(maskedCard.substring(maskedCard.length() - 4));
            }
        }
        if (json.has("cardBrand")) {
            builder.cardBrand(json.path("cardBrand").asText());
        }

        // 3DS redirect
        if (json.has("redirectUrl")) {
            builder.authUrl(json.path("redirectUrl").asText());
            builder.authStatus(AuthenticationStatus.PENDING);
        }

        // Authorization code
        if (json.has("authCode")) {
            builder.gatewayChargeId(json.path("authCode").asText());
        }

        return builder.build();
    }

    private void mapCMIStatus(String status, String responseCode, GatewayResponse.Builder builder) {
        if ("00".equals(responseCode) || "000".equals(responseCode)) {
            // Success codes
            switch (status.toUpperCase()) {
                case "APPROVED", "SUCCESS", "CAPTURED" -> builder.success(true).status(GatewayStatus.SUCCEEDED);
                case "AUTHORIZED" -> builder.success(true).status(GatewayStatus.REQUIRES_CAPTURE);
                case "PENDING" -> builder.success(false).status(GatewayStatus.PENDING);
                case "3DS_REQUIRED", "REDIRECT" -> builder.success(false).status(GatewayStatus.REQUIRES_ACTION);
                case "REFUNDED" -> builder.success(true).status(GatewayStatus.REFUNDED);
                case "CANCELLED", "VOIDED" -> builder.success(true).status(GatewayStatus.CANCELLED);
                default -> builder.success(true).status(GatewayStatus.SUCCEEDED);
            }
        } else {
            // Error codes
            builder.success(false).status(GatewayStatus.FAILED);
        }
    }

    private String generateSignature(Map<String, String> params) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }

            return calculateHmac(sb.toString(), properties.getCmi().getSecretKey());

        } catch (Exception e) {
            log.error("Failed to generate CMI signature: {}", e.getMessage());
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    private String calculateHmac(String data, String key) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private String formatAmount(BigDecimal amount) {
        // CMI expects amount in minor units (centimes)
        return String.valueOf(amount.movePointRight(2).longValue());
    }

    private String mapToCurrencyCode(String currency) {
        return switch (currency.toUpperCase()) {
            case "MAD" -> "504";
            case "USD" -> "840";
            case "EUR" -> "978";
            default -> "504"; // Default to MAD
        };
    }

    private String mapCurrencyCode(String code) {
        return switch (code) {
            case "504" -> "MAD";
            case "840" -> "USD";
            case "978" -> "EUR";
            default -> code;
        };
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}
