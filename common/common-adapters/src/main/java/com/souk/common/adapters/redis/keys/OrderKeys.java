package com.souk.common.adapters.redis.keys;

/**
 * Order-related Redis keys.
 * Namespace: order:*
 */
public class OrderKeys extends RedisKeyManager {

    private static final String PREFIX = "order";

    /**
     * Order details cache.
     * Pattern: order:details:{order_id}
     * TTL: 1 hour
     */
    public static String orderDetails(Long orderId) {
        return buildKey(PREFIX, "details", orderId);
    }

    /**
     * Orders by customer (sorted by timestamp).
     * Pattern: order:customer:{customer_id}
     * TTL: 30 minutes
     * Type: Sorted Set
     */
    public static String ordersByCustomer(Long customerId) {
        return buildKey(PREFIX, "customer", customerId);
    }

    /**
     * Orders by vendor (sorted by timestamp).
     * Pattern: order:vendor:{vendor_id}
     * TTL: 30 minutes
     * Type: Sorted Set
     */
    public static String ordersByVendor(Long vendorId) {
        return buildKey(PREFIX, "vendor", vendorId);
    }

    /**
     * Order status tracking.
     * Pattern: order:status:{order_id}
     * TTL: 24 hours
     */
    public static String orderStatus(Long orderId) {
        return buildKey(PREFIX, "status", orderId);
    }

    public static String allOrderKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
