package com.souk.common.adapters.redis.keys;

/**
 * Configuration Redis keys.
 * Namespace: config:*
 */
public class ConfigKeys extends RedisKeyManager {

    private static final String PREFIX = "config";

    /**
     * Rate limit configuration.
     * Pattern: config:ratelimit
     * TTL: Persistent
     * Type: Hash
     */
    public static String rateLimitConfig() {
        return buildKey(PREFIX, "ratelimit");
    }

    /**
     * Cache TTL configuration.
     * Pattern: config:cache
     * TTL: Persistent
     * Type: Hash
     */
    public static String cacheConfig() {
        return buildKey(PREFIX, "cache");
    }

    /**
     * Feature flags.
     * Pattern: config:features
     * TTL: Persistent
     * Type: Hash
     */
    public static String featureFlags() {
        return buildKey(PREFIX, "features");
    }

    public static String allConfigKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
