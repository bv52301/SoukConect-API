# SoukConect Notification Architecture

## Overview

This document describes the complete notification architecture across all SoukConect services, including customer notifications, vendor notifications, authentication notifications, failure handling, and retry strategies.

---

## Table of Contents

1. [Architecture Principles](#architecture-principles)
2. [Notification Types](#notification-types)
3. [Service Responsibilities](#service-responsibilities)
4. [Notification Flows](#notification-flows)
5. [Failure Handling](#failure-handling)
6. [Implementation Guide](#implementation-guide)
7. [API Reference](#api-reference)
8. [Future: Notification Service](#future-notification-service)

---

## Architecture Principles

### Core Design Rules

1. **Domain Services Own Notifications**
   - Customer notifications → **Customer Service**
   - Vendor notifications → **Vendor Service**
   - Auth notifications → **Auth Service** (direct)

2. **Callers Use Simple IDs**
   - Services pass only IDs (customerId, vendorId, orderId)
   - Domain services fetch their own data
   - No need to pass contact details

3. **Non-Blocking Operations**
   - All notification endpoints return **202 Accepted** immediately
   - Process notifications asynchronously
   - Callers don't wait for delivery

4. **Internal Retry Logic**
   - Domain services handle retry internally
   - Use Spring @Async + @Retryable
   - Callers don't need retry logic

5. **Graceful Degradation**
   - Notification failures don't block business logic
   - Log errors and continue
   - Accept eventual consistency

---

## Notification Types

### Customer Notifications

| Event | Trigger | Channel | Priority |
|-------|---------|---------|----------|
| Order Confirmed | Order created | Email | High |
| Payment Successful | Payment captured | Email | High |
| Order Shipped | Order dispatched | Email + SMS | High |
| Order Delivered | Delivery confirmed | Email | Normal |
| Order Cancelled | Customer cancels | Email | High |
| Refund Processed | Refund completed | Email | High |
| Payment Failed | Payment declined | Email + SMS | Urgent |

### Vendor Notifications

| Event | Trigger | Channel | Priority |
|-------|---------|---------|----------|
| New Order | Order confirmed | Email + Push | Urgent |
| Order Cancelled | Customer cancels | Email | High |
| Low Inventory | Stock below threshold | Email + Push | Normal |
| Payout Processed | Settlement completed | Email | Normal |
| New Review | Customer review | Email | Low |

### Auth Notifications

| Event | Trigger | Channel | Priority |
|-------|---------|---------|----------|
| Email Verification | Registration | Email | High |
| Password Reset | Reset requested | Email | Urgent |
| MFA Enabled | MFA setup | Email | Normal |
| Welcome Email | Registration complete | Email | Normal |
| Account Locked | Failed login attempts | Email | Urgent |

---

## Service Responsibilities

```
┌────────────────────────────────────────────────────────────┐
│                     NOTIFICATION ROUTING                    │
└────────────────────────────────────────────────────────────┘

Customer Notifications:
  Order Service ────┐
  BPM Worker ───────┼──→ Customer Service ──→ Email Service
  Payment Service ──┘

Vendor Notifications:
  Order Service ────┐
  BPM Worker ───────┼──→ Vendor Service ──→ Email Service
  Inventory Service─┘

Auth Notifications:
  Auth Service ─────────→ Email Service (direct)
```

### Customer Service
**Responsibility**: All customer business notifications
- Order status updates
- Payment confirmations
- Refund notifications
- Delivery updates

**Does NOT handle**: Authentication emails (handled by Auth Service)

### Vendor Service
**Responsibility**: All vendor business notifications
- New orders
- Order cancellations
- Inventory alerts
- Payout notifications

### Auth Service
**Responsibility**: Security and authentication notifications
- Email verification
- Password reset
- MFA setup
- Account security

**Why separate?**
- Avoid circular dependencies
- Security isolation
- Independent from business domain

---

## Notification Flows

### Flow 1: BPM Worker → Customer Notification

**Scenario**: Order workflow notifies customer after payment

```
┌─────────────────────────────────────────────────────────────┐
│ BPM Worker (OrderWorkflow)                                  │
│                                                             │
│  @ActivityMethod                                            │
│  public void notifyOrderConfirmed(Long orderId) {          │
│      Order order = orderService.getOrder(orderId);         │
│                                                             │
│      customerClient.notifyOrderConfirmed(                  │
│          order.getCustomerId(),                            │
│          new OrderConfirmedNotification(orderId, total)    │
│      );                                                     │
│  }                                                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ POST /customers/{id}/notify-order-confirmed
                        │ (with Temporal retry: 10 attempts)
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ Customer Service                                            │
│                                                             │
│  @PostMapping("/{customerId}/notify-order-confirmed")      │
│  public ResponseEntity<Void> notifyOrderConfirmed(...) {   │
│      sendNotificationAsync(customerId, notification);      │
│      return ResponseEntity.accepted().build(); // 202      │
│  }                                                          │
│                                                             │
│  @Async @Retryable(maxAttempts=5, backoff=...)            │
│  private void sendNotificationAsync(...) {                 │
│      Customer customer = getCustomer(customerId);          │
│      emailService.sendOrderConfirmedEmail(...);           │
│  }                                                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ (Async with 5 retries)
                        ▼
                   Email Service
```

**Retry Strategy**:
1. **Temporal Level**: BPM retries calling Customer Service (10 attempts over 20 mins)
2. **Customer Service Level**: Retries email delivery (5 attempts with backoff)

**Timeline**:
- t=0s: BPM calls Customer Service → Gets 202 immediately
- t=0s: Customer Service starts async email sending
- If email fails: Retry at t=10s, t=30s, t=70s, t=150s, t=310s
- BPM workflow continues (doesn't wait)

---

### Flow 2: BPM Worker → Vendor Notification

**Scenario**: Order workflow notifies vendors of new order

```
┌─────────────────────────────────────────────────────────────┐
│ BPM Worker (OrderWorkflow)                                  │
│                                                             │
│  @ActivityMethod(retryOptions = @RetryOptions(...))        │
│  public void notifyVendors(Long orderId) {                 │
│      Order order = orderService.getOrder(orderId);         │
│                                                             │
│      for (Long vendorId : order.getVendorIds()) {         │
│          vendorClient.notifyNewOrder(                      │
│              vendorId,                                      │
│              new NewOrderNotification(orderId, items)      │
│          );                                                 │
│      }                                                      │
│  }                                                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ POST /vendors/{id}/notify-new-order
                        │ (with Temporal retry)
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ Vendor Service                                              │
│                                                             │
│  @PostMapping("/{vendorId}/notify-new-order")              │
│  public ResponseEntity<Void> notifyNewOrder(...) {         │
│      sendNotificationAsync(vendorId, notification);        │
│      return ResponseEntity.accepted().build(); // 202      │
│  }                                                          │
│                                                             │
│  @Async @Retryable(maxAttempts=5, backoff=...)            │
│  private void sendNotificationAsync(...) {                 │
│      Vendor vendor = getVendor(vendorId);                  │
│      emailService.sendNewOrderEmail(...);                  │
│  }                                                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ (Async with 5 retries)
                        ▼
                   Email Service
```

**Same retry strategy as Customer notifications**

---

### Flow 3: Order Service → Customer Notification

**Scenario**: Order status updated, notify customer

```
┌─────────────────────────────────────────────────────────────┐
│ Order Service                                               │
│                                                             │
│  public void updateOrderStatus(Long orderId, Status status) {
│      Order order = orderRepository.findById(orderId);      │
│      order.setStatus(status);                              │
│      orderRepository.save(order);                          │
│                                                             │
│      // Notify customer (best effort - don't block)        │
│      try {                                                  │
│          customerClient.notifyOrderStatusChanged(          │
│              order.getCustomerId(),                        │
│              new OrderStatusNotification(orderId, status)  │
│          );                                                 │
│      } catch (Exception e) {                               │
│          log.error("Failed to notify: {}", e.getMessage());│
│          // Continue - order update succeeded              │
│      }                                                      │
│  }                                                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ POST /customers/{id}/notify-status-changed
                        │ (try once, log if fails)
                        ▼
                Customer Service (same as above)
```

**Retry Strategy**:
1. **Order Service**: Try once, log if fails, don't block business logic
2. **Customer Service**: Retries email delivery (5 attempts with backoff)

**Note**: Even if Order Service call fails, Customer Service will retry email delivery internally if the call succeeds.

---

### Flow 4: Order Service → Vendor Notification

**Scenario**: Customer cancels order, notify vendor

```
┌─────────────────────────────────────────────────────────────┐
│ Order Service                                               │
│                                                             │
│  public void cancelOrder(Long orderId) {                   │
│      Order order = orderRepository.findById(orderId);      │
│      order.setStatus(OrderStatus.CANCELLED);               │
│      orderRepository.save(order);                          │
│                                                             │
│      // Notify vendors (best effort)                       │
│      for (Long vendorId : order.getVendorIds()) {         │
│          try {                                              │
│              vendorClient.notifyOrderCancelled(            │
│                  vendorId,                                  │
│                  new OrderCancelledNotification(orderId)   │
│              );                                             │
│          } catch (Exception e) {                           │
│              log.error("Failed to notify vendor: {}", e);  │
│          }                                                  │
│      }                                                      │
│  }                                                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ POST /vendors/{id}/notify-order-cancelled
                        │ (try once per vendor)
                        ▼
                 Vendor Service (same as above)
```

---

### Flow 5: Auth Service → User Notification

**Scenario**: Password reset requested

```
┌─────────────────────────────────────────────────────────────┐
│ Auth Service                                                │
│                                                             │
│  public void initiatePasswordReset(String email) {         │
│      User user = userRepository.findByEmail(email);        │
│      String token = UUID.randomUUID().toString();          │
│                                                             │
│      // Store token in Redis + DB                          │
│      storeResetToken(user, token);                         │
│                                                             │
│      // Send email directly (no other service)             │
│      emailService.sendPasswordResetEmail(email, token);    │
│  }                                                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ (Direct call)
                        ▼
                Email Service (internal)
```

**Why Direct?**
- Auth service is self-contained
- No dependency on Customer Service
- Security-critical, needs isolation

---

## Failure Handling

### Three-Level Retry Strategy

```
┌────────────────────────────────────────────────────────────┐
│ Level 1: Caller Retry                                      │
│ - BPM Worker: Temporal retry (10 attempts, 20 mins)       │
│ - Other Services: Best effort (1 attempt, log and continue)│
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│ Level 2: Domain Service Async                             │
│ - Returns 202 Accepted immediately                        │
│ - Processes notification asynchronously                   │
│ - Caller doesn't wait                                     │
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│ Level 3: Email/Notification Delivery Retry                │
│ - @Retryable: 5 attempts with exponential backoff        │
│ - Backoff: 10s, 20s, 40s, 80s, 160s                     │
│ - After exhaustion: Log and give up                      │
└────────────────────────────────────────────────────────────┘
```

### Failure Scenarios

#### Scenario 1: Domain Service Down

```
BPM Worker → Customer Service (DOWN) ❌
           ↓
    Temporal retry (10s later)
           ↓
BPM Worker → Customer Service (UP) ✅
           ↓
    Gets 202, workflow continues
```

**Handling**: Temporal automatically retries, eventually succeeds when service recovers

#### Scenario 2: Email Service Down

```
BPM Worker → Customer Service (UP) ✅ → Returns 202
           ↓
    Async processing
           ↓
    Email Service (DOWN) ❌
           ↓
    @Retryable: Retry after 10s
           ↓
    Email Service (UP) ✅ → Email sent
```

**Handling**: Domain service retries email delivery internally

#### Scenario 3: Both Services Down

```
BPM Worker → Customer Service (DOWN) ❌
           ↓
    Temporal retry (10s later)
           ↓
BPM Worker → Customer Service (DOWN) ❌
           ↓
    ... continues retrying for 20 minutes
           ↓
Eventually succeeds or workflow timeout
```

**Handling**: Multiple layers of retry provide high reliability

#### Scenario 4: Permanent Failure

```
BPM Worker → Customer Service ✅ → 202 Accepted
           ↓
    Email Service (DOWN for 5+ minutes)
           ↓
    Retry #1 (10s later) ❌
    Retry #2 (30s later) ❌
    Retry #3 (70s later) ❌
    Retry #4 (150s later) ❌
    Retry #5 (310s later) ❌
           ↓
    @Recover method called
           ↓
    Log: "Exhausted all retries - giving up"
           ↓
    Optional: Store in dead letter queue
```

**Handling**: After 5 attempts (total ~9 minutes), give up and log

---

## Implementation Guide

### Step 1: Customer Service Notification Endpoints

```java
package com.souk.customer.api;

import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/customers/{customerId}")
public class CustomerNotificationController {

    private final CustomerRepository customerRepository;
    private final EmailService emailService;

    // ===== Order Notifications =====

    @PostMapping("/notify-order-confirmed")
    public ResponseEntity<Void> notifyOrderConfirmed(
            @PathVariable Long customerId,
            @RequestBody OrderConfirmedNotification notification) {

        sendOrderConfirmedAsync(customerId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(
        value = {EmailException.class, MailSendException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 10000, multiplier = 2)
    )
    private void sendOrderConfirmedAsync(Long customerId, OrderConfirmedNotification notification) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (!customer.isEmailNotificationsEnabled()) {
            log.info("Customer {} has email notifications disabled", customerId);
            return;
        }

        emailService.sendOrderConfirmedEmail(
            customer.getEmail(),
            notification.getOrderId(),
            notification.getOrderTotal()
        );

        log.info("Successfully notified customer {} about order confirmation", customerId);
    }

    @Recover
    private void recoverOrderConfirmed(Exception e, Long customerId, OrderConfirmedNotification notification) {
        log.error("Exhausted all retries for customer {} order confirmation - giving up. Error: {}",
            customerId, e.getMessage());
        // Optional: Store in dead letter queue for manual review
    }

    // ===== Order Status Notifications =====

    @PostMapping("/notify-order-shipped")
    public ResponseEntity<Void> notifyOrderShipped(
            @PathVariable Long customerId,
            @RequestBody OrderShippedNotification notification) {

        sendOrderShippedAsync(customerId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(
        value = {EmailException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 10000, multiplier = 2)
    )
    private void sendOrderShippedAsync(Long customerId, OrderShippedNotification notification) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        emailService.sendOrderShippedEmail(
            customer.getEmail(),
            notification.getOrderId(),
            notification.getTrackingNumber()
        );

        log.info("Successfully notified customer {} about order shipment", customerId);
    }

    @PostMapping("/notify-order-delivered")
    public ResponseEntity<Void> notifyOrderDelivered(
            @PathVariable Long customerId,
            @RequestBody OrderDeliveredNotification notification) {

        sendOrderDeliveredAsync(customerId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 10000, multiplier = 2))
    private void sendOrderDeliveredAsync(Long customerId, OrderDeliveredNotification notification) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        emailService.sendOrderDeliveredEmail(customer.getEmail(), notification.getOrderId());
        log.info("Successfully notified customer {} about order delivery", customerId);
    }

    // ===== Payment/Refund Notifications =====

    @PostMapping("/notify-refund-processed")
    public ResponseEntity<Void> notifyRefundProcessed(
            @PathVariable Long customerId,
            @RequestBody RefundProcessedNotification notification) {

        sendRefundProcessedAsync(customerId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 10000, multiplier = 2))
    private void sendRefundProcessedAsync(Long customerId, RefundProcessedNotification notification) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        emailService.sendRefundProcessedEmail(
            customer.getEmail(),
            notification.getOrderId(),
            notification.getRefundAmount()
        );

        log.info("Successfully notified customer {} about refund", customerId);
    }

    @PostMapping("/notify-payment-failed")
    public ResponseEntity<Void> notifyPaymentFailed(
            @PathVariable Long customerId,
            @RequestBody PaymentFailedNotification notification) {

        sendPaymentFailedAsync(customerId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 10000, multiplier = 2))
    private void sendPaymentFailedAsync(Long customerId, PaymentFailedNotification notification) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        emailService.sendPaymentFailedEmail(
            customer.getEmail(),
            notification.getOrderId(),
            notification.getReason()
        );

        log.info("Successfully notified customer {} about payment failure", customerId);
    }
}
```

### Step 2: Vendor Service Notification Endpoints

```java
package com.souk.vendor.api;

import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/vendors/{vendorId}")
public class VendorNotificationController {

    private final VendorRepository vendorRepository;
    private final EmailService emailService;

    // ===== Order Notifications =====

    @PostMapping("/notify-new-order")
    public ResponseEntity<Void> notifyNewOrder(
            @PathVariable Long vendorId,
            @RequestBody NewOrderNotification notification) {

        sendNewOrderAsync(vendorId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(
        value = {EmailException.class, MailSendException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 10000, multiplier = 2)
    )
    private void sendNewOrderAsync(Long vendorId, NewOrderNotification notification) {
        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new VendorNotFoundException(vendorId));

        if (!vendor.isNotificationsEnabled()) {
            log.info("Vendor {} has notifications disabled", vendorId);
            return;
        }

        emailService.sendNewOrderEmail(
            vendor.getEmail(),
            notification.getOrderId(),
            notification.getOrderItems(),
            notification.getOrderTotal()
        );

        log.info("Successfully notified vendor {} about new order {}",
            vendorId, notification.getOrderId());
    }

    @Recover
    private void recoverNewOrder(Exception e, Long vendorId, NewOrderNotification notification) {
        log.error("Exhausted all retries for vendor {} new order notification - giving up. Error: {}",
            vendorId, e.getMessage());
    }

    @PostMapping("/notify-order-cancelled")
    public ResponseEntity<Void> notifyOrderCancelled(
            @PathVariable Long vendorId,
            @RequestBody OrderCancelledNotification notification) {

        sendOrderCancelledAsync(vendorId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 10000, multiplier = 2))
    private void sendOrderCancelledAsync(Long vendorId, OrderCancelledNotification notification) {
        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();

        emailService.sendOrderCancelledEmail(
            vendor.getEmail(),
            notification.getOrderId(),
            notification.getCancellationReason()
        );

        log.info("Successfully notified vendor {} about order cancellation", vendorId);
    }

    // ===== Inventory Notifications =====

    @PostMapping("/notify-low-inventory")
    public ResponseEntity<Void> notifyLowInventory(
            @PathVariable Long vendorId,
            @RequestBody LowInventoryNotification notification) {

        sendLowInventoryAsync(vendorId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 10000, multiplier = 2))
    private void sendLowInventoryAsync(Long vendorId, LowInventoryNotification notification) {
        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();

        emailService.sendLowInventoryEmail(
            vendor.getEmail(),
            notification.getProductId(),
            notification.getProductName(),
            notification.getCurrentStock(),
            notification.getThreshold()
        );

        log.info("Successfully notified vendor {} about low inventory", vendorId);
    }

    // ===== Financial Notifications =====

    @PostMapping("/notify-payout-processed")
    public ResponseEntity<Void> notifyPayoutProcessed(
            @PathVariable Long vendorId,
            @RequestBody PayoutProcessedNotification notification) {

        sendPayoutProcessedAsync(vendorId, notification);
        return ResponseEntity.accepted().build();
    }

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 10000, multiplier = 2))
    private void sendPayoutProcessedAsync(Long vendorId, PayoutProcessedNotification notification) {
        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();

        emailService.sendPayoutProcessedEmail(
            vendor.getEmail(),
            notification.getPayoutId(),
            notification.getAmount(),
            notification.getPeriod()
        );

        log.info("Successfully notified vendor {} about payout", vendorId);
    }
}
```

### Step 3: Enable Async and Retry

```java
package com.souk.customer.config; // Same for vendor-service

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

### Step 4: BPM Worker Activities

```java
package com.souk.bpm.order.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import java.time.Duration;

@ActivityInterface
public interface OrderActivities {

    @ActivityMethod
    void notifyOrderConfirmed(Long orderId);

    @ActivityMethod
    void notifyVendorsOfNewOrder(Long orderId);
}

// Implementation
@Component
public class OrderActivitiesImpl implements OrderActivities {

    private final OrderServiceClient orderServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final VendorServiceClient vendorServiceClient;

    @Override
    public void notifyOrderConfirmed(Long orderId) {
        Order order = orderServiceClient.getOrder(orderId);

        // Notify customer - Temporal will retry if this fails
        customerServiceClient.notifyOrderConfirmed(
            order.getCustomerId(),
            new OrderConfirmedNotification(orderId, order.getTotalAmount())
        );

        log.info("Customer notification sent for order {}", orderId);
    }

    @Override
    public void notifyVendorsOfNewOrder(Long orderId) {
        Order order = orderServiceClient.getOrder(orderId);

        // Notify each vendor - Temporal will retry if any fail
        for (Long vendorId : order.getVendorIds()) {
            vendorServiceClient.notifyNewOrder(
                vendorId,
                new NewOrderNotification(orderId, order.getTotalAmount())
            );

            log.info("Vendor {} notified of new order {}", vendorId, orderId);
        }
    }
}

// Workflow configuration
public class OrderWorkflowImpl implements OrderWorkflow {

    private final OrderActivities activities = Workflow.newActivityStub(
        OrderActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(10))
                .setMaximumInterval(Duration.ofMinutes(5))
                .setBackoffCoefficient(2.0)
                .setMaximumAttempts(10)
                .build())
            .build()
    );

    @Override
    public void processOrder(Long orderId) {
        // ... payment, inventory, etc.

        // Notify customer (with automatic retry)
        activities.notifyOrderConfirmed(orderId);

        // Notify vendors (with automatic retry)
        activities.notifyVendorsOfNewOrder(orderId);

        // Workflow continues...
    }
}
```

### Step 5: Order Service (Best Effort)

```java
package com.souk.order.service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerServiceClient customerServiceClient;
    private final VendorServiceClient vendorServiceClient;

    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, newStatus);

        // Notify customer (best effort - don't block order update)
        notifyCustomerOfStatusChange(order, newStatus);
    }

    private void notifyCustomerOfStatusChange(Order order, OrderStatus newStatus) {
        try {
            switch (newStatus) {
                case SHIPPED -> customerServiceClient.notifyOrderShipped(
                    order.getCustomerId(),
                    new OrderShippedNotification(order.getOrderId(), order.getTrackingNumber())
                );

                case DELIVERED -> customerServiceClient.notifyOrderDelivered(
                    order.getCustomerId(),
                    new OrderDeliveredNotification(order.getOrderId())
                );

                case CANCELLED -> {
                    customerServiceClient.notifyOrderCancelled(
                        order.getCustomerId(),
                        new OrderCancelledNotification(order.getOrderId())
                    );
                    notifyVendorsOfCancellation(order);
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify customer {} about order {} status change: {}",
                order.getCustomerId(), order.getOrderId(), e.getMessage());
            // Don't throw - order update succeeded
        }
    }

    private void notifyVendorsOfCancellation(Order order) {
        for (Long vendorId : order.getVendorIds()) {
            try {
                vendorServiceClient.notifyOrderCancelled(
                    vendorId,
                    new OrderCancelledNotification(order.getOrderId())
                );
            } catch (Exception e) {
                log.error("Failed to notify vendor {} about order {} cancellation: {}",
                    vendorId, order.getOrderId(), e.getMessage());
                // Continue with other vendors
            }
        }
    }
}
```

---

## API Reference

### Customer Service Notification Endpoints

#### POST /customers/{customerId}/notify-order-confirmed
Notify customer that order has been confirmed and payment captured.

**Request Body:**
```json
{
  "orderId": 123,
  "orderTotal": 150.00,
  "estimatedDelivery": "2026-02-15"
}
```

**Response:** `202 Accepted`

---

#### POST /customers/{customerId}/notify-order-shipped
Notify customer that order has been shipped.

**Request Body:**
```json
{
  "orderId": 123,
  "trackingNumber": "TRACK123456",
  "carrier": "DHL",
  "estimatedDelivery": "2026-02-15"
}
```

**Response:** `202 Accepted`

---

#### POST /customers/{customerId}/notify-order-delivered
Notify customer that order has been delivered.

**Request Body:**
```json
{
  "orderId": 123,
  "deliveredAt": "2026-02-14T15:30:00Z"
}
```

**Response:** `202 Accepted`

---

#### POST /customers/{customerId}/notify-refund-processed
Notify customer that refund has been processed.

**Request Body:**
```json
{
  "orderId": 123,
  "refundAmount": 150.00,
  "refundMethod": "ORIGINAL_PAYMENT_METHOD",
  "estimatedDays": "3-5 business days"
}
```

**Response:** `202 Accepted`

---

#### POST /customers/{customerId}/notify-payment-failed
Notify customer that payment failed.

**Request Body:**
```json
{
  "orderId": 123,
  "reason": "Insufficient funds",
  "retryUrl": "https://app.soukconect.com/orders/123/retry-payment"
}
```

**Response:** `202 Accepted`

---

### Vendor Service Notification Endpoints

#### POST /vendors/{vendorId}/notify-new-order
Notify vendor of a new order.

**Request Body:**
```json
{
  "orderId": 123,
  "orderItems": [
    {
      "productId": 456,
      "productName": "Product A",
      "quantity": 2,
      "price": 75.00
    }
  ],
  "orderTotal": 150.00,
  "customerName": "John Doe",
  "deliveryAddress": "123 Main St"
}
```

**Response:** `202 Accepted`

---

#### POST /vendors/{vendorId}/notify-order-cancelled
Notify vendor that order was cancelled.

**Request Body:**
```json
{
  "orderId": 123,
  "cancellationReason": "Customer requested cancellation",
  "cancelledAt": "2026-02-14T10:30:00Z"
}
```

**Response:** `202 Accepted`

---

#### POST /vendors/{vendorId}/notify-low-inventory
Notify vendor of low inventory.

**Request Body:**
```json
{
  "productId": 456,
  "productName": "Product A",
  "currentStock": 5,
  "threshold": 10,
  "recommended": 50
}
```

**Response:** `202 Accepted`

---

#### POST /vendors/{vendorId}/notify-payout-processed
Notify vendor that payout has been processed.

**Request Body:**
```json
{
  "payoutId": "payout_xyz123",
  "amount": 2500.00,
  "period": "2026-02-01 to 2026-02-07",
  "transactionId": "txn_abc456"
}
```

**Response:** `202 Accepted`

---

## Future: Notification Service

### Migration Plan

When creating the centralized notification-service:

#### Phase 1: Create Notification Service
- New microservice: `notification-service`
- Implement all delivery channels (Email, SMS, Push, Webhook)
- Unified API: `POST /notifications/send`

#### Phase 2: Migrate Email from Auth Service
- Move `EmailService` from auth-service to notification-service
- Auth service calls notification-service
- Keep same notification endpoints

#### Phase 3: Update Domain Services
- Customer Service → Notification Service (instead of direct email)
- Vendor Service → Notification Service (instead of direct email)
- Keep same external API (callers don't change)

#### Phase 4: Add New Channels
- SMS via Twilio/SNS
- Push via FCM/APNS
- WhatsApp Business API
- In-app notifications

### Future Architecture

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│Order Service│  │ BPM Worker  │  │Auth Service │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       ▼                ▼                │
┌─────────────┐  ┌─────────────┐        │
│  Customer   │  │   Vendor    │        │
│  Service    │  │   Service   │        │
└──────┬──────┘  └──────┬──────┘        │
       │                │                │
       └────────┬───────┴────────────────┘
                ▼
    ┌──────────────────────────┐
    │  Notification Service    │
    │  - Email (SMTP/SES)      │
    │  - SMS (Twilio/SNS)      │
    │  - Push (FCM/APNS)       │
    │  - Webhook (HTTP)        │
    │  - WhatsApp Business     │
    └──────────────────────────┘
```

**Key Point**: External APIs stay the same. Only internal implementation changes.

---

## Summary

### Current State

✅ **Customer notifications** → Customer Service → Email
✅ **Vendor notifications** → Vendor Service → Email
✅ **Auth notifications** → Auth Service → Email (direct)
✅ **BPM retry** → Temporal automatic retry
✅ **Domain service retry** → @Async + @Retryable
✅ **Non-blocking** → All endpoints return 202 Accepted

### Key Patterns

1. **Domain services own notifications** - Single entry point per domain
2. **Callers pass only IDs** - Simple, decoupled
3. **Async processing** - Non-blocking, returns 202 immediately
4. **Multi-level retry** - Temporal + @Retryable + exponential backoff
5. **Graceful degradation** - Log and continue, don't block business logic

### Reliability

- **BPM notifications**: Very reliable (10 retries over 20 minutes)
- **Email delivery**: Reliable (5 retries over ~9 minutes)
- **Best effort calls**: Eventual consistency accepted

---

## Contact

For questions or updates to this architecture, contact the platform team or update this document.

**Last Updated**: 2026-02-08
**Version**: 1.0
