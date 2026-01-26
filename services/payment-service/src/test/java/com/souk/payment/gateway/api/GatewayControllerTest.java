package com.souk.payment.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.payment.gateway.GatewayService;
import com.souk.payment.gateway.PaymentGateway;
import com.souk.payment.gateway.IPaymentGatewayService;
import com.souk.payment.gateway.api.dto.CaptureRequest;
import com.souk.payment.gateway.api.dto.ChargeRequest;
import com.souk.payment.gateway.api.dto.RefundRequest;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for GatewayController using standalone MockMvc setup.
 * Uses manual mock creation to avoid bytecode instrumentation issues.
 */
@DisplayName("GatewayController")
class GatewayControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private GatewayService gatewayService;
    private IPaymentGatewayService paymentGatewayService;
    private PaymentGateway mockStripeGateway;
    private GatewayController gatewayController;

    @BeforeEach
    void setUp() {
        // Manual mock creation avoids MockitoExtension bytecode issues
        gatewayService = mock(GatewayService.class);
        paymentGatewayService = mock(IPaymentGatewayService.class);
        mockStripeGateway = mock(PaymentGateway.class);
        
        objectMapper = new ObjectMapper();
        gatewayController = new GatewayController(gatewayService, paymentGatewayService);
        mockMvc = MockMvcBuilders.standaloneSetup(gatewayController).build();

        lenient().when(mockStripeGateway.getGatewayId()).thenReturn("STRIPE");
        lenient().when(mockStripeGateway.getDisplayName()).thenReturn("Stripe");
        lenient().when(mockStripeGateway.isEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("GET /api/v1/gateway/available")
    class GetAvailableGateways {

        @Test
        @DisplayName("should return list of enabled gateways")
        void shouldReturnEnabledGateways() throws Exception {
            when(gatewayService.getEnabledGateways()).thenReturn(List.of(mockStripeGateway));

            mockMvc.perform(get("/api/v1/gateway/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("STRIPE"))
                    .andExpect(jsonPath("$[0].displayName").value("Stripe"));
        }

        @Test
        @DisplayName("should return empty list when no gateways enabled")
        void shouldReturnEmptyList() throws Exception {
            when(gatewayService.getEnabledGateways()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/gateway/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gateway/{gatewayId}/supports")
    class CheckSupport {

        @Test
        @DisplayName("should return support status")
        void shouldReturnSupportStatus() throws Exception {
            when(gatewayService.getGateway("STRIPE")).thenReturn(mockStripeGateway);
            when(mockStripeGateway.supports("CARD")).thenReturn(true);
            when(mockStripeGateway.supportsCurrency("USD")).thenReturn(true);

            mockMvc.perform(get("/api/v1/gateway/STRIPE/supports")
                            .param("paymentMethod", "CARD")
                            .param("currency", "USD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.supportsMethod").value(true))
                    .andExpect(jsonPath("$.supportsCurrency").value(true))
                    .andExpect(jsonPath("$.supported").value(true));
        }

        @Test
        @DisplayName("should return false when currency not supported")
        void shouldReturnFalseWhenCurrencyNotSupported() throws Exception {
            when(gatewayService.getGateway("STRIPE")).thenReturn(mockStripeGateway);
            when(mockStripeGateway.supports("CARD")).thenReturn(true);
            when(mockStripeGateway.supportsCurrency("XYZ")).thenReturn(false);

            mockMvc.perform(get("/api/v1/gateway/STRIPE/supports")
                            .param("paymentMethod", "CARD")
                            .param("currency", "XYZ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.supportsMethod").value(true))
                    .andExpect(jsonPath("$.supportsCurrency").value(false))
                    .andExpect(jsonPath("$.supported").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/charge")
    class Charge {

        @Test
        @DisplayName("should process charge successfully")
        void shouldProcessChargeSuccessfully() throws Exception {
            ChargeRequest request = new ChargeRequest(
                    1L, "test-idem-key", new BigDecimal("100.00"), "MAD",
                    "tok_123", null, 100L, "Test payment",
                    "test@example.com", "Test User", "+212600000000",
                    null, null, null, null, null, null,
                    null, null, false
            );

            GatewayResponse response = GatewayResponse.success("STRIPE", "pi_123");

            when(paymentGatewayService.charge(eq("STRIPE"), eq(1L), any(GatewayRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/charge")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.gatewayPaymentId").value("pi_123"));

            ArgumentCaptor<GatewayRequest> captor = ArgumentCaptor.forClass(GatewayRequest.class);
            verify(paymentGatewayService).charge(eq("STRIPE"), eq(1L), captor.capture());
            GatewayRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.getAmount()).isEqualByComparingTo("100.00");
            assertThat(capturedRequest.getCurrency()).isEqualTo("MAD");
        }

        @Test
        @DisplayName("should handle failed charge")
        void shouldHandleFailedCharge() throws Exception {
            ChargeRequest request = new ChargeRequest(
                    1L, "test-idem-key", new BigDecimal("100.00"), "MAD",
                    "tok_123", null, null, null,
                    null, null, null,
                    null, null, null, null, null, null,
                    null, null, false
            );

            GatewayResponse response = GatewayResponse.failure("STRIPE", "card_declined", "Your card was declined");

            when(paymentGatewayService.charge(eq("STRIPE"), eq(1L), any(GatewayRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/charge")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("card_declined"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/authorize")
    class Authorize {

        @Test
        @DisplayName("should authorize payment successfully")
        void shouldAuthorizeSuccessfully() throws Exception {
            ChargeRequest request = new ChargeRequest(
                    1L, "test-idem-key", new BigDecimal("100.00"), "MAD",
                    "tok_123", null, null, null,
                    null, null, null,
                    null, null, null, null, null, null,
                    null, null, false
            );

            GatewayResponse response = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayResponse.GatewayStatus.REQUIRES_CAPTURE)
                    .gatewayName("STRIPE")
                    .gatewayPaymentId("pi_123")
                    .build();

            when(paymentGatewayService.authorize(eq("STRIPE"), eq(1L), any(GatewayRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value("REQUIRES_CAPTURE"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/capture")
    class Capture {

        @Test
        @DisplayName("should capture payment successfully")
        void shouldCaptureSuccessfully() throws Exception {
            CaptureRequest request = new CaptureRequest(1L, "pi_123", new BigDecimal("100.00"));

            GatewayResponse response = GatewayResponse.success("STRIPE", "pi_123");

            when(paymentGatewayService.capture("STRIPE", 1L, "pi_123", new BigDecimal("100.00")))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/capture")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(paymentGatewayService).capture("STRIPE", 1L, "pi_123", new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/cancel")
    class Cancel {

        @Test
        @DisplayName("should cancel payment successfully")
        void shouldCancelSuccessfully() throws Exception {
            GatewayResponse response = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayResponse.GatewayStatus.CANCELLED)
                    .gatewayName("STRIPE")
                    .gatewayPaymentId("pi_123")
                    .build();

            when(paymentGatewayService.cancel("STRIPE", 1L, "pi_123")).thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/cancel")
                            .param("paymentId", "1")
                            .param("gatewayPaymentId", "pi_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/refund")
    class Refund {

        @Test
        @DisplayName("should refund payment successfully")
        void shouldRefundSuccessfully() throws Exception {
            RefundRequest request = new RefundRequest(1L, "pi_123", new BigDecimal("50.00"), "Customer request");

            GatewayResponse response = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayResponse.GatewayStatus.PARTIALLY_REFUNDED)
                    .gatewayName("STRIPE")
                    .gatewayPaymentId("pi_123")
                    .build();

            when(paymentGatewayService.refund("STRIPE", 1L, "pi_123", new BigDecimal("50.00"), "Customer request"))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gateway/{gatewayId}/status/{gatewayPaymentId}")
    class GetStatus {

        @Test
        @DisplayName("should return payment status")
        void shouldReturnPaymentStatus() throws Exception {
            GatewayResponse response = GatewayResponse.success("STRIPE", "pi_123");

            when(gatewayService.getStatus("STRIPE", "pi_123")).thenReturn(response);

            mockMvc.perform(get("/api/v1/gateway/STRIPE/status/pi_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.gatewayPaymentId").value("pi_123"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/webhook")
    class HandleWebhook {

        @Test
        @DisplayName("should process valid webhook")
        void shouldProcessValidWebhook() throws Exception {
            GatewayResponse response = GatewayResponse.success("STRIPE", "pi_123");

            when(paymentGatewayService.handleWebhook("STRIPE", "{\"type\":\"payment_intent.succeeded\"}", "sig_123"))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"payment_intent.succeeded\"}")
                            .header("Stripe-Signature", "sig_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return bad request for invalid signature")
        void shouldReturnBadRequestForInvalidSignature() throws Exception {
            GatewayResponse response = GatewayResponse.failure("STRIPE", "INVALID_SIGNATURE", "Webhook signature verification failed");

            when(paymentGatewayService.handleWebhook("STRIPE", "{}", "bad_sig"))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/gateway/STRIPE/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header("Stripe-Signature", "bad_sig"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_SIGNATURE"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/customer")
    class CreateCustomer {

        @Test
        @DisplayName("should create customer successfully")
        void shouldCreateCustomerSuccessfully() throws Exception {
            when(gatewayService.createCustomer("STRIPE", "test@example.com", "Test User"))
                    .thenReturn("cus_123");

            mockMvc.perform(post("/api/v1/gateway/STRIPE/customer")
                            .param("email", "test@example.com")
                            .param("name", "Test User"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gatewayCustomerId").value("cus_123"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gateway/{gatewayId}/customer/{customerId}/payment-method")
    class SavePaymentMethod {

        @Test
        @DisplayName("should save payment method successfully")
        void shouldSavePaymentMethodSuccessfully() throws Exception {
            when(gatewayService.savePaymentMethod("STRIPE", "cus_123", "tok_123"))
                    .thenReturn("pm_123");

            mockMvc.perform(post("/api/v1/gateway/STRIPE/customer/cus_123/payment-method")
                            .param("paymentToken", "tok_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentMethodId").value("pm_123"));
        }
    }
}
