package com.souk.payment.gateway.impl;

import com.souk.payment.gateway.config.GatewayProperties;
import com.souk.payment.gateway.impl.StripeGateway;
import com.souk.payment.gateway.model.GatewayRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeGateway")
class StripeGatewayTest {

    private GatewayProperties properties;
    private StripeGateway stripeGateway;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        GatewayProperties.StripeProperties stripeProps = new GatewayProperties.StripeProperties();
        stripeProps.setEnabled(true);
        stripeProps.setSecretKey("sk_test_xxx");
        stripeProps.setWebhookSecret("whsec_xxx");
        properties.setStripe(stripeProps);

        stripeGateway = new StripeGateway(properties);
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("should return correct gateway ID")
        void shouldReturnGatewayId() {
            assertThat(stripeGateway.getGatewayId()).isEqualTo("STRIPE");
        }

        @Test
        @DisplayName("should return display name")
        void shouldReturnDisplayName() {
            assertThat(stripeGateway.getDisplayName()).isEqualTo("Stripe");
        }

        @Test
        @DisplayName("should be enabled when properties are set")
        void shouldBeEnabledWhenPropertiesSet() {
            assertThat(stripeGateway.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should be disabled when enabled flag is false")
        void shouldBeDisabledWhenFlagFalse() {
            properties.getStripe().setEnabled(false);
            stripeGateway = new StripeGateway(properties);

            assertThat(stripeGateway.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should be disabled when stripe properties are null")
        void shouldBeDisabledWhenPropertiesNull() {
            properties.setStripe(null);
            stripeGateway = new StripeGateway(properties);

            assertThat(stripeGateway.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Payment Method Support")
    class PaymentMethodSupport {

        @Test
        @DisplayName("should support CARD payment method")
        void shouldSupportCard() {
            assertThat(stripeGateway.supports("CARD")).isTrue();
        }

        @Test
        @DisplayName("should support CREDIT_CARD payment method")
        void shouldSupportCreditCard() {
            assertThat(stripeGateway.supports("CREDIT_CARD")).isTrue();
        }

        @Test
        @DisplayName("should support DEBIT_CARD payment method")
        void shouldSupportDebitCard() {
            assertThat(stripeGateway.supports("DEBIT_CARD")).isTrue();
        }

        @Test
        @DisplayName("should be case-insensitive for payment methods")
        void shouldBeCaseInsensitive() {
            assertThat(stripeGateway.supports("card")).isTrue();
            assertThat(stripeGateway.supports("Card")).isTrue();
        }

        @Test
        @DisplayName("should not support unsupported payment methods")
        void shouldNotSupportUnsupportedMethods() {
            assertThat(stripeGateway.supports("CRYPTO")).isFalse();
            assertThat(stripeGateway.supports("BANK_TRANSFER")).isFalse();
            assertThat(stripeGateway.supports("COD")).isFalse();
        }
    }

    @Nested
    @DisplayName("Currency Support")
    class CurrencySupport {

        @Test
        @DisplayName("should support major currencies")
        void shouldSupportMajorCurrencies() {
            assertThat(stripeGateway.supportsCurrency("USD")).isTrue();
            assertThat(stripeGateway.supportsCurrency("EUR")).isTrue();
            assertThat(stripeGateway.supportsCurrency("GBP")).isTrue();
            assertThat(stripeGateway.supportsCurrency("MAD")).isTrue();
        }

        @Test
        @DisplayName("should be case-insensitive for currencies")
        void shouldBeCaseInsensitive() {
            assertThat(stripeGateway.supportsCurrency("usd")).isTrue();
            assertThat(stripeGateway.supportsCurrency("Eur")).isTrue();
        }

        @Test
        @DisplayName("should not support unsupported currencies")
        void shouldNotSupportUnsupportedCurrencies() {
            assertThat(stripeGateway.supportsCurrency("XYZ")).isFalse();
            assertThat(stripeGateway.supportsCurrency("ABC")).isFalse();
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidation {

        @Test
        @DisplayName("should throw for null request")
        void shouldThrowForNullRequest() {
            assertThatThrownBy(() -> stripeGateway.charge(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Gateway request cannot be null");
        }

        @Test
        @DisplayName("should throw for null amount")
        void shouldThrowForNullAmount() {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .currency("MAD")
                    .build();

            assertThatThrownBy(() -> stripeGateway.charge(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount must be positive");
        }

        @Test
        @DisplayName("should throw for zero amount")
        void shouldThrowForZeroAmount() {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(BigDecimal.ZERO)
                    .currency("MAD")
                    .build();

            assertThatThrownBy(() -> stripeGateway.charge(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount must be positive");
        }

        @Test
        @DisplayName("should throw for negative amount")
        void shouldThrowForNegativeAmount() {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("-10.00"))
                    .currency("MAD")
                    .build();

            assertThatThrownBy(() -> stripeGateway.charge(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount must be positive");
        }

        @Test
        @DisplayName("should throw for null currency")
        void shouldThrowForNullCurrency() {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("100.00"))
                    .build();

            assertThatThrownBy(() -> stripeGateway.charge(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Currency is required");
        }

        @Test
        @DisplayName("should throw for blank currency")
        void shouldThrowForBlankCurrency() {
            GatewayRequest request = GatewayRequest.builder()
                    .paymentId(1L)
                    .amount(new BigDecimal("100.00"))
                    .currency("   ")
                    .build();

            assertThatThrownBy(() -> stripeGateway.charge(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Currency is required");
        }
    }

    @Nested
    @DisplayName("Currency Conversion")
    class CurrencyConversion {

        @Test
        @DisplayName("should convert standard currency to minor units")
        void shouldConvertStandardCurrency() {
            // USD: $100.00 should be 10000 cents
            long result = stripeGateway.toMinorUnits(new BigDecimal("100.00"), "USD");
            assertThat(result).isEqualTo(10000L);
        }

        @Test
        @DisplayName("should convert zero-decimal currency correctly")
        void shouldConvertZeroDecimalCurrency() {
            // JPY: 1000 JPY should stay as 1000 (no decimal places)
            long result = stripeGateway.toMinorUnits(new BigDecimal("1000"), "JPY");
            assertThat(result).isEqualTo(1000L);
        }

        @Test
        @DisplayName("should convert three-decimal currency correctly")
        void shouldConvertThreeDecimalCurrency() {
            // KWD: 10.000 KWD should be 10000 (3 decimal places)
            long result = stripeGateway.toMinorUnits(new BigDecimal("10.000"), "KWD");
            assertThat(result).isEqualTo(10000L);
        }

        @Test
        @DisplayName("should convert from minor units for standard currency")
        void shouldConvertFromMinorUnitsStandard() {
            // 10000 cents = $100.00
            BigDecimal result = stripeGateway.fromMinorUnits(10000L, "USD");
            assertThat(result).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("should convert from minor units for zero-decimal currency")
        void shouldConvertFromMinorUnitsZeroDecimal() {
            // 1000 JPY stays 1000
            BigDecimal result = stripeGateway.fromMinorUnits(1000L, "JPY");
            assertThat(result).isEqualByComparingTo("1000");
        }
    }

    @Nested
    @DisplayName("Decimal Places")
    class DecimalPlaces {

        @Test
        @DisplayName("should return 2 for standard currencies")
        void shouldReturn2ForStandardCurrencies() {
            assertThat(stripeGateway.getDecimalPlaces("USD")).isEqualTo(2);
            assertThat(stripeGateway.getDecimalPlaces("EUR")).isEqualTo(2);
            assertThat(stripeGateway.getDecimalPlaces("GBP")).isEqualTo(2);
            assertThat(stripeGateway.getDecimalPlaces("MAD")).isEqualTo(2);
        }

        @Test
        @DisplayName("should return 0 for zero-decimal currencies")
        void shouldReturn0ForZeroDecimalCurrencies() {
            assertThat(stripeGateway.getDecimalPlaces("JPY")).isEqualTo(0);
            assertThat(stripeGateway.getDecimalPlaces("KRW")).isEqualTo(0);
            assertThat(stripeGateway.getDecimalPlaces("VND")).isEqualTo(0);
        }

        @Test
        @DisplayName("should return 3 for three-decimal currencies")
        void shouldReturn3ForThreeDecimalCurrencies() {
            assertThat(stripeGateway.getDecimalPlaces("BHD")).isEqualTo(3);
            assertThat(stripeGateway.getDecimalPlaces("JOD")).isEqualTo(3);
            assertThat(stripeGateway.getDecimalPlaces("KWD")).isEqualTo(3);
            assertThat(stripeGateway.getDecimalPlaces("OMR")).isEqualTo(3);
            assertThat(stripeGateway.getDecimalPlaces("TND")).isEqualTo(3);
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(stripeGateway.getDecimalPlaces("jpy")).isEqualTo(0);
            assertThat(stripeGateway.getDecimalPlaces("usd")).isEqualTo(2);
        }
    }
}
