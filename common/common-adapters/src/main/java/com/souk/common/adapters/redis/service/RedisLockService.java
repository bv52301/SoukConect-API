package com.souk.common.adapters.redis.service;

import com.souk.common.adapters.redis.config.RedisTTL;
import com.souk.common.adapters.redis.keys.LockKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Distributed lock service using Redis.
 * Prevents race conditions in distributed systems.
 */
@Service
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);
    private static final String LOCK_VALUE = "LOCKED";

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    // ============================================
    // Generic Lock Operations
    // ============================================

    /**
     * Acquire a distributed lock.
     *
     * @param lockKey Lock identifier
     * @param timeout Lock expiration time
     * @return true if lock acquired, false if already locked
     */
    public boolean acquireLock(String lockKey, Duration timeout) {
        Boolean acquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, LOCK_VALUE, timeout);

        boolean result = Boolean.TRUE.equals(acquired);

        if (result) {
            log.debug("Lock acquired: {}", lockKey);
        } else {
            log.debug("Lock already held: {}", lockKey);
        }

        return result;
    }

    /**
     * Release a distributed lock.
     *
     * @param lockKey Lock identifier
     */
    public void releaseLock(String lockKey) {
        Boolean deleted = stringRedisTemplate.delete(lockKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Lock released: {}", lockKey);
        }
    }

    /**
     * Check if lock exists.
     *
     * @param lockKey Lock identifier
     * @return true if locked
     */
    public boolean isLocked(String lockKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
    }

    /**
     * Execute action with lock protection.
     *
     * @param lockKey Lock identifier
     * @param timeout Lock timeout
     * @param action Action to execute
     * @return true if action executed, false if lock not acquired
     */
    public boolean executeWithLock(String lockKey, Duration timeout, Runnable action) {
        if (acquireLock(lockKey, timeout)) {
            try {
                action.run();
                return true;
            } finally {
                releaseLock(lockKey);
            }
        }
        return false;
    }

    // ============================================
    // Inventory Lock Operations
    // ============================================

    /**
     * Lock product inventory (prevent overselling during checkout).
     *
     * @param productId Product ID
     * @return true if lock acquired
     */
    public boolean lockInventory(Long productId) {
        String lockKey = LockKeys.inventoryLock(productId);
        boolean acquired = acquireLock(lockKey, RedisTTL.INVENTORY_LOCK);

        if (!acquired) {
            log.warn("Failed to acquire inventory lock for product {}", productId);
        }

        return acquired;
    }

    /**
     * Unlock product inventory.
     *
     * @param productId Product ID
     */
    public void unlockInventory(Long productId) {
        String lockKey = LockKeys.inventoryLock(productId);
        releaseLock(lockKey);
    }

    /**
     * Check if inventory is locked.
     *
     * @param productId Product ID
     * @return true if locked
     */
    public boolean isInventoryLocked(Long productId) {
        String lockKey = LockKeys.inventoryLock(productId);
        return isLocked(lockKey);
    }

    // ============================================
    // Order Creation Lock Operations
    // ============================================

    /**
     * Lock order creation for a user (prevent duplicate submissions).
     *
     * @param userId User ID
     * @return true if lock acquired
     */
    public boolean lockOrderCreation(Long userId) {
        String lockKey = LockKeys.orderCreationLock(userId);
        boolean acquired = acquireLock(lockKey, RedisTTL.ORDER_CREATION_LOCK);

        if (!acquired) {
            log.warn("Failed to acquire order creation lock for user {}", userId);
        }

        return acquired;
    }

    /**
     * Unlock order creation.
     *
     * @param userId User ID
     */
    public void unlockOrderCreation(Long userId) {
        String lockKey = LockKeys.orderCreationLock(userId);
        releaseLock(lockKey);
    }

    /**
     * Check if order creation is locked.
     *
     * @param userId User ID
     * @return true if locked
     */
    public boolean isOrderCreationLocked(Long userId) {
        String lockKey = LockKeys.orderCreationLock(userId);
        return isLocked(lockKey);
    }

    // ============================================
    // Utility Methods
    // ============================================

    /**
     * Wait for lock with retry.
     *
     * @param lockKey Lock identifier
     * @param timeout Lock timeout
     * @param maxRetries Maximum retry attempts
     * @param retryDelayMs Delay between retries in milliseconds
     * @return true if lock acquired
     */
    public boolean acquireLockWithRetry(String lockKey, Duration timeout, int maxRetries, long retryDelayMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (acquireLock(lockKey, timeout)) {
                return true;
            }

            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Lock acquisition interrupted for {}", lockKey);
                return false;
            }
        }

        log.warn("Failed to acquire lock {} after {} retries", lockKey, maxRetries);
        return false;
    }
}
