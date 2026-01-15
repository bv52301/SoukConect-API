package com.souk.auth.service;

import com.souk.common.adapters.redis.keys.AuthKeys;
import com.souk.common.adapters.redis.config.RedisTTL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public SessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void createSession(Long userId, String refreshTokenUuid, String deviceInfo, String ipAddress) {
        String sessionsKey = AuthKeys.userSessions(userId);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("refreshToken", refreshTokenUuid);
        sessionData.put("deviceInfo", deviceInfo != null ? deviceInfo : "Unknown");
        sessionData.put("ipAddress", ipAddress != null ? ipAddress : "Unknown");
        sessionData.put("createdAt", System.currentTimeMillis());
        sessionData.put("lastActive", System.currentTimeMillis());

        // Store session in a hash set (keyed by refresh token UUID)
        redisTemplate.opsForHash().put(sessionsKey, refreshTokenUuid, sessionData);
        redisTemplate.expire(sessionsKey, RedisTTL.USER_SESSIONS.getSeconds(), TimeUnit.SECONDS);

        log.debug("Session created for user {} with token {}", userId, refreshTokenUuid);
    }

    public void updateSessionActivity(Long userId, String refreshTokenUuid) {
        String sessionsKey = AuthKeys.userSessions(userId);

        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = (Map<String, Object>) redisTemplate.opsForHash().get(sessionsKey, refreshTokenUuid);

        if (sessionData != null) {
            sessionData.put("lastActive", System.currentTimeMillis());
            redisTemplate.opsForHash().put(sessionsKey, refreshTokenUuid, sessionData);
        }
    }

    public void removeSession(Long userId, String refreshTokenUuid) {
        String sessionsKey = AuthKeys.userSessions(userId);
        redisTemplate.opsForHash().delete(sessionsKey, refreshTokenUuid);
        log.debug("Session removed for user {} with token {}", userId, refreshTokenUuid);
    }

    public void removeAllSessions(Long userId) {
        String sessionsKey = AuthKeys.userSessions(userId);
        redisTemplate.delete(sessionsKey);
        log.info("All sessions removed for user {}", userId);
    }

    public List<Map<String, Object>> getUserSessions(Long userId) {
        String sessionsKey = AuthKeys.userSessions(userId);
        Map<Object, Object> allSessions = redisTemplate.opsForHash().entries(sessionsKey);

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : allSessions.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = (Map<String, Object>) entry.getValue();
            sessionData.put("sessionId", entry.getKey());
            sessions.add(sessionData);
        }

        return sessions;
    }

    public int getActiveSessionCount(Long userId) {
        String sessionsKey = AuthKeys.userSessions(userId);
        Long size = redisTemplate.opsForHash().size(sessionsKey);
        return size != null ? size.intValue() : 0;
    }

    public void recordFailedLoginAttempt(String email) {
        String failedKey = AuthKeys.failedLogin(email);

        Long attempts = redisTemplate.opsForValue().increment(failedKey);
        if (attempts != null && attempts == 1) {
            // First attempt - set expiry
            redisTemplate.expire(failedKey, RedisTTL.FAILED_LOGIN.getSeconds(), TimeUnit.SECONDS);
        }
    }

    public int getFailedLoginAttempts(String email) {
        String failedKey = AuthKeys.failedLogin(email);
        Object value = redisTemplate.opsForValue().get(failedKey);

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        return 0;
    }

    public void clearFailedLoginAttempts(String email) {
        String failedKey = AuthKeys.failedLogin(email);
        redisTemplate.delete(failedKey);
    }

    public boolean isAccountLocked(String email, int maxAttempts) {
        return getFailedLoginAttempts(email) >= maxAttempts;
    }
}
