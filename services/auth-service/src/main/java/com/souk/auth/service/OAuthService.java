package com.souk.auth.service;

import com.souk.auth.api.dto.TokenResponse;
import com.souk.auth.config.OAuthProperties;
import com.souk.common.adapters.jpa.repository.UserRepository;
import com.souk.common.domain.User;
import com.souk.common.domain.UserOAuthConnection;
import com.souk.common.domain.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final OAuthProperties oAuthProperties;

    public OAuthService(UserRepository userRepository,
                       JwtService jwtService,
                       SessionService sessionService,
                       OAuthProperties oAuthProperties) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.oAuthProperties = oAuthProperties;
    }

    @Transactional
    public TokenResponse processOAuthLogin(String provider, String providerUserId, String email,
                                           String profilePictureUrl, String deviceInfo, String ipAddress) {
        UserOAuthConnection.OAuthProvider oauthProvider = UserOAuthConnection.OAuthProvider.valueOf(provider.toUpperCase());

        // Try to find existing user by OAuth connection
        Optional<User> existingUserOpt = findUserByOAuthConnection(oauthProvider, providerUserId);

        User user;
        if (existingUserOpt.isPresent()) {
            // Existing OAuth user - update last login
            user = existingUserOpt.get();
            updateOAuthConnection(user, oauthProvider, providerUserId);
        } else {
            // Check if user exists with same email
            Optional<User> userByEmail = userRepository.findByEmail(email);

            if (userByEmail.isPresent()) {
                // Link OAuth to existing account
                user = userByEmail.get();
                linkOAuthToUser(user, oauthProvider, providerUserId, email);
            } else {
                // Create new user via OAuth
                user = createOAuthUser(email, profilePictureUrl, oauthProvider, providerUserId);
            }
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        user.setLastLoginMethod(providerToLoginMethod(oauthProvider));
        userRepository.save(user);

        // Generate tokens (no MFA required for OAuth as per design)
        return generateTokenResponse(user, deviceInfo, ipAddress);
    }

    private Optional<User> findUserByOAuthConnection(UserOAuthConnection.OAuthProvider provider, String providerUserId) {
        // Find user through their OAuth connections
        return userRepository.findAll().stream()
                .filter(u -> u.getOauthConnections().stream()
                        .anyMatch(conn -> conn.getProvider() == provider
                                && conn.getProviderUserId().equals(providerUserId)))
                .findFirst();
    }

    private void updateOAuthConnection(User user, UserOAuthConnection.OAuthProvider provider, String providerUserId) {
        user.getOauthConnections().stream()
                .filter(conn -> conn.getProvider() == provider && conn.getProviderUserId().equals(providerUserId))
                .findFirst()
                .ifPresent(conn -> conn.setLastUsed(LocalDateTime.now()));
    }

    private void linkOAuthToUser(User user, UserOAuthConnection.OAuthProvider provider,
                                 String providerUserId, String email) {
        UserOAuthConnection connection = new UserOAuthConnection();
        connection.setProvider(provider);
        connection.setProviderUserId(providerUserId);
        connection.setProviderEmail(email);
        // connectedAt is auto-set by database (DEFAULT CURRENT_TIMESTAMP)
        connection.setLastUsed(LocalDateTime.now());

        user.addOAuthConnection(connection);

        log.info("Linked {} OAuth to existing user: {}", provider, user.getUserId());
    }

    @Transactional
    protected User createOAuthUser(String email, String profilePictureUrl,
                                   UserOAuthConnection.OAuthProvider provider, String providerUserId) {
        User user = new User();
        user.setEmail(email);
        user.setEmailVerified(true); // OAuth emails are pre-verified
        user.setProfilePictureUrl(profilePictureUrl);

        // Add default CUSTOMER role
        UserRole customerRole = new UserRole();
        customerRole.setRole(UserRole.Role.CUSTOMER);
        user.addRole(customerRole);

        // Create OAuth connection
        UserOAuthConnection connection = new UserOAuthConnection();
        connection.setProvider(provider);
        connection.setProviderUserId(providerUserId);
        connection.setProviderEmail(email);
        // connectedAt is auto-set by database (DEFAULT CURRENT_TIMESTAMP)
        connection.setLastUsed(LocalDateTime.now());
        user.addOAuthConnection(connection);

        user = userRepository.save(user);

        log.info("Created new user via {} OAuth: {}", provider, user.getUserId());
        return user;
    }

    private TokenResponse generateTokenResponse(User user, String deviceInfo, String ipAddress) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getEmail(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getUserId());

        // Create session
        sessionService.createSession(user.getUserId(), refreshToken, deviceInfo, ipAddress);

        TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setEmail(user.getEmail());
        userInfo.setRoles(roles);
        userInfo.setEmailVerified(user.getEmailVerified());
        userInfo.setMfaEnabled(user.getMfaEnabled());
        userInfo.setProfilePictureUrl(user.getProfilePictureUrl());

        return TokenResponse.success(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                userInfo
        );
    }

    private User.LoginMethod providerToLoginMethod(UserOAuthConnection.OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> User.LoginMethod.GOOGLE;
            case APPLE -> User.LoginMethod.APPLE;
            case FACEBOOK -> User.LoginMethod.FACEBOOK;
        };
    }

    public boolean isProviderEnabled(String provider) {
        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> oAuthProperties.getGoogle().isEnabled();
            case "APPLE" -> oAuthProperties.getApple().isEnabled();
            case "FACEBOOK" -> oAuthProperties.getFacebook().isEnabled();
            default -> false;
        };
    }
}
