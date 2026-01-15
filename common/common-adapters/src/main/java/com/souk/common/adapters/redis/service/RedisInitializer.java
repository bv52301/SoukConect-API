package com.souk.common.adapters.redis.service;

import com.souk.common.adapters.redis.keys.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Redis initialization on application startup.
 * Sets up configuration keys and persistent data structures.
 */
@Component
public class RedisInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisInitializer.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("============================================");
        log.info("Initializing Redis for SoukConect");
        log.info("============================================");

        try {
            // Step 1: Test connection
            if (!testConnection()) {
                log.error("Redis connection failed! Skipping initialization.");
                return;
            }

            // Step 2: Initialize configuration keys
            initializeConfigKeys();

            // Step 3: Initialize persistent data structures
            initializePersistentStructures();

            // Step 4: Log summary
            logInitializationSummary();

            log.info("============================================");
            log.info("Redis initialization complete");
            log.info("============================================");

        } catch (Exception e) {
            log.error("Redis initialization failed", e);
        }
    }

    /**
     * Test Redis connection.
     */
    private boolean testConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("✓ Redis connection successful");
            return true;
        } catch (Exception e) {
            log.error("✗ Redis connection failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Initialize configuration keys.
     */
    private void initializeConfigKeys() {
        log.info("Initializing configuration keys...");

        // Rate limit configuration
        String rateLimitKey = ConfigKeys.rateLimitConfig();
        redisTemplate.opsForHash().putIfAbsent(rateLimitKey, "login_attempts_per_minute", "5");
        redisTemplate.opsForHash().putIfAbsent(rateLimitKey, "api_requests_per_minute", "100");
        redisTemplate.opsForHash().putIfAbsent(rateLimitKey, "order_creation_per_hour", "10");
        redisTemplate.opsForHash().putIfAbsent(rateLimitKey, "search_requests_per_minute", "30");
        log.info("✓ Rate limit configuration initialized");

        // Cache TTL configuration (in seconds)
        String cacheKey = ConfigKeys.cacheConfig();
        redisTemplate.opsForHash().putIfAbsent(cacheKey, "user_profile_ttl", "3600");
        redisTemplate.opsForHash().putIfAbsent(cacheKey, "product_details_ttl", "1800");
        redisTemplate.opsForHash().putIfAbsent(cacheKey, "vendor_details_ttl", "1800");
        redisTemplate.opsForHash().putIfAbsent(cacheKey, "category_list_ttl", "3600");
        redisTemplate.opsForHash().putIfAbsent(cacheKey, "homepage_ttl", "600");
        log.info("✓ Cache TTL configuration initialized");

        // Feature flags
        String featuresKey = ConfigKeys.featureFlags();
        redisTemplate.opsForHash().putIfAbsent(featuresKey, "enable_mfa", "true");
        redisTemplate.opsForHash().putIfAbsent(featuresKey, "enable_guest_checkout", "true");
        redisTemplate.opsForHash().putIfAbsent(featuresKey, "maintenance_mode", "false");
        redisTemplate.opsForHash().putIfAbsent(featuresKey, "enable_product_reviews", "true");
        redisTemplate.opsForHash().putIfAbsent(featuresKey, "enable_vendor_chat", "false");
        log.info("✓ Feature flags initialized");
    }

    /**
     * Initialize persistent data structures.
     */
    private void initializePersistentStructures() {
        log.info("Initializing persistent data structures...");

        // Vendor geospatial index
        String vendorGeoKey = VendorKeys.geoLocations();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(vendorGeoKey))) {
            log.info("✓ Vendor geospatial index ready (will be populated by application)");
        } else {
            Long vendorCount = redisTemplate.opsForZSet().size(vendorGeoKey);
            log.info("✓ Vendor geospatial index exists ({} vendors)", vendorCount);
        }

        // Online users sorted set
        String onlineUsersKey = RealtimeKeys.onlineUsers();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(onlineUsersKey))) {
            log.info("✓ Online users tracking ready");
        } else {
            Long onlineCount = redisTemplate.opsForZSet().size(onlineUsersKey);
            log.info("✓ Online users tracking exists ({} users)", onlineCount);
        }

        // Email queues
        for (String priority : Arrays.asList("high", "medium", "low")) {
            String queueKey = QueueKeys.emailQueue(priority);
            if (Boolean.FALSE.equals(redisTemplate.hasKey(queueKey))) {
                log.info("✓ Email queue initialized: {}", priority);
            } else {
                Long queueSize = redisTemplate.opsForList().size(queueKey);
                log.info("✓ Email queue exists: {} ({} items)", priority, queueSize);
            }
        }

        // Order processing queue
        String orderQueueKey = QueueKeys.orderProcessingQueue();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(orderQueueKey))) {
            log.info("✓ Order processing queue ready");
        } else {
            Long queueSize = redisTemplate.opsForList().size(orderQueueKey);
            log.info("✓ Order processing queue exists ({} items)", queueSize);
        }
    }

    /**
     * Log initialization summary.
     */
    private void logInitializationSummary() {
        try {
            Long totalKeys = redisTemplate.getConnectionFactory()
                .getConnection()
                .dbSize();

            log.info("");
            log.info("Redis Summary:");
            log.info("  Total Keys: {}", totalKeys);

            // Count keys by namespace
            log.info("  Keys by Namespace:");
            countKeysByNamespace("auth");
            countKeysByNamespace("cache");
            countKeysByNamespace("cart");
            countKeysByNamespace("order");
            countKeysByNamespace("vendor");
            countKeysByNamespace("product");
            countKeysByNamespace("config");

        } catch (Exception e) {
            log.warn("Could not retrieve Redis statistics: {}", e.getMessage());
        }
    }

    /**
     * Count keys by namespace prefix.
     */
    private void countKeysByNamespace(String namespace) {
        try {
            // Note: KEYS command should not be used in production on large datasets
            // This is only for initialization logging
            var keys = redisTemplate.keys(namespace + ":*");
            if (keys != null && !keys.isEmpty()) {
                log.info("    {}: {}", namespace, keys.size());
            }
        } catch (Exception e) {
            log.debug("Could not count keys for namespace {}: {}", namespace, e.getMessage());
        }
    }
}
