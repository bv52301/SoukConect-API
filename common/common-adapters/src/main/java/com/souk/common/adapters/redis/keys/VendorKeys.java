package com.souk.common.adapters.redis.keys;

/**
 * Vendor-related Redis keys.
 * Namespace: vendor:*
 */
public class VendorKeys extends RedisKeyManager {

    private static final String PREFIX = "vendor";

    /**
     * Vendors by category.
     * Pattern: vendor:category:{category}
     * TTL: 15 minutes
     * Type: Set
     */
    public static String vendorsByCategory(String category) {
        return buildKey(PREFIX, "category", category);
    }

    /**
     * Vendor active/online status.
     * Pattern: vendor:active:{vendor_id}
     * TTL: 5 minutes
     */
    public static String vendorActive(Long vendorId) {
        return buildKey(PREFIX, "active", vendorId);
    }

    /**
     * Vendor geospatial locations.
     * Pattern: vendor:geo:locations
     * TTL: Persistent
     * Type: Geospatial
     */
    public static String geoLocations() {
        return buildKey(PREFIX, "geo", "locations");
    }

    public static String allVendorKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
