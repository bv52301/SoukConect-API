package com.souk.payment.gateway;

import com.souk.common.adapters.jpa.PaymentJpaAdapter;
import com.souk.common.domain.Payment;
import com.souk.payment.gateway.model.GatewayRequest;
import com.souk.payment.gateway.model.GatewayResponse;
import com.souk.payment.gateway.model.GatewayResponse.GatewayStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentGatewayService")
class PaymentGatewayServiceTest {

    @Mock
    private GatewayService gatewayService;

    @Mock
    private PaymentJpaAdapter paymentAdapter;

    @InjectMocks
    private PaymentGatewayService paymentGatewayService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private Payment testPayment;
    private GatewayRequest testRequest;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setAmount(new BigDecimal("100.00"));
        testPayment.setCurrency("MAD");
        testPayment.setStatus(Payment.PaymentStatus.PENDING);
        testPayment.setAttemptCount(0);

        testRequest = GatewayRequest.builder()
                .paymentId(1L)
                .idempotencyKey("test-idempotency-key")
                .amount(new BigDecimal("100.00"))
                .currency("MAD")
                .paymentToken("tok_test")
                .customerEmail("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("charge()")
    class Charge {

        @Test
        @DisplayName("should update payment on successful charge")
        void shouldUpdatePaymentOnSuccess() {
            // Arrange
            GatewayResponse successResponse = GatewayResponse.success("STRIPE", "pi_123");
            successResponse.setCardBrand("visa");
            successResponse.setCardLastFour("4242");

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.charge("STRIPE", testRequest)).thenReturn(successResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.charge("STRIPE", 1L, testRequest);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getGatewayPaymentId()).isEqualTo("pi_123");

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
            assertThat(savedPayment.getGatewayPaymentId()).isEqualTo("pi_123");
            assertThat(savedPayment.getPaymentGateway()).isEqualTo("STRIPE");
            assertThat(savedPayment.getCardBrand()).isEqualTo("visa");
            assertThat(savedPayment.getCardLastFour()).isEqualTo("4242");
        }

        @Test
        @DisplayName("should update payment on failed charge")
        void shouldUpdatePaymentOnFailure() {
            // Arrange
            GatewayResponse failureResponse = GatewayResponse.failure("STRIPE", "card_declined", "Your card was declined");

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.charge("STRIPE", testRequest)).thenReturn(failureResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.charge("STRIPE", 1L, testRequest);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("card_declined");

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
            assertThat(savedPayment.getGatewayResponseCode()).isEqualTo("card_declined");
            assertThat(savedPayment.getGatewayResponseMessage()).isEqualTo("Your card was declined");
            assertThat(savedPayment.getFailureReason()).isEqualTo("Your card was declined");
            assertThat(savedPayment.getAttemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle 3DS requires action response")
        void shouldHandle3DSRequiresAction() {
            // Arrange
            GatewayResponse requiresActionResponse = GatewayResponse.requiresAction("STRIPE", "pi_123", "https://stripe.com/3ds");

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.charge("STRIPE", testRequest)).thenReturn(requiresActionResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.charge("STRIPE", 1L, testRequest);

            // Assert
            assertThat(result.requiresCustomerAction()).isTrue();
            assertThat(result.getAuthUrl()).isEqualTo("https://stripe.com/3ds");

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.REQUIRES_ACTION);
            assertThat(savedPayment.getAuthStatus()).isEqualTo(Payment.AuthenticationStatus.PENDING);
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            // Arrange
            when(paymentAdapter.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentGatewayService.charge("STRIPE", 999L, testRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Payment not found: 999");

            verify(gatewayService, never()).charge(any(), any());
        }
    }

    @Nested
    @DisplayName("authorize()")
    class Authorize {

        @Test
        @DisplayName("should update payment status to AUTHORIZED on success")
        void shouldUpdatePaymentToAuthorized() {
            // Arrange
            GatewayResponse authorizeResponse = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayStatus.REQUIRES_CAPTURE)
                    .gatewayName("STRIPE")
                    .gatewayPaymentId("pi_123")
                    .build();

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.authorize("STRIPE", testRequest)).thenReturn(authorizeResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.authorize("STRIPE", 1L, testRequest);

            // Assert
            assertThat(result.requiresCapture()).isTrue();

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.AUTHORIZED);
        }
    }

    @Nested
    @DisplayName("capture()")
    class Capture {

        @Test
        @DisplayName("should capture authorized payment successfully")
        void shouldCaptureSuccessfully() {
            // Arrange
            testPayment.setStatus(Payment.PaymentStatus.AUTHORIZED);
            testPayment.setGatewayPaymentId("pi_123");

            GatewayResponse captureResponse = GatewayResponse.success("STRIPE", "pi_123");

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.capture("STRIPE", "pi_123", new BigDecimal("100.00"))).thenReturn(captureResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.capture("STRIPE", 1L, "pi_123", new BigDecimal("100.00"));

            // Assert
            assertThat(result.isSuccess()).isTrue();

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("should cancel authorized payment successfully")
        void shouldCancelSuccessfully() {
            // Arrange
            testPayment.setStatus(Payment.PaymentStatus.AUTHORIZED);
            testPayment.setGatewayPaymentId("pi_123");

            GatewayResponse cancelResponse = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayStatus.CANCELLED)
                    .gatewayName("STRIPE")
                    .gatewayPaymentId("pi_123")
                    .build();

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.cancel("STRIPE", "pi_123")).thenReturn(cancelResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.cancel("STRIPE", 1L, "pi_123");

            // Assert
            assertThat(result.isSuccess()).isTrue();

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
            assertThat(savedPayment.getStatusReason()).isEqualTo("Cancelled via gateway");
        }
    }

    @Nested
    @DisplayName("refund()")
    class Refund {

        @Test
        @DisplayName("should process full refund successfully")
        void shouldProcessFullRefund() {
            // Arrange
            testPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            testPayment.setGatewayPaymentId("pi_123");
            testPayment.setAmount(new BigDecimal("100.00"));
            testPayment.setRefundedAmount(BigDecimal.ZERO);

            GatewayResponse refundResponse = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayStatus.REFUNDED)
                    .gatewayName("STRIPE")
                    .gatewayPaymentId("pi_123")
                    .build();

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.refund("STRIPE", "pi_123", new BigDecimal("100.00"), "Customer request"))
                    .thenReturn(refundResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.refund("STRIPE", 1L, "pi_123",
                    new BigDecimal("100.00"), "Customer request");

            // Assert
            assertThat(result.isSuccess()).isTrue();

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
            assertThat(savedPayment.getRefundedAmount()).isEqualByComparingTo("100.00");
            assertThat(savedPayment.getRefundReason()).isEqualTo("Customer request");
            assertThat(savedPayment.getRefundDate()).isNotNull();
        }

        @Test
        @DisplayName("should process partial refund successfully")
        void shouldProcessPartialRefund() {
            // Arrange
            testPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            testPayment.setGatewayPaymentId("pi_123");
            testPayment.setAmount(new BigDecimal("100.00"));
            testPayment.setRefundedAmount(BigDecimal.ZERO);

            GatewayResponse refundResponse = GatewayResponse.builder()
                    .success(true)
                    .status(GatewayStatus.PARTIALLY_REFUNDED)
                    .gatewayName("STRIPE")
                    .gatewayPaymentId("pi_123")
                    .build();

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(gatewayService.refund("STRIPE", "pi_123", new BigDecimal("30.00"), "Partial refund"))
                    .thenReturn(refundResponse);
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.refund("STRIPE", 1L, "pi_123",
                    new BigDecimal("30.00"), "Partial refund");

            // Assert
            assertThat(result.isSuccess()).isTrue();

            verify(paymentAdapter).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(savedPayment.getRefundedAmount()).isEqualByComparingTo("30.00");
        }
    }

    @Nested
    @DisplayName("giveup()")
    class Giveup {

        @Test
        @DisplayName("should mark payment as ABANDONED")
        void shouldMarkPaymentAsAbandoned() {
            // Arrange
            testPayment.setStatus(Payment.PaymentStatus.FAILED);
            testPayment.setAttemptCount(3);

            when(paymentAdapter.findById(1L)).thenReturn(Optional.of(testPayment));
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Payment result = paymentGatewayService.giveup(1L, "Max retries exhausted");

            // Assert
            assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.ABANDONED);
            assertThat(result.getStatusReason()).isEqualTo("Max retries exhausted");
            assertThat(result.getNextRetryAt()).isNull();

            verify(paymentAdapter).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("handleWebhook()")
    class HandleWebhook {

        @Test
        @DisplayName("should reject invalid webhook signature")
        void shouldRejectInvalidSignature() {
            // Arrange
            when(gatewayService.verifyWebhook("STRIPE", "payload", "invalid_sig")).thenReturn(false);

            // Act
            GatewayResponse result = paymentGatewayService.handleWebhook("STRIPE", "payload", "invalid_sig");

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_SIGNATURE");

            verify(paymentAdapter, never()).save(any());
        }

        @Test
        @DisplayName("should process valid webhook and update payment")
        void shouldProcessValidWebhook() {
            // Arrange
            testPayment.setGatewayPaymentId("pi_123");

            GatewayResponse webhookResponse = GatewayResponse.success("STRIPE", "pi_123");

            when(gatewayService.verifyWebhook("STRIPE", "payload", "valid_sig")).thenReturn(true);
            when(gatewayService.parseWebhook("STRIPE", "payload")).thenReturn(webhookResponse);
            when(paymentAdapter.findByGatewayPaymentId("pi_123")).thenReturn(Optional.of(testPayment));
            when(paymentAdapter.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            GatewayResponse result = paymentGatewayService.handleWebhook("STRIPE", "payload", "valid_sig");

            // Assert
            assertThat(result.isSuccess()).isTrue();

            verify(paymentAdapter).save(any(Payment.class));
        }
    }
}
