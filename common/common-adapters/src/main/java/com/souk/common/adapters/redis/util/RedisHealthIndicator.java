package com.souk.common.adapters.redis.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis health indicator for Spring Boot Actuator.
 * Exposes Redis health status at /actuator/health endpoint.
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            // Test connection with PING
            String pingResponse = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            if (!"PONG".equalsIgnoreCase(pingResponse)) {
                return Health.down()
                    .withDetail("error", "Invalid PING response: " + pingResponse)
                    .build();
            }

            // Get additional details
            Map<String, Object> details = new HashMap<>();

            // Get key count
            try {
                Long keyCount = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .dbSize();
                details.put("keys", keyCount);
            } catch (Exception e) {
                details.put("keys", "unavailable");
            }

            // Get memory info
            try {
                var info = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .info("memory");

                if (info != null && info.getProperty("used_memory_human") != null) {
                    details.put("memory", info.getProperty("used_memory_human"));
                }
            } catch (Exception e) {
                details.put("memory", "unavailable");
            }

            // Get server info
            try {
                var info = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .info("server");

                if (info != null) {
                    if (info.getProperty("redis_version") != null) {
                        details.put("version", info.getProperty("redis_version"));
                    }
                    if (info.getProperty("uptime_in_seconds") != null) {
                        details.put("uptime_seconds", info.getProperty("uptime_in_seconds"));
                    }
                }
            } catch (Exception e) {
                // Server info is optional
            }

            details.put("status", "connected");

            return Health.up()
                .withDetails(details)
                .build();

        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
