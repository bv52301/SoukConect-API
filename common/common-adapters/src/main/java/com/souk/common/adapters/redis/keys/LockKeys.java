package com.souk.common.adapters.redis.keys;

/**
 * Distributed lock Redis keys.
 * Namespace: lock:*
 */
public class LockKeys extends RedisKeyManager {

    private static final String PREFIX = "lock";

    /**
     * Inventory lock (prevent overselling).
     * Pattern: lock:inventory:{product_id}
     * TTL: 30 seconds
     */
    public static String inventoryLock(Long productId) {
        return buildKey(PREFIX, "inventory", productId);
    }

    /**
     * Order creation lock (prevent duplicate submission).
     * Pattern: lock:order:create:{user_id}
     * TTL: 10 seconds
     */
    public static String orderCreationLock(Long userId) {
        return buildKey(PREFIX, "order", "create", userId);
    }

    public static String allLockKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
