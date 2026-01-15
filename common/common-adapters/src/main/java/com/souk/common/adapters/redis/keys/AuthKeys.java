package com.souk.common.adapters.redis.keys;

/**
 * Authentication-related Redis keys.
 * Namespace: auth:*
 */
public class AuthKeys extends RedisKeyManager {

    private static final String PREFIX = "auth";

    /**
     * Refresh token storage.
     * Pattern: auth:refresh_token:{uuid}
     * TTL: 7 days
     *
     * @param tokenUuid Refresh token UUID
     * @return Redis key
     */
    public static String refreshToken(String tokenUuid) {
        return buildKey(PREFIX, "refresh_token", tokenUuid);
    }

    /**
     * Blacklisted access tokens (after logout).
     * Pattern: auth:blacklist:access_token:{jti}
     * TTL: 15 minutes
     *
     * @param jti JWT Token ID (jti claim)
     * @return Redis key
     */
    public static String accessTokenBlacklist(String jti) {
        return buildKey(PREFIX, "blacklist", "access_token", jti);
    }

    /**
     * Email verification tokens.
     * Pattern: auth:email_verification:{token}
     * TTL: 24 hours
     *
     * @param token Email verification token
     * @return Redis key
     */
    public static String emailVerification(String token) {
        return buildKey(PREFIX, "email_verification", token);
    }

    /**
     * Password reset tokens.
     * Pattern: auth:password_reset:{token}
     * TTL: 1 hour
     *
     * @param token Password reset token
     * @return Redis key
     */
    public static String passwordReset(String token) {
        return buildKey(PREFIX, "password_reset", token);
    }

    /**
     * Failed login attempt counter.
     * Pattern: auth:failed_login:{email}
     * TTL: 15 minutes
     *
     * @param email User email
     * @return Redis key
     */
    public static String failedLogin(String email) {
        return buildKey(PREFIX, "failed_login", email);
    }

    /**
     * Active sessions for a user.
     * Pattern: auth:sessions:{user_id}
     * TTL: 30 days
     *
     * @param userId User ID
     * @return Redis key
     */
    public static String userSessions(Long userId) {
        return buildKey(PREFIX, "sessions", userId);
    }

    /**
     * MFA verification code.
     * Pattern: auth:mfa:code:{user_id}
     * TTL: 5 minutes
     *
     * @param userId User ID
     * @return Redis key
     */
    public static String mfaCode(Long userId) {
        return buildKey(PREFIX, "mfa", "code", userId);
    }

    /**
     * Pattern for scanning all auth keys.
     *
     * @return Pattern key
     */
    public static String allAuthKeysPattern() {
        return buildPattern(PREFIX, "*");
    }
}
