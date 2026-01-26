package com.souk.payment.gateway.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.payment.gateway.config.GatewayProperties;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CMIGateway")
class CMIGatewayTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private GatewayProperties properties;
    private CMIGateway cmiGateway;

    @Captor
    private ArgumentCaptor<HttpEntity<String>> httpEntityCaptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        properties = new GatewayProperties();
        GatewayProperties.CMIProperties cmiProps = new GatewayProperties.CMIProperties();
        cmiProps.setEnabled(true);
        cmiProps.setApiUrl("https://api.cmi.ma");
        cmiProps.setMerchantId("MERCHANT123");
        cmiProps.setTerminalId("TERM001");
        cmiProps.setSecretKey("secret123");
        cmiProps.setStoreKey("store123");
        properties.setCmi(cmiProps);

        cmiGateway = new CMIGateway(properties, restTemplate, objectMapper);
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("should return correct gateway ID")
        void shouldReturnGatewayId() {
            assertThat(cmiGateway.getGatewayId()).isEqualTo("CMI");
        }

        @Test
        @DisplayName("should return display name")
        void shouldReturnDisplayName() {
            assertThat(cmiGateway.getDisplayName()).isEqualTo("CMI (Morocco)");
        }

        @Test
        @DisplayName("should be enabled when properties are set")
        void shouldBeEnabledWhenPropertiesSet() {
            assertThat(cmiGateway.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should be disabled when enabled flag is false")
        void shouldBeDisabledWhenFlagFalse() {
            properties.getCmi().setEnabled(false);
            cmiGateway = new CMIGateway(properties, restTemplate, objectMapper);

            assertThat(cmiGateway.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should be disabled when CMI properties are null")
        void shouldBeDisabledWhenPropertiesNull() {
            properties.setCmi(null);
            cmiGateway = new CMIGateway(properties, restTemplate, objectMapper);

            assertThat(cmiGateway.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Payment Method Support")
    class PaymentMethodSupport {

        @Test
        @DisplayName("should support CARD payment method")
        void shouldSupportCard() {
            assertThat(cmiGateway.supports("CARD")).isTrue();
        }

        @Test
        @DisplayName("should support CMI_CARD payment method")
        void shouldSupportCMICard() {
            assertThat(cmiGateway.supports("CMI_CARD")).isTrue();
        }

        @Test
        @DisplayName("should be case-insensitive for payment methods")
        void shouldBeCaseInsensitive() {
            assertThat(cmiGateway.supports("card")).isTrue();
            assertThat(cmiGateway.supports("Card")).isTrue();
        }

        @Test
        @DisplayName("should not support unsupported payment methods")
        void shouldNotSupportUnsupportedMethods() {
            assertThat(cmiGateway.supports("PAYPAL")).isFalse();
            assertThat(cmiGateway.supports("CRYPTO")).isFalse();
        }
    }

    @Nested
    @DisplayName("Currency Support")
    class CurrencySupport {

        @Test
        @DisplayName("should support MAD currency")
        void shouldSupportMAD() {
            assertThat(cmiGateway.supportsCurrency("MAD")).isTrue();
        }

        @Test
        @DisplayName("should support USD and EUR")
        void shouldSupportUSDAndEUR() {
            assertThat(cmiGateway.supportsCurrency("USD")).isTrue();
            assertThat(cmiGateway.supportsCurrency("EUR")).isTrue();
        }

        @Test
        @DisplayName("should be case-insensitive for currencies")
        void shouldBeCaseInsensitive() {
            assertThat(cmiGateway.supportsCurrency("mad")).isTrue();
        }

        @Test
        @DisplayName("should not support unsupported currencies")
        void shouldNotSupportUnsupportedCurrencies() {
            assertThat(cmiGateway.supportsCurrency("GBP")).isFalse();
            assertThat(cmiGateway.supportsCurrency("JPY")).isFalse();
        }
    }

    @Nested
    @DisplayName("Charge Operation")
    class ChargeOperation {

        @Test
        @DisplayName("should process successful charge")
        void shouldProcessSuccessfulCharge() throws Exception {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("100.00"))
                    .currency("MAD")
                    .paymentToken("tok_test")
                    .customerEmail("test@example.com")
                    .build();

            String apiResponse = """
                {
                    "transactionId": "CMI123456",
                    "status": "APPROVED",
                    "responseCode": "00",
                    "amount": "10000",
                    "currency": "504",
                    "cardBrand": "VISA",
                    "cardNumber": "****1234"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.charge(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGatewayPaymentId()).isEqualTo("CMI123456");
            assertThat(response.getCardBrand()).isEqualTo("VISA");
            assertThat(response.getCardLastFour()).isEqualTo("1234");

            verify(restTemplate).exchange(
                    eq("https://api.cmi.ma/payment/process"),
                    eq(HttpMethod.POST),
                    httpEntityCaptor.capture(),
                    eq(String.class)
            );

            HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
            assertThat(capturedEntity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            assertThat(capturedEntity.getHeaders().get("X-Merchant-Id")).contains("MERCHANT123");
        }

        @Test
        @DisplayName("should handle 3DS redirect response")
        void shouldHandle3DSRedirect() throws Exception {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("100.00"))
                    .currency("MAD")
                    .paymentToken("tok_test")
                    .returnUrl("https://mystore.ma/return")
                    .build();

            String apiResponse = """
                {
                    "transactionId": "CMI123456",
                    "status": "3DS_REQUIRED",
                    "responseCode": "00",
                    "redirectUrl": "https://cmi.ma/3ds/verify"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.charge(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.requiresCustomerAction()).isTrue();
            assertThat(response.getAuthUrl()).isEqualTo("https://cmi.ma/3ds/verify");
        }

        @Test
        @DisplayName("should handle declined transaction")
        void shouldHandleDeclinedTransaction() throws Exception {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("100.00"))
                    .currency("MAD")
                    .paymentToken("tok_test")
                    .build();

            String apiResponse = """
                {
                    "transactionId": "CMI123456",
                    "status": "DECLINED",
                    "responseCode": "05",
                    "responseMessage": "Do not honor"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.charge(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(GatewayResponse.GatewayStatus.FAILED);
            assertThat(response.getErrorCode()).isEqualTo("05");
            assertThat(response.getErrorMessage()).isEqualTo("Do not honor");
        }
    }

    @Nested
    @DisplayName("Authorize Operation")
    class AuthorizeOperation {

        @Test
        @DisplayName("should process successful authorization")
        void shouldProcessSuccessfulAuthorization() throws Exception {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("100.00"))
                    .currency("MAD")
                    .paymentToken("tok_test")
                    .build();

            String apiResponse = """
                {
                    "transactionId": "CMI123456",
                    "status": "AUTHORIZED",
                    "responseCode": "00",
                    "authCode": "AUTH123"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.authorize(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.requiresCapture()).isTrue();
            assertThat(response.getGatewayChargeId()).isEqualTo("AUTH123");

            verify(restTemplate).exchange(
                    eq("https://api.cmi.ma/payment/authorize"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("Capture Operation")
    class CaptureOperation {

        @Test
        @DisplayName("should capture authorized payment")
        void shouldCaptureAuthorizedPayment() throws Exception {
            String apiResponse = """
                {
                    "transactionId": "CMI123456",
                    "status": "CAPTURED",
                    "responseCode": "00"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.capture("CMI123456", new BigDecimal("100.00"));

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCapturedAt()).isNotNull();

            verify(restTemplate).exchange(
                    eq("https://api.cmi.ma/payment/capture"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("Refund Operation")
    class RefundOperation {

        @Test
        @DisplayName("should process refund successfully")
        void shouldProcessRefund() throws Exception {
            String apiResponse = """
                {
                    "transactionId": "REF123456",
                    "status": "REFUNDED",
                    "responseCode": "00",
                    "amount": "5000"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.refund("CMI123456", new BigDecimal("50.00"), "Customer request");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(GatewayResponse.GatewayStatus.REFUNDED);

            verify(restTemplate).exchange(
                    eq("https://api.cmi.ma/payment/refund"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("Cancel Operation")
    class CancelOperation {

        @Test
        @DisplayName("should cancel payment successfully")
        void shouldCancelPayment() throws Exception {
            String apiResponse = """
                {
                    "transactionId": "CMI123456",
                    "status": "CANCELLED",
                    "responseCode": "00"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.cancel("CMI123456");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(GatewayResponse.GatewayStatus.CANCELLED);

            verify(restTemplate).exchange(
                    eq("https://api.cmi.ma/payment/cancel"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("GetStatus Operation")
    class GetStatusOperation {

        @Test
        @DisplayName("should get payment status")
        void shouldGetPaymentStatus() throws Exception {
            String apiResponse = """
                {
                    "transactionId": "CMI123456",
                    "status": "APPROVED",
                    "responseCode": "00",
                    "amount": "10000",
                    "currency": "504"
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            GatewayResponse response = cmiGateway.getStatus("CMI123456");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGatewayPaymentId()).isEqualTo("CMI123456");

            verify(restTemplate).exchange(
                    eq("https://api.cmi.ma/payment/status"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("Webhook Processing")
    class WebhookProcessing {

        @Test
        @DisplayName("should parse webhook successfully")
        void shouldParseWebhook() {
            String payload = """
                {
                    "transactionId": "CMI123456",
                    "status": "APPROVED",
                    "responseCode": "00",
                    "amount": "10000",
                    "currency": "504"
                }
                """;

            GatewayResponse response = cmiGateway.parseWebhook(payload);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGatewayPaymentId()).isEqualTo("CMI123456");
            assertThat(response.getCurrency()).isEqualTo("MAD");
        }

        @Test
        @DisplayName("should verify webhook signature")
        void shouldVerifyWebhookSignature() throws Exception {
            // The CMI gateway uses HMAC-SHA256 for signature verification
            // This test verifies the signature check logic works

            // With invalid signature
            assertThat(cmiGateway.verifyWebhook("test-payload", "invalid-signature")).isFalse();
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidation {

        @Test
        @DisplayName("should throw for null request")
        void shouldThrowForNullRequest() {
            assertThatThrownBy(() -> cmiGateway.charge(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Gateway request cannot be null");
        }

        @Test
        @DisplayName("should throw for invalid amount")
        void shouldThrowForInvalidAmount() {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(BigDecimal.ZERO)
                    .currency("MAD")
                    .build();

            assertThatThrownBy(() -> cmiGateway.charge(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount must be positive");
        }
    }
}
