package com.souk.auth.service;

import com.souk.common.adapters.jpa.repository.UserRepository;
import com.souk.common.adapters.redis.keys.AuthKeys;
import com.souk.common.adapters.redis.config.RedisTTL;
import com.souk.common.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    public PasswordService(PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          RedisTemplate<String, Object> redisTemplate,
                          EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Don't reveal if user exists or not
            log.debug("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();

        // Store token in Redis
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("userId", user.getUserId());
        tokenData.put("email", user.getEmail());
        tokenData.put("createdAt", System.currentTimeMillis());

        String redisKey = AuthKeys.passwordReset(resetToken);
        redisTemplate.opsForHash().putAll(redisKey, tokenData);
        redisTemplate.expire(redisKey, RedisTTL.PASSWORD_RESET.getSeconds(), TimeUnit.SECONDS);

        // Also store in DB for tracking
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.info("Password reset initiated for user: {}", user.getUserId());
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        String redisKey = AuthKeys.passwordReset(token);
        Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(redisKey);

        if (tokenData.isEmpty()) {
            log.debug("Invalid or expired password reset token");
            return false;
        }

        Object userIdObj = tokenData.get("userId");
        Long userId;
        if (userIdObj instanceof Integer) {
            userId = ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            userId = (Long) userIdObj;
        } else {
            return false;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Update password
        user.setPasswordHash(hashPassword(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        // Delete Redis token
        redisTemplate.delete(redisKey);

        log.info("Password reset completed for user: {}", userId);
        return true;
    }

    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Verify current password
        if (!verifyPassword(currentPassword, user.getPasswordHash())) {
            log.debug("Current password verification failed for user: {}", userId);
            return false;
        }

        // Update password
        user.setPasswordHash(hashPassword(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
        return true;
    }
}
