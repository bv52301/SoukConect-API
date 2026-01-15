package com.souk.auth.service;

import com.souk.auth.config.JwtProperties;
import com.souk.common.adapters.redis.keys.AuthKeys;
import com.souk.common.adapters.redis.config.RedisTTL;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties, RedisTemplate<String, Object> redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
    }

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        }
        return secretKey;
    }

    public String generateAccessToken(Long userId, String email, Set<String> roles) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        String tokenUuid = UUID.randomUUID().toString();

        // Store refresh token data in Redis
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("userId", userId);
        tokenData.put("createdAt", System.currentTimeMillis());

        String redisKey = AuthKeys.refreshToken(tokenUuid);
        redisTemplate.opsForHash().putAll(redisKey, tokenData);
        redisTemplate.expire(redisKey, RedisTTL.REFRESH_TOKEN.getSeconds(), TimeUnit.SECONDS);

        return tokenUuid;
    }

    public String generateMfaToken(Long userId) {
        String tokenUuid = UUID.randomUUID().toString();

        // Store MFA pending token in Redis (short-lived, 5 minutes)
        // Store both directions: token -> userId and userId -> token
        String redisKey = AuthKeys.mfaCode(userId);
        redisTemplate.opsForValue().set(redisKey, tokenUuid, RedisTTL.MFA_CODE.getSeconds(), TimeUnit.SECONDS);

        // Also store reverse mapping: mfaToken -> userId
        String reverseKey = "soukconect:auth:mfa:pending:" + tokenUuid;
        redisTemplate.opsForValue().set(reverseKey, userId, RedisTTL.MFA_CODE.getSeconds(), TimeUnit.SECONDS);

        return tokenUuid;
    }

    public boolean validateMfaToken(Long userId, String mfaToken) {
        String redisKey = AuthKeys.mfaCode(userId);
        Object storedToken = redisTemplate.opsForValue().get(redisKey);
        return storedToken != null && storedToken.equals(mfaToken);
    }

    public Long getUserIdFromMfaToken(String mfaToken) {
        String reverseKey = "soukconect:auth:mfa:pending:" + mfaToken;
        Object userIdObj = redisTemplate.opsForValue().get(reverseKey);

        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        return null;
    }

    public void clearMfaToken(Long userId) {
        // Get the token first to delete reverse mapping
        String redisKey = AuthKeys.mfaCode(userId);
        Object token = redisTemplate.opsForValue().get(redisKey);

        if (token != null) {
            String reverseKey = "soukconect:auth:mfa:pending:" + token;
            redisTemplate.delete(reverseKey);
        }

        redisTemplate.delete(redisKey);
    }

    public Claims parseAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.debug("Invalid token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseAccessToken(token);
            String jti = claims.getId();

            // Check if token is blacklisted
            if (isTokenBlacklisted(jti)) {
                return false;
            }

            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseAccessToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseAccessToken(token);
        return claims.get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = parseAccessToken(token);
        List<String> rolesList = claims.get("roles", List.class);
        return new HashSet<>(rolesList);
    }

    public Long validateRefreshToken(String refreshTokenUuid) {
        String redisKey = AuthKeys.refreshToken(refreshTokenUuid);
        Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(redisKey);

        if (tokenData.isEmpty()) {
            return null;
        }

        Object userId = tokenData.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    public void revokeRefreshToken(String refreshTokenUuid) {
        redisTemplate.delete(AuthKeys.refreshToken(refreshTokenUuid));
    }

    public void blacklistAccessToken(String token) {
        try {
            Claims claims = parseAccessToken(token);
            String jti = claims.getId();
            Date expiry = claims.getExpiration();

            // Calculate remaining TTL
            long remainingMs = expiry.getTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                String blacklistKey = AuthKeys.accessTokenBlacklist(jti);
                redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingMs, TimeUnit.MILLISECONDS);
            }
        } catch (ExpiredJwtException e) {
            // Token already expired, no need to blacklist
        } catch (JwtException e) {
            log.warn("Could not blacklist invalid token: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String jti) {
        String blacklistKey = AuthKeys.accessTokenBlacklist(jti);
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpiration();
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}
