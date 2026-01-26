package com.souk.common.adapters.jpa;

import com.souk.common.domain.Payment;
import com.souk.common.adapters.jpa.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentJpaAdapter extends JpaDataAccessAdapter<Payment, Long> {

    private final PaymentRepository paymentRepository;

    public PaymentJpaAdapter(PaymentRepository repo) {
        super(repo);
        this.paymentRepository = repo;
    }

    // --- Idempotency ---
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey);
    }

    // --- By Order ---
    public List<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrder_Id(orderId);
    }

    // --- By Customer ---
    public List<Payment> findByCustomerId(Long customerId) {
        return paymentRepository.findByCustomer_Id(customerId);
    }

    // --- By Vendor ---
    public List<Payment> findByVendorId(Long vendorId) {
        return paymentRepository.findByVendor_VendorId(vendorId);
    }

    // --- By Status ---
    public List<Payment> findByStatus(Payment.PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    // --- By Vendor Payout Status ---
    public List<Payment> findByVendorPayoutStatus(Payment.PayoutStatus payoutStatus) {
        return paymentRepository.findByVendorPayoutStatus(payoutStatus);
    }

    // --- By Payment Gateway ---
    public List<Payment> findByPaymentGateway(String paymentGateway) {
        return paymentRepository.findByPaymentGateway(paymentGateway);
    }

    // --- By Gateway Payment ID ---
    public Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId) {
        return paymentRepository.findByGatewayPaymentId(gatewayPaymentId);
    }

    // --- By External Reference ---
    public Optional<Payment> findByExternalReference(String externalReference) {
        return paymentRepository.findByExternalReference(externalReference);
    }

    // --- By Parent Payment (for refunds) ---
    public List<Payment> findByParentPaymentId(Long parentPaymentId) {
        return paymentRepository.findByParentPayment_Id(parentPaymentId);
    }

    // --- Payments pending payout for a vendor ---
    public List<Payment> findPendingPayoutsForVendor(Long vendorId) {
        return paymentRepository.findPendingPayoutsForVendor(vendorId);
    }

    // --- Payments requiring retry ---
    public List<Payment> findPaymentsForRetry(int maxAttempts, LocalDateTime now) {
        return paymentRepository.findPaymentsForRetry(maxAttempts, now);
    }

    // --- Payments by date range ---
    public List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.findByCreatedAtBetween(startDate, endDate);
    }

    // --- Payments by customer and status ---
    public List<Payment> findByCustomerIdAndStatus(Long customerId, Payment.PaymentStatus status) {
        return paymentRepository.findByCustomer_IdAndStatus(customerId, status);
    }

    // --- Payments by vendor and status ---
    public List<Payment> findByVendorIdAndStatus(Long vendorId, Payment.PaymentStatus status) {
        return paymentRepository.findByVendor_VendorIdAndStatus(vendorId, status);
    }
}
