package com.souk.payment.api.dto;

import com.souk.common.domain.Customer;
import com.souk.common.domain.Order;
import com.souk.common.domain.Payment;
import com.souk.common.domain.Vendor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentCreateRequest(
        // --- Required fields ---
        @NotNull @Positive BigDecimal amount,
        @NotNull Payment.PaymentMethod paymentMethod,

        // --- Relationships ---
        Long orderId,
        Long customerId,
        Long vendorId,

        // --- Payment Type ---
        Payment.PaymentType paymentType,

        // --- Currency ---
        String currency,

        // --- Amount breakdown ---
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal deliveryFee,
        BigDecimal serviceFee,

        // --- Platform/Vendor split ---
        BigDecimal platformFee,
        BigDecimal platformFeePercent,
        BigDecimal vendorAmount,

        // --- Gateway ---
        String paymentGateway,

        // --- Payer info ---
        String payerEmail,
        String payerPhone,
        String payerName,

        // --- Billing address ---
        String billingAddressLine1,
        String billingAddressLine2,
        String billingCity,
        String billingState,
        String billingPostalCode,
        String billingCountry,

        // --- Device/Session info (for fraud detection) ---
        String ipAddress,
        String userAgent,
        String deviceFingerprint,
        String sessionId,

        // --- References ---
        String idempotencyKey,
        String externalReference,
        String invoiceNumber,

        // --- Metadata ---
        String description,
        String metadata
) {
    public Payment toDomain(Order order, Customer customer, Vendor vendor) {
        Payment payment = new Payment();

        // Relationships
        payment.setOrder(order);
        payment.setCustomer(customer);
        payment.setVendor(vendor);

        // Payment type
        payment.setPaymentType(paymentType != null ? paymentType : Payment.PaymentType.PURCHASE);

        // Currency & amounts
        payment.setCurrency(currency != null ? currency : "MAD");
        payment.setAmount(amount);
        payment.setOriginalAmount(originalAmount);
        payment.setDiscountAmount(discountAmount);
        payment.setTaxAmount(taxAmount);
        payment.setTipAmount(tipAmount);
        payment.setDeliveryFee(deliveryFee);
        payment.setServiceFee(serviceFee);

        // Platform/Vendor split
        payment.setPlatformFee(platformFee);
        payment.setPlatformFeePercent(platformFeePercent);
        payment.setVendorAmount(vendorAmount);

        // Payment method & gateway
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentGateway(paymentGateway);

        // Payer info
        payment.setPayerEmail(payerEmail);
        payment.setPayerPhone(payerPhone);
        payment.setPayerName(payerName);

        // Billing address
        payment.setBillingAddressLine1(billingAddressLine1);
        payment.setBillingAddressLine2(billingAddressLine2);
        payment.setBillingCity(billingCity);
        payment.setBillingState(billingState);
        payment.setBillingPostalCode(billingPostalCode);
        payment.setBillingCountry(billingCountry);

        // Device/Session info
        payment.setIpAddress(ipAddress);
        payment.setUserAgent(userAgent);
        payment.setDeviceFingerprint(deviceFingerprint);
        payment.setSessionId(sessionId);

        // References
        payment.setIdempotencyKey(idempotencyKey);
        payment.setExternalReference(externalReference);
        payment.setInvoiceNumber(invoiceNumber);

        // Metadata
        payment.setDescription(description);
        payment.setMetadata(metadata);

        // Initial status
        payment.setStatus(Payment.PaymentStatus.PENDING);

        return payment;
    }
}
