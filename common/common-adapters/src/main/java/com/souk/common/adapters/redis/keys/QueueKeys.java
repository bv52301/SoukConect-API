package com.souk.common.adapters.redis.keys;

/**
 * Background job queue Redis keys.
 * Namespace: queue:*
 */
public class QueueKeys extends RedisKeyManager {

    private static final String PREFIX = "queue";

    /**
     * Email send queue by priority.
     * Pattern: queue:email:{priority}
     * TTL: Persistent
     * Type: List
     */
    public static String emailQueue(String priority) {
        return buildKey(PREFIX, "email", priority);
    }

    /**
     * Order processing queue.
     * Pattern: queue:order:processing
     * TTL: Persistent
     * Type: List
     */
    public static String orderProcessingQueue() {
        return buildKey(PREFIX, "order", "processing");
    }

    public static String allQueueKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
