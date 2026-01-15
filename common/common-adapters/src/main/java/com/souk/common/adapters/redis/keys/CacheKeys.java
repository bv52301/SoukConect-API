package com.souk.common.adapters.redis.keys;

/**
 * Caching-related Redis keys.
 * Namespace: cache:*
 */
public class CacheKeys extends RedisKeyManager {

    private static final String PREFIX = "cache";

    /**
     * User profile cache (includes roles and business profiles).
     * Pattern: cache:user:profile:{user_id}
     * TTL: 1 hour
     *
     * @param userId User ID
     * @return Redis key
     */
    public static String userProfile(Long userId) {
        return buildKey(PREFIX, "user", "profile", userId);
    }

    /**
     * User roles cache.
     * Pattern: cache:user:roles:{user_id}
     * TTL: 1 hour
     *
     * @param userId User ID
     * @return Redis key
     */
    public static String userRoles(Long userId) {
        return buildKey(PREFIX, "user", "roles", userId);
    }

    /**
     * User permissions cache (for ADMIN/SUPPORT roles).
     * Pattern: cache:user:permissions:{user_id}
     * TTL: 1 hour
     *
     * @param userId User ID
     * @return Redis key
     */
    public static String userPermissions(Long userId) {
        return buildKey(PREFIX, "user", "permissions", userId);
    }

    /**
     * Vendor details cache.
     * Pattern: cache:vendor:details:{vendor_id}
     * TTL: 30 minutes
     *
     * @param vendorId Vendor ID
     * @return Redis key
     */
    public static String vendorDetails(Long vendorId) {
        return buildKey(PREFIX, "vendor", "details", vendorId);
    }

    /**
     * Product details cache.
     * Pattern: cache:product:details:{product_id}
     * TTL: 30 minutes
     *
     * @param productId Product ID
     * @return Redis key
     */
    public static String productDetails(Long productId) {
        return buildKey(PREFIX, "product", "details", productId);
    }

    /**
     * Category and subcategory list cache.
     * Pattern: cache:categories
     * TTL: 1 hour
     *
     * @return Redis key
     */
    public static String categoryList() {
        return buildKey(PREFIX, "categories");
    }

    /**
     * Homepage data bundle cache.
     * Pattern: cache:homepage:{version}
     * TTL: 10 minutes
     *
     * @param version Cache version (e.g., "v1", "v2")
     * @return Redis key
     */
    public static String homepage(String version) {
        return buildKey(PREFIX, "homepage", version);
    }

    /**
     * Pattern for scanning all cache keys.
     *
     * @return Pattern key
     */
    public static String allCacheKeysPattern() {
        return buildPattern(PREFIX, "*");
    }

    /**
     * Pattern for scanning user-related cache keys.
     *
     * @return Pattern key
     */
    public static String allUserCachePattern() {
        return buildPattern(PREFIX, "user", "*");
    }

    /**
     * Pattern for scanning product-related cache keys.
     *
     * @return Pattern key
     */
    public static String allProductCachePattern() {
        return buildPattern(PREFIX, "product", "*");
    }

    /**
     * Pattern for scanning vendor-related cache keys.
     *
     * @return Pattern key
     */
    public static String allVendorCachePattern() {
        return buildPattern(PREFIX, "vendor", "*");
    }
}
