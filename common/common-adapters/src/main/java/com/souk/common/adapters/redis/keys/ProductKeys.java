package com.souk.common.adapters.redis.keys;

/**
 * Product-related Redis keys.
 * Namespace: product:*
 */
public class ProductKeys extends RedisKeyManager {

    private static final String PREFIX = "product";

    /**
     * Product inventory levels.
     * Pattern: product:inventory:{product_id}
     * TTL: 5 minutes
     */
    public static String productInventory(Long productId) {
        return buildKey(PREFIX, "inventory", productId);
    }

    /**
     * Products by vendor.
     * Pattern: product:vendor:{vendor_id}
     * TTL: 15 minutes
     * Type: Set
     */
    public static String productsByVendor(Long vendorId) {
        return buildKey(PREFIX, "vendor", vendorId);
    }

    /**
     * Products by category (sorted by popularity/price).
     * Pattern: product:category:{category}:{subcategory}
     * TTL: 15 minutes
     * Type: Sorted Set
     */
    public static String productsByCategory(String category, String subcategory) {
        return buildKey(PREFIX, "category", category, subcategory);
    }

    /**
     * Cached search results.
     * Pattern: product:search:{query_hash}
     * TTL: 10 minutes
     * Type: List
     */
    public static String searchResults(String queryHash) {
        return buildKey(PREFIX, "search", queryHash);
    }

    public static String allProductKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
