package com.souk.auth.api.controller;

import com.souk.auth.api.dto.*;
import com.souk.auth.security.UserPrincipal;
import com.souk.auth.service.AuthenticationService;
import com.souk.auth.service.AuthenticationService.AuthenticationException;
import com.souk.common.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account with email and password")
    public ResponseEntity<ApiResponse<TokenResponse.UserInfo>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            User user = authenticationService.register(request);

            TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setEmail(user.getEmail());
            userInfo.setEmailVerified(user.getEmailVerified());
            userInfo.setMfaEnabled(user.getMfaEnabled());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registration successful. Please verify your email.", userInfo));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email and password")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String deviceInfo = httpRequest.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(httpRequest);

            TokenResponse response = authenticationService.login(request, deviceInfo, ipAddress);

            if (response.isMfaRequired()) {
                return ResponseEntity.ok(ApiResponse.success("MFA verification required", response));
            }

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get a new access token using refresh token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        try {
            String deviceInfo = httpRequest.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(httpRequest);

            TokenResponse response = authenticationService.refreshToken(
                    request.getRefreshToken(), deviceInfo, ipAddress);

            return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate current session tokens")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        String refreshToken = request != null ? request.getRefreshToken() : null;

        authenticationService.logout(accessToken, refreshToken);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all sessions", description = "Invalidate all sessions for the current user")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        authenticationService.logoutAllSessions(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("All sessions logged out"));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify email address using token from email link")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        boolean verified = authenticationService.verifyEmail(token);

        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired verification token"));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    public ResponseEntity<ApiResponse<TokenResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = authenticationService.getCurrentUser(principal.getUserId());

        TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setEmail(user.getEmail());
        userInfo.setRoles(principal.getRoles());
        userInfo.setEmailVerified(user.getEmailVerified());
        userInfo.setMfaEnabled(user.getMfaEnabled());
        userInfo.setProfilePictureUrl(user.getProfilePictureUrl());

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
