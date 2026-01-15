package com.souk.common.adapters.redis.keys;

/**
 * Real-time feature Redis keys.
 * Namespace: realtime:*
 */
public class RealtimeKeys extends RedisKeyManager {

    private static final String PREFIX = "realtime";

    /**
     * Online users tracking.
     * Pattern: realtime:online_users
     * TTL: Persistent
     * Type: Sorted Set (score = last_active timestamp)
     */
    public static String onlineUsers() {
        return buildKey(PREFIX, "online_users");
    }

    /**
     * Vendor online status.
     * Pattern: realtime:vendor:online:{vendor_id}
     * TTL: 5 minutes
     */
    public static String vendorOnline(Long vendorId) {
        return buildKey(PREFIX, "vendor", "online", vendorId);
    }

    /**
     * User notification queue.
     * Pattern: realtime:notification:{user_id}
     * TTL: 7 days
     * Type: List
     */
    public static String userNotifications(Long userId) {
        return buildKey(PREFIX, "notification", userId);
    }

    public static String allRealtimeKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
