package com.souk.auth.service;

import com.souk.auth.api.dto.*;
import com.souk.common.adapters.jpa.repository.CustomerRepository;
import com.souk.common.adapters.jpa.repository.UserRepository;
import com.souk.common.adapters.jpa.repository.VendorRepository;
import com.souk.common.adapters.redis.keys.AuthKeys;
import com.souk.common.adapters.redis.config.RedisTTL;
import com.souk.common.domain.Customer;
import com.souk.common.domain.CustomerAddress;
import com.souk.common.domain.User;
import com.souk.common.domain.UserRole;
import com.souk.common.domain.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final MfaService mfaService;
    private final SessionService sessionService;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthenticationService(UserRepository userRepository,
                                 CustomerRepository customerRepository,
                                 VendorRepository vendorRepository,
                                 JwtService jwtService,
                                 PasswordService passwordService,
                                 MfaService mfaService,
                                 SessionService sessionService,
                                 EmailService emailService,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.vendorRepository = vendorRepository;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
        this.mfaService = mfaService;
        this.sessionService = sessionService;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        String email = request.getEmail().toLowerCase().trim();

        // Check rate limiting
        if (sessionService.isAccountLocked(email, MAX_FAILED_ATTEMPTS)) {
            log.warn("Login blocked due to too many failed attempts: {}", email);
            throw new AuthenticationException("Too many failed login attempts. Please try again later.");
        }

        // Find user
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            sessionService.recordFailedLoginAttempt(email);
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userOpt.get();

        // Check if account is locked in DB
        if (user.getAccountLocked() && user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AuthenticationException("Account is locked. Please try again later.");
        }

        // Verify password
        if (user.getPasswordHash() == null) {
            // User registered via OAuth only
            throw new AuthenticationException("Please login using your social account (Google, Apple, or Facebook)");
        }

        if (!passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, email);
            throw new AuthenticationException("Invalid email or password");
        }

        // Check if email is verified
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AuthenticationException("Please verify your email before logging in. Check your inbox for the verification link.");
        }

        // Password verified - clear failed attempts
        sessionService.clearFailedLoginAttempts(email);
        resetFailedAttempts(user);

        // Check MFA requirement
        if (requiresMfa(user)) {
            if (user.getMfaEnabled() && user.getMfaSecret() != null) {
                // MFA is set up - require verification
                String mfaToken = jwtService.generateMfaToken(user.getUserId());
                log.info("MFA required for user: {}", user.getUserId());
                return TokenResponse.mfaRequired(mfaToken);
            } else {
                // Admin/Support without MFA - force them to set it up
                // For now, allow login but flag that MFA should be enabled
                log.warn("Admin/Support user {} logged in without MFA enabled", user.getUserId());
            }
        }

        // Generate tokens and complete login
        return completeLogin(user, deviceInfo, ipAddress);
    }

    @Transactional
    public TokenResponse verifyMfaAndLogin(Long userId, String code, String deviceInfo, String ipAddress) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        User user = userOpt.get();

        if (!mfaService.verifyCodeForUser(userId, code)) {
            throw new AuthenticationException("Invalid MFA code");
        }

        // Clear MFA token
        jwtService.clearMfaToken(userId);

        return completeLogin(user, deviceInfo, ipAddress);
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new AuthenticationException("Email is already registered");
        }

        // Validate role (only CUSTOMER and VENDOR can self-register)
        if (request.getRole() == UserRole.Role.ADMIN || request.getRole() == UserRole.Role.SUPPORT) {
            throw new AuthenticationException("Cannot self-register as Admin or Support");
        }

        // Validate role-specific required fields
        validateRoleSpecificFields(request);

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordService.hashPassword(request.getPassword()));
        user.setEmailVerified(false);

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));

        // Add role
        UserRole userRole = new UserRole();
        userRole.setRole(request.getRole());
        user.addRole(userRole);

        user = userRepository.save(user);

        // Create role-specific profile
        if (request.getRole() == UserRole.Role.CUSTOMER) {
            createCustomerProfile(user, request, email);
        } else if (request.getRole() == UserRole.Role.VENDOR) {
            createVendorProfile(user, request, email);
        }

        // Store verification token in Redis for quick lookup
        String redisKey = AuthKeys.emailVerification(verificationToken);
        redisTemplate.opsForValue().set(redisKey, user.getUserId(),
                RedisTTL.EMAIL_VERIFICATION.getSeconds(), TimeUnit.SECONDS);

        // Send verification email
        emailService.sendVerificationEmail(email, verificationToken);

        log.info("User registered: {} with role {}", user.getUserId(), request.getRole());
        return user;
    }

    private void validateRoleSpecificFields(RegisterRequest request) {
        if (request.getRole() == UserRole.Role.CUSTOMER) {
            if (isBlank(request.getFirstName())) {
                throw new AuthenticationException("First name is required for customer registration");
            }
            if (isBlank(request.getLastName())) {
                throw new AuthenticationException("Last name is required for customer registration");
            }
        } else if (request.getRole() == UserRole.Role.VENDOR) {
            if (isBlank(request.getBusinessName())) {
                throw new AuthenticationException("Business name is required for vendor registration");
            }
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void createCustomerProfile(User user, RegisterRequest request, String email) {
        Customer customer = new Customer();
        customer.setUserId(user.getUserId());
        customer.setFirstName(request.getFirstName().trim());
        customer.setLastName(request.getLastName().trim());
        customer.setEmail(email);
        customer.setPhone(request.getPhone());

        // Add address if provided
        if (!isBlank(request.getStreet()) || !isBlank(request.getCity())) {
            CustomerAddress address = new CustomerAddress();
            address.setCustomer(customer);
            address.setStreet(request.getStreet());
            address.setUnit(request.getUnit());
            address.setCity(request.getCity());
            address.setPostal(request.getPostal());
            address.setCountry(request.getCountry());
            address.setType(CustomerAddress.AddressType.HOME);
            address.setIsDefault(true);
            customer.getAddresses().add(address);
        }

        customerRepository.save(customer);
        log.info("Customer profile created for user: {}", user.getUserId());
    }

    private void createVendorProfile(User user, RegisterRequest request, String email) {
        Vendor vendor = new Vendor();
        vendor.setUserId(user.getUserId());
        vendor.setName(request.getBusinessName().trim());
        vendor.setDescription(request.getBusinessDescription());
        vendor.setContactName(request.getContactName());
        vendor.setEmail(email);
        vendor.setPhoneNumber(request.getPhone());
        vendor.setAddress1(request.getAddress1());
        vendor.setAddress2(request.getAddress2());
        vendor.setState(request.getState());
        vendor.setLandmark(request.getLandmark());
        vendor.setPincode(request.getPincode());
        if (request.getLatitude() != null) {
            vendor.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            vendor.setLongitude(request.getLongitude());
        }
        vendorRepository.save(vendor);
        log.info("Vendor profile created for user: {}", user.getUserId());
    }

    @Transactional
    public boolean verifyEmail(String token) {
        String redisKey = AuthKeys.emailVerification(token);
        Object userIdObj = redisTemplate.opsForValue().get(redisKey);

        Long userId = null;
        if (userIdObj instanceof Integer) {
            userId = ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            userId = (Long) userIdObj;
        }

        if (userId == null) {
            // Check DB fallback
            Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);
            if (userOpt.isEmpty()) {
                return false;
            }
            User user = userOpt.get();
            if (user.getEmailVerificationExpiry() != null
                    && user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
                return false;
            }
            userId = user.getUserId();
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);

        // Delete Redis key
        redisTemplate.delete(redisKey);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), null);

        log.info("Email verified for user: {}", userId);
        return true;
    }

    public TokenResponse refreshToken(String refreshTokenUuid, String deviceInfo, String ipAddress) {
        Long userId = jwtService.validateRefreshToken(refreshTokenUuid);

        if (userId == null) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        User user = userOpt.get();

        // Revoke old refresh token
        jwtService.revokeRefreshToken(refreshTokenUuid);
        sessionService.removeSession(userId, refreshTokenUuid);

        // Generate new tokens
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.toSet());

        String newAccessToken = jwtService.generateAccessToken(user.getUserId(), user.getEmail(), roles);
        String newRefreshToken = jwtService.generateRefreshToken(user.getUserId());

        // Create new session
        sessionService.createSession(user.getUserId(), newRefreshToken, deviceInfo, ipAddress);

        TokenResponse.UserInfo userInfo = buildUserInfo(user, roles);

        return TokenResponse.success(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                userInfo
        );
    }

    @Transactional
    public void logout(String accessToken, String refreshTokenUuid) {
        // Blacklist access token
        if (accessToken != null) {
            jwtService.blacklistAccessToken(accessToken);
        }

        // Revoke refresh token
        if (refreshTokenUuid != null) {
            Long userId = jwtService.validateRefreshToken(refreshTokenUuid);
            if (userId != null) {
                jwtService.revokeRefreshToken(refreshTokenUuid);
                sessionService.removeSession(userId, refreshTokenUuid);
            }
        }

        log.debug("User logged out");
    }

    @Transactional
    public void logoutAllSessions(Long userId) {
        // Get all sessions
        List<Map<String, Object>> sessions = sessionService.getUserSessions(userId);

        // Revoke all refresh tokens
        for (Map<String, Object> session : sessions) {
            String refreshToken = (String) session.get("refreshToken");
            if (refreshToken != null) {
                jwtService.revokeRefreshToken(refreshToken);
            }
        }

        // Clear all sessions
        sessionService.removeAllSessions(userId);

        log.info("All sessions logged out for user: {}", userId);
    }

    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
    }

    private TokenResponse completeLogin(User user, String deviceInfo, String ipAddress) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getEmail(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getUserId());

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        user.setLastLoginMethod(User.LoginMethod.PASSWORD);
        userRepository.save(user);

        // Create session
        sessionService.createSession(user.getUserId(), refreshToken, deviceInfo, ipAddress);

        TokenResponse.UserInfo userInfo = buildUserInfo(user, roles);

        log.info("User logged in: {}", user.getUserId());

        return TokenResponse.success(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                userInfo
        );
    }

    private TokenResponse.UserInfo buildUserInfo(User user, Set<String> roles) {
        TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setEmail(user.getEmail());
        userInfo.setRoles(roles);
        userInfo.setEmailVerified(user.getEmailVerified());
        userInfo.setMfaEnabled(user.getMfaEnabled());
        userInfo.setProfilePictureUrl(user.getProfilePictureUrl());
        return userInfo;
    }

    private boolean requiresMfa(User user) {
        // MFA is mandatory for ADMIN and SUPPORT roles
        return user.getRoles().stream()
                .anyMatch(r -> r.getRole() == UserRole.Role.ADMIN || r.getRole() == UserRole.Role.SUPPORT);
    }

    private void handleFailedLogin(User user, String email) {
        sessionService.recordFailedLoginAttempt(email);

        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            log.warn("Account locked due to failed attempts: {}", user.getUserId());
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getAccountLocked()) {
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
