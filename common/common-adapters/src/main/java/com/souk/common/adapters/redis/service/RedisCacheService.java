package com.souk.common.adapters.redis.service;

import com.souk.common.adapters.redis.config.RedisTTL;
import com.souk.common.adapters.redis.keys.CacheKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Generic Redis caching service.
 * Provides common cache operations for all services.
 */
@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ============================================
    // Generic Cache Operations
    // ============================================

    /**
     * Cache a value with TTL.
     */
    public <T> void set(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Get cached value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    /**
     * Delete cached value.
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Check if key exists.
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Delete multiple keys by pattern.
     * WARNING: Use cautiously, KEYS command is slow on large datasets.
     */
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Set expiration on existing key.
     */
    public void expire(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    // ============================================
    // User Cache Operations
    // ============================================

    /**
     * Cache user profile.
     */
    public void cacheUserProfile(Long userId, Object userProfile) {
        String key = CacheKeys.userProfile(userId);
        set(key, userProfile, RedisTTL.USER_PROFILE_CACHE);
    }

    /**
     * Get cached user profile.
     */
    public <T> T getUserProfile(Long userId, Class<T> type) {
        String key = CacheKeys.userProfile(userId);
        return get(key, type);
    }

    /**
     * Invalidate user profile cache.
     */
    public void invalidateUserProfile(Long userId) {
        String key = CacheKeys.userProfile(userId);
        delete(key);
    }

    /**
     * Cache user roles.
     */
    public void cacheUserRoles(Long userId, Set<String> roles) {
        String key = CacheKeys.userRoles(userId);
        redisTemplate.opsForSet().add(key, roles.toArray());
        expire(key, RedisTTL.USER_ROLES_CACHE);
    }

    /**
     * Get cached user roles.
     */
    public Set<Object> getUserRoles(Long userId) {
        String key = CacheKeys.userRoles(userId);
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Invalidate all user-related cache.
     */
    public void invalidateAllUserCache(Long userId) {
        invalidateUserProfile(userId);
        delete(CacheKeys.userRoles(userId));
        delete(CacheKeys.userPermissions(userId));
    }

    // ============================================
    // Vendor Cache Operations
    // ============================================

    /**
     * Cache vendor details.
     */
    public void cacheVendorDetails(Long vendorId, Object vendorDetails) {
        String key = CacheKeys.vendorDetails(vendorId);
        set(key, vendorDetails, RedisTTL.VENDOR_DETAILS_CACHE);
    }

    /**
     * Get cached vendor details.
     */
    public <T> T getVendorDetails(Long vendorId, Class<T> type) {
        String key = CacheKeys.vendorDetails(vendorId);
        return get(key, type);
    }

    /**
     * Invalidate vendor details cache.
     */
    public void invalidateVendorDetails(Long vendorId) {
        String key = CacheKeys.vendorDetails(vendorId);
        delete(key);
    }

    // ============================================
    // Product Cache Operations
    // ============================================

    /**
     * Cache product details.
     */
    public void cacheProductDetails(Long productId, Object productDetails) {
        String key = CacheKeys.productDetails(productId);
        set(key, productDetails, RedisTTL.PRODUCT_DETAILS_CACHE);
    }

    /**
     * Get cached product details.
     */
    public <T> T getProductDetails(Long productId, Class<T> type) {
        String key = CacheKeys.productDetails(productId);
        return get(key, type);
    }

    /**
     * Invalidate product details cache.
     */
    public void invalidateProductDetails(Long productId) {
        String key = CacheKeys.productDetails(productId);
        delete(key);
    }

    // ============================================
    // Category Cache Operations
    // ============================================

    /**
     * Cache category list.
     */
    public void cacheCategoryList(Object categories) {
        String key = CacheKeys.categoryList();
        set(key, categories, RedisTTL.CATEGORY_LIST_CACHE);
    }

    /**
     * Get cached category list.
     */
    public <T> T getCategoryList(Class<T> type) {
        String key = CacheKeys.categoryList();
        return get(key, type);
    }

    /**
     * Invalidate category list cache.
     */
    public void invalidateCategoryList() {
        String key = CacheKeys.categoryList();
        delete(key);
    }

    // ============================================
    // Homepage Cache Operations
    // ============================================

    /**
     * Cache homepage data.
     */
    public void cacheHomepage(String version, Object homepageData) {
        String key = CacheKeys.homepage(version);
        set(key, homepageData, RedisTTL.HOMEPAGE_CACHE);
    }

    /**
     * Get cached homepage data.
     */
    public <T> T getHomepage(String version, Class<T> type) {
        String key = CacheKeys.homepage(version);
        return get(key, type);
    }

    /**
     * Invalidate homepage cache.
     */
    public void invalidateHomepage(String version) {
        String key = CacheKeys.homepage(version);
        delete(key);
    }

    // ============================================
    // Bulk Cache Invalidation
    // ============================================

    /**
     * Invalidate all product caches.
     * WARNING: Use cautiously on large datasets.
     */
    public void invalidateAllProductCache() {
        deleteByPattern(CacheKeys.allProductCachePattern());
    }

    /**
     * Invalidate all vendor caches.
     * WARNING: Use cautiously on large datasets.
     */
    public void invalidateAllVendorCache() {
        deleteByPattern(CacheKeys.allVendorCachePattern());
    }

    /**
     * Invalidate all caches.
     * WARNING: Use cautiously on large datasets.
     */
    public void invalidateAllCache() {
        deleteByPattern(CacheKeys.allCacheKeysPattern());
    }
}
