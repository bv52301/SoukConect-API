package com.souk.common.adapters.redis.keys;

/**
 * Rate limiting Redis keys.
 * Namespace: ratelimit:*
 */
public class RateLimitKeys extends RedisKeyManager {

    private static final String PREFIX = "ratelimit";

    /**
     * Rate limit by user.
     * Pattern: ratelimit:user:{user_id}:{endpoint}:{window}
     * TTL: Dynamic (based on window)
     * Type: String (counter)
     */
    public static String userRateLimit(Long userId, String endpoint, String window) {
        return buildKey(PREFIX, "user", userId, endpoint, window);
    }

    /**
     * Rate limit by IP address.
     * Pattern: ratelimit:ip:{ip_address}:{endpoint}:{window}
     * TTL: Dynamic (based on window)
     * Type: String (counter)
     */
    public static String ipRateLimit(String ipAddress, String endpoint, String window) {
        return buildKey(PREFIX, "ip", ipAddress, endpoint, window);
    }

    public static String allRateLimitKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
