package com.souk.common.adapters.redis.config;

import java.time.Duration;

/**
 * Redis TTL (Time-To-Live) constants for all key types.
 * Centralized management of expiration times.
 */
public class RedisTTL {

    // ============================================
    // Authentication & Sessions
    // ============================================

    /** Refresh token expiration: 7 days */
    public static final Duration REFRESH_TOKEN = Duration.ofDays(7);

    /** Access token blacklist (after logout): 15 minutes */
    public static final Duration ACCESS_TOKEN_BLACKLIST = Duration.ofMinutes(15);

    /** Email verification token: 24 hours */
    public static final Duration EMAIL_VERIFICATION = Duration.ofHours(24);

    /** Password reset token: 1 hour */
    public static final Duration PASSWORD_RESET = Duration.ofHours(1);

    /** Failed login attempt counter: 15 minutes */
    public static final Duration FAILED_LOGIN = Duration.ofMinutes(15);

    /** User active sessions: 30 days */
    public static final Duration USER_SESSIONS = Duration.ofDays(30);

    /** MFA verification code: 5 minutes */
    public static final Duration MFA_CODE = Duration.ofMinutes(5);

    // ============================================
    // Caching
    // ============================================

    /** User profile cache: 1 hour */
    public static final Duration USER_PROFILE_CACHE = Duration.ofHours(1);

    /** User roles cache: 1 hour */
    public static final Duration USER_ROLES_CACHE = Duration.ofHours(1);

    /** Vendor details cache: 30 minutes */
    public static final Duration VENDOR_DETAILS_CACHE = Duration.ofMinutes(30);

    /** Product details cache: 30 minutes */
    public static final Duration PRODUCT_DETAILS_CACHE = Duration.ofMinutes(30);

    /** Category list cache: 1 hour */
    public static final Duration CATEGORY_LIST_CACHE = Duration.ofHours(1);

    /** Homepage data cache: 10 minutes */
    public static final Duration HOMEPAGE_CACHE = Duration.ofMinutes(10);

    // ============================================
    // Shopping
    // ============================================

    /** User shopping cart: 7 days */
    public static final Duration USER_CART = Duration.ofDays(7);

    /** Guest shopping cart: 24 hours */
    public static final Duration GUEST_CART = Duration.ofHours(24);

    // ============================================
    // Orders
    // ============================================

    /** Order details cache: 1 hour */
    public static final Duration ORDER_DETAILS_CACHE = Duration.ofHours(1);

    /** Orders by customer/vendor: 30 minutes */
    public static final Duration ORDERS_LIST_CACHE = Duration.ofMinutes(30);

    /** Order status tracking: 24 hours */
    public static final Duration ORDER_STATUS = Duration.ofHours(24);

    // ============================================
    // Vendor & Product Data
    // ============================================

    /** Vendors by category: 15 minutes */
    public static final Duration VENDOR_CATEGORY_LIST = Duration.ofMinutes(15);

    /** Vendor active status: 5 minutes */
    public static final Duration VENDOR_ACTIVE_STATUS = Duration.ofMinutes(5);

    /** Product inventory: 5 minutes */
    public static final Duration PRODUCT_INVENTORY = Duration.ofMinutes(5);

    /** Products by vendor: 15 minutes */
    public static final Duration PRODUCTS_BY_VENDOR = Duration.ofMinutes(15);

    /** Products by category: 15 minutes */
    public static final Duration PRODUCTS_BY_CATEGORY = Duration.ofMinutes(15);

    /** Product search results: 10 minutes */
    public static final Duration PRODUCT_SEARCH_RESULTS = Duration.ofMinutes(10);

    // ============================================
    // Rate Limiting Windows
    // ============================================

    /** Rate limit window: 1 minute */
    public static final Duration RATE_LIMIT_1_MIN = Duration.ofMinutes(1);

    /** Rate limit window: 1 hour */
    public static final Duration RATE_LIMIT_1_HOUR = Duration.ofHours(1);

    // ============================================
    // Analytics
    // ============================================

    /** Trending products: 1 hour */
    public static final Duration TRENDING_PRODUCTS = Duration.ofHours(1);

    /** Product view count: 30 days */
    public static final Duration PRODUCT_VIEW_COUNT = Duration.ofDays(30);

    /** Vendor metrics: 7 days */
    public static final Duration VENDOR_METRICS = Duration.ofDays(7);

    // ============================================
    // Distributed Locks
    // ============================================

    /** Inventory lock during checkout: 30 seconds */
    public static final Duration INVENTORY_LOCK = Duration.ofSeconds(30);

    /** Order creation lock: 10 seconds */
    public static final Duration ORDER_CREATION_LOCK = Duration.ofSeconds(10);

    // ============================================
    // Real-time Features
    // ============================================

    /** Vendor online status: 5 minutes */
    public static final Duration VENDOR_ONLINE_STATUS = Duration.ofMinutes(5);

    /** User notifications: 7 days */
    public static final Duration USER_NOTIFICATIONS = Duration.ofDays(7);

    private RedisTTL() {
        // Utility class, prevent instantiation
    }
}
