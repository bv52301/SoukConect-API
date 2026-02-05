package com.souk.auth.api.controller;

import com.souk.auth.api.dto.ApiResponse;
import com.souk.auth.api.dto.TokenResponse;
import com.souk.auth.config.OAuthProperties;
import com.souk.auth.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth/oauth")
@Tag(name = "OAuth", description = "Social login endpoints")
public class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    private final OAuthService oAuthService;
    private final OAuthProperties oAuthProperties;

    public OAuthController(OAuthService oAuthService, OAuthProperties oAuthProperties) {
        this.oAuthService = oAuthService;
        this.oAuthProperties = oAuthProperties;
    }

    @PostMapping("/{provider}/callback")
    @Operation(summary = "OAuth callback", description = "Handle OAuth provider callback with user info")
    public ResponseEntity<ApiResponse<TokenResponse>> handleOAuthCallback(
            @PathVariable String provider,
            @RequestBody Map<String, String> oauthData,
            HttpServletRequest httpRequest) {

        String providerUpper = provider.toUpperCase();

        // Check if provider is enabled
        if (!oAuthService.isProviderEnabled(providerUpper)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(provider + " login is not enabled"));
        }

        // Extract OAuth data from the request
        String providerUserId = oauthData.get("providerUserId");
        String email = oauthData.get("email");
        String profilePictureUrl = oauthData.get("profilePictureUrl");

        if (providerUserId == null || email == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Missing required OAuth data"));
        }

        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        try {
            TokenResponse response = oAuthService.processOAuthLogin(
                    providerUpper,
                    providerUserId,
                    email,
                    profilePictureUrl,
                    deviceInfo,
                    ipAddress);

            return ResponseEntity.ok(ApiResponse.success("OAuth login successful", response));
        } catch (Exception e) {
            log.error("OAuth login failed for provider {}: {}", provider, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OAuth login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/providers")
    @Operation(summary = "Get enabled OAuth providers", description = "Returns list of enabled OAuth providers")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getEnabledProviders() {
        Map<String, Boolean> providers = Map.of(
                "google", oAuthProperties.getGoogle().isEnabled(),
                "apple", oAuthProperties.getApple().isEnabled(),
                "facebook", oAuthProperties.getFacebook().isEnabled());

        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
