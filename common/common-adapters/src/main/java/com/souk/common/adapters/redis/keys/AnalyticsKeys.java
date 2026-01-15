package com.souk.common.adapters.redis.keys;

/**
 * Analytics and metrics Redis keys.
 * Namespace: analytics:*
 */
public class AnalyticsKeys extends RedisKeyManager {

    private static final String PREFIX = "analytics";

    /**
     * Trending products.
     * Pattern: analytics:trending:products:{period}
     * TTL: 1 hour
     * Type: Sorted Set (score = view count)
     */
    public static String trendingProducts(String period) {
        return buildKey(PREFIX, "trending", "products", period);
    }

    /**
     * Product view count by date.
     * Pattern: analytics:product:views:{product_id}:{date}
     * TTL: 30 days
     * Type: String (counter)
     */
    public static String productViews(Long productId, String date) {
        return buildKey(PREFIX, "product", "views", productId, date);
    }

    /**
     * Vendor performance metrics.
     * Pattern: analytics:vendor:metrics:{vendor_id}:{metric}:{date}
     * TTL: 7 days
     */
    public static String vendorMetrics(Long vendorId, String metric, String date) {
        return buildKey(PREFIX, "vendor", "metrics", vendorId, metric, date);
    }

    public static String allAnalyticsKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
