package com.souk.common.adapters.redis.keys;

/**
 * Shopping cart Redis keys.
 * Namespace: cart:*
 */
public class CartKeys extends RedisKeyManager {

    private static final String PREFIX = "cart";

    /**
     * User shopping cart.
     * Pattern: cart:{user_id}
     * TTL: 7 days
     * Type: Hash (product_id → quantity)
     */
    public static String userCart(Long userId) {
        return buildKey(PREFIX, userId);
    }

    /**
     * Guest shopping cart.
     * Pattern: cart:guest:{session_id}
     * TTL: 24 hours
     * Type: Hash (product_id → quantity)
     */
    public static String guestCart(String sessionId) {
        return buildKey(PREFIX, "guest", sessionId);
    }

    public static String allCartKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
