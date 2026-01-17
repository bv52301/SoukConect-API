package com.souk.combined.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates JWT tokens for the combined-service.
 * This is a simplified validator that checks signature and expiration only.
 * It uses the same secret key as auth-service for token validation.
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public JwtTokenValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        }
        return secretKey;
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token has expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        List<String> rolesList = claims.get("roles", List.class);
        return rolesList != null ? new HashSet<>(rolesList) : new HashSet<>();
    }
}
