package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // --- Idempotency ---
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // --- By Order (using underscore for relationship traversal) ---
    List<Payment> findByOrder_Id(Long orderId);

    // --- By Customer (using underscore for relationship traversal) ---
    List<Payment> findByCustomer_Id(Long customerId);

    // --- By Vendor (Vendor uses vendorId, not id) ---
    List<Payment> findByVendor_VendorId(Long vendorId);

    // --- By Status ---
    List<Payment> findByStatus(Payment.PaymentStatus status);

    // --- By Vendor Payout Status ---
    List<Payment> findByVendorPayoutStatus(Payment.PayoutStatus payoutStatus);

    // --- By Payment Gateway ---
    List<Payment> findByPaymentGateway(String paymentGateway);

    // --- By Gateway Payment ID ---
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    // --- By External Reference ---
    Optional<Payment> findByExternalReference(String externalReference);

    // --- By Parent Payment (using underscore for relationship traversal) ---
    List<Payment> findByParentPayment_Id(Long parentPaymentId);

    // --- Payments pending payout for a vendor (Vendor uses vendorId) ---
    @Query("SELECT p FROM Payment p WHERE p.vendor.vendorId = :vendorId AND p.status = 'COMPLETED' AND (p.vendorPayoutStatus IS NULL OR p.vendorPayoutStatus = 'PENDING')")
    List<Payment> findPendingPayoutsForVendor(@Param("vendorId") Long vendorId);

    // --- Payments requiring retry ---
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.attemptCount < :maxAttempts AND (p.nextRetryAt IS NULL OR p.nextRetryAt <= :now)")
    List<Payment> findPaymentsForRetry(@Param("maxAttempts") int maxAttempts, @Param("now") LocalDateTime now);

    // --- Payments by date range ---
    List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // --- Payments by customer and status ---
    List<Payment> findByCustomer_IdAndStatus(Long customerId, Payment.PaymentStatus status);

    // --- Payments by vendor and status ---
    List<Payment> findByVendor_VendorIdAndStatus(Long vendorId, Payment.PaymentStatus status);
}
