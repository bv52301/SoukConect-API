package com.souk.payment.gateway;

import com.souk.payment.gateway.config.GatewayProperties;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayService")
class GatewayServiceTest {

    @Mock
    private PaymentGateway stripeGateway;

    @Mock
    private PaymentGateway cmiGateway;

    private GatewayProperties properties;
    private GatewayService gatewayService;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        properties.setDefaultGateway("STRIPE");

        when(stripeGateway.getGatewayId()).thenReturn("STRIPE");
        when(cmiGateway.getGatewayId()).thenReturn("CMI");

        List<PaymentGateway> gateways = Arrays.asList(stripeGateway, cmiGateway);
        gatewayService = new GatewayService(gateways, properties);
    }

    @Nested
    @DisplayName("getGateway()")
    class GetGateway {

        @Test
        @DisplayName("should return gateway by ID (case insensitive)")
        void shouldReturnGatewayById() {
            when(stripeGateway.isEnabled()).thenReturn(true);

            PaymentGateway result = gatewayService.getGateway("stripe");

            assertThat(result).isEqualTo(stripeGateway);
        }

        @Test
        @DisplayName("should throw for unknown gateway ID")
        void shouldThrowForUnknownGateway() {
            assertThatThrownBy(() -> gatewayService.getGateway("UNKNOWN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown gateway: UNKNOWN");
        }

        @Test
        @DisplayName("should throw for disabled gateway")
        void shouldThrowForDisabledGateway() {
            when(stripeGateway.isEnabled()).thenReturn(false);

            assertThatThrownBy(() -> gatewayService.getGateway("STRIPE"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Gateway not enabled: STRIPE");
        }
    }

    @Nested
    @DisplayName("getDefaultGateway()")
    class GetDefaultGateway {

        @Test
        @DisplayName("should return configured default gateway")
        void shouldReturnDefaultGateway() {
            when(stripeGateway.isEnabled()).thenReturn(true);

            PaymentGateway result = gatewayService.getDefaultGateway();

            assertThat(result).isEqualTo(stripeGateway);
        }
    }

    @Nested
    @DisplayName("findGateway()")
    class FindGateway {

        @Test
        @DisplayName("should find gateway supporting payment method and currency")
        void shouldFindSupportingGateway() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            when(stripeGateway.supports("CARD")).thenReturn(true);
            when(stripeGateway.supportsCurrency("USD")).thenReturn(true);

            Optional<PaymentGateway> result = gatewayService.findGateway("CARD", "USD");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(stripeGateway);
        }

        @Test
        @DisplayName("should return empty when no gateway supports payment method")
        void shouldReturnEmptyWhenNoSupport() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            when(stripeGateway.supports("CRYPTO")).thenReturn(false);
            when(cmiGateway.isEnabled()).thenReturn(true);
            when(cmiGateway.supports("CRYPTO")).thenReturn(false);

            Optional<PaymentGateway> result = gatewayService.findGateway("CRYPTO", "USD");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip disabled gateways")
        void shouldSkipDisabledGateways() {
            when(stripeGateway.isEnabled()).thenReturn(false);
            when(cmiGateway.isEnabled()).thenReturn(true);
            when(cmiGateway.supports("CARD")).thenReturn(true);
            when(cmiGateway.supportsCurrency("MAD")).thenReturn(true);

            Optional<PaymentGateway> result = gatewayService.findGateway("CARD", "MAD");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(cmiGateway);
        }
    }

    @Nested
    @DisplayName("getEnabledGateways()")
    class GetEnabledGateways {

        @Test
        @DisplayName("should return only enabled gateways")
        void shouldReturnOnlyEnabledGateways() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            when(cmiGateway.isEnabled()).thenReturn(false);

            List<PaymentGateway> result = gatewayService.getEnabledGateways();

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(stripeGateway);
        }

        @Test
        @DisplayName("should return empty list when all gateways disabled")
        void shouldReturnEmptyWhenAllDisabled() {
            when(stripeGateway.isEnabled()).thenReturn(false);
            when(cmiGateway.isEnabled()).thenReturn(false);

            List<PaymentGateway> result = gatewayService.getEnabledGateways();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isGatewayAvailable()")
    class IsGatewayAvailable {

        @Test
        @DisplayName("should return true for enabled gateway")
        void shouldReturnTrueForEnabledGateway() {
            when(stripeGateway.isEnabled()).thenReturn(true);

            boolean result = gatewayService.isGatewayAvailable("STRIPE");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for disabled gateway")
        void shouldReturnFalseForDisabledGateway() {
            when(stripeGateway.isEnabled()).thenReturn(false);

            boolean result = gatewayService.isGatewayAvailable("STRIPE");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for unknown gateway")
        void shouldReturnFalseForUnknownGateway() {
            boolean result = gatewayService.isGatewayAvailable("UNKNOWN");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("charge()")
    class Charge {

        private GatewayRequest testRequest;

        @BeforeEach
        void setUp() {
            testRequest = GatewayRequest.builder()
                    .paymentId(1L)
                    .idempotencyKey("test-key")
                    .amount(new BigDecimal("100.00"))
                    .currency("MAD")
                    .build();
        }

        @Test
        @DisplayName("should delegate charge to specified gateway")
        void shouldDelegateToSpecifiedGateway() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayResponse expectedResponse = GatewayResponse.success("STRIPE", "pi_123");
            when(stripeGateway.charge(testRequest)).thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.charge("STRIPE", testRequest);

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).charge(testRequest);
        }

        @Test
        @DisplayName("should use default gateway when gateway ID not specified")
        void shouldUseDefaultGateway() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayResponse expectedResponse = GatewayResponse.success("STRIPE", "pi_123");
            when(stripeGateway.charge(testRequest)).thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.charge(testRequest);

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).charge(testRequest);
        }
    }

    @Nested
    @DisplayName("authorize()")
    class Authorize {

        @Test
        @DisplayName("should delegate authorize to gateway")
        void shouldDelegateAuthorize() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("100.00"))
                    .currency("MAD")
                    .build();
            GatewayResponse expectedResponse = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayResponse.GatewayStatus.REQUIRES_CAPTURE)
                    .build();
            when(stripeGateway.authorize(request)).thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.authorize("STRIPE", request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).authorize(request);
        }
    }

    @Nested
    @DisplayName("capture()")
    class Capture {

        @Test
        @DisplayName("should delegate capture to gateway")
        void shouldDelegateCapture() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayResponse expectedResponse = GatewayResponse.success("STRIPE", "pi_123");
            when(stripeGateway.capture("pi_123", new BigDecimal("100.00"))).thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.capture("STRIPE", "pi_123", new BigDecimal("100.00"));

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).capture("pi_123", new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("should delegate cancel to gateway")
        void shouldDelegateCancel() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayResponse expectedResponse = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayResponse.GatewayStatus.CANCELLED)
                    .build();
            when(stripeGateway.cancel("pi_123")).thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.cancel("STRIPE", "pi_123");

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).cancel("pi_123");
        }
    }

    @Nested
    @DisplayName("refund()")
    class Refund {

        @Test
        @DisplayName("should delegate refund to gateway")
        void shouldDelegateRefund() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayResponse expectedResponse = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayResponse.GatewayStatus.REFUNDED)
                    .build();
            when(stripeGateway.refund("pi_123", new BigDecimal("50.00"), "Customer request"))
                    .thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.refund("STRIPE", "pi_123",
                    new BigDecimal("50.00"), "Customer request");

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).refund("pi_123", new BigDecimal("50.00"), "Customer request");
        }
    }

    @Nested
    @DisplayName("getStatus()")
    class GetStatus {

        @Test
        @DisplayName("should delegate getStatus to gateway")
        void shouldDelegateGetStatus() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayResponse expectedResponse = GatewayResponse.success("STRIPE", "pi_123");
            when(stripeGateway.getStatus("pi_123")).thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.getStatus("STRIPE", "pi_123");

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).getStatus("pi_123");
        }
    }

    @Nested
    @DisplayName("verifyWebhook()")
    class VerifyWebhook {

        @Test
        @DisplayName("should delegate webhook verification to gateway")
        void shouldDelegateWebhookVerification() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            when(stripeGateway.verifyWebhook("payload", "signature")).thenReturn(true);

            boolean result = gatewayService.verifyWebhook("STRIPE", "payload", "signature");

            assertThat(result).isTrue();
            verify(stripeGateway).verifyWebhook("payload", "signature");
        }
    }

    @Nested
    @DisplayName("parseWebhook()")
    class ParseWebhook {

        @Test
        @DisplayName("should delegate webhook parsing to gateway")
        void shouldDelegateWebhookParsing() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            GatewayResponse expectedResponse = GatewayResponse.success("STRIPE", "pi_123");
            when(stripeGateway.parseWebhook("payload")).thenReturn(expectedResponse);

            GatewayResponse result = gatewayService.parseWebhook("STRIPE", "payload");

            assertThat(result).isEqualTo(expectedResponse);
            verify(stripeGateway).parseWebhook("payload");
        }
    }

    @Nested
    @DisplayName("createCustomer()")
    class CreateCustomer {

        @Test
        @DisplayName("should delegate customer creation to gateway")
        void shouldDelegateCustomerCreation() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            when(stripeGateway.createCustomer("test@example.com", "Test User")).thenReturn("cus_123");

            String result = gatewayService.createCustomer("STRIPE", "test@example.com", "Test User");

            assertThat(result).isEqualTo("cus_123");
            verify(stripeGateway).createCustomer("test@example.com", "Test User");
        }
    }

    @Nested
    @DisplayName("savePaymentMethod()")
    class SavePaymentMethod {

        @Test
        @DisplayName("should delegate payment method saving to gateway")
        void shouldDelegatePaymentMethodSaving() {
            when(stripeGateway.isEnabled()).thenReturn(true);
            when(stripeGateway.savePaymentMethod("cus_123", "pm_token")).thenReturn("pm_123");

            String result = gatewayService.savePaymentMethod("STRIPE", "cus_123", "pm_token");

            assertThat(result).isEqualTo("pm_123");
            verify(stripeGateway).savePaymentMethod("cus_123", "pm_token");
        }
    }
}
