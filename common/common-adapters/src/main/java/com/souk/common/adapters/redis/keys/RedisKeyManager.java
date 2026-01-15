package com.souk.common.adapters.redis.keys;

/**
 * Base class for Redis key management.
 * Provides utility methods for building type-safe Redis keys.
 *
 * <p>All keys are prefixed with "soukconect:" for namespace isolation.
 *
 * <p>Key naming convention: soukconect:{namespace}:{entity}:{identifier}:{attribute}
 *
 * <p>Example:
 * <pre>
 *   soukconect:auth:refresh_token:550e8400-e29b-41d4-9876-1a2b3c4d5e6f
 *   soukconect:cache:user:profile:100
 *   soukconect:cart:100
 * </pre>
 */
public abstract class RedisKeyManager {

    /**
     * Global key prefix for namespace isolation.
     * All SoukConect keys will start with this prefix.
     */
    protected static final String KEY_PREFIX = "soukconect";

    /**
     * Build a Redis key from multiple parts.
     * Parts are joined with colons (:).
     * All keys are automatically prefixed with "soukconect:".
     *
     * @param parts Key components
     * @return Formatted Redis key with soukconect prefix
     */
    protected static String buildKey(Object... parts) {
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("Key parts cannot be empty");
        }

        StringBuilder key = new StringBuilder(KEY_PREFIX);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] == null) {
                throw new IllegalArgumentException("Key part at index " + i + " cannot be null");
            }

            key.append(":");
            key.append(parts[i]);
        }

        return key.toString();
    }

    /**
     * Build a pattern key for scanning/matching.
     * Use "*" as wildcard.
     *
     * @param parts Key components (can include wildcards)
     * @return Pattern key for SCAN/KEYS commands
     */
    protected static String buildPattern(Object... parts) {
        return buildKey(parts);
    }

    /**
     * Validate that a key matches expected pattern.
     *
     * @param key The key to validate
     * @param expectedPrefix Expected prefix (without the global soukconect prefix)
     * @return true if key starts with expected prefix
     */
    protected static boolean validateKeyPrefix(String key, String expectedPrefix) {
        return key != null && key.startsWith(KEY_PREFIX + ":" + expectedPrefix + ":");
    }

    /**
     * Get the global key prefix.
     *
     * @return The global key prefix (soukconect)
     */
    public static String getKeyPrefix() {
        return KEY_PREFIX;
    }

    /**
     * Build a pattern to match all keys for this application.
     *
     * @return Pattern to match all soukconect keys (soukconect:*)
     */
    public static String getAllKeysPattern() {
        return KEY_PREFIX + ":*";
    }
}
