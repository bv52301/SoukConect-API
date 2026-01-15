package com.souk.auth.api.controller;

import com.souk.auth.api.dto.*;
import com.souk.auth.security.UserPrincipal;
import com.souk.auth.service.AuthenticationService;
import com.souk.auth.service.AuthenticationService.AuthenticationException;
import com.souk.auth.service.JwtService;
import com.souk.auth.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@Tag(name = "MFA", description = "Multi-factor authentication endpoints")
public class MfaController {

    private final MfaService mfaService;
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public MfaController(MfaService mfaService,
                        JwtService jwtService,
                        AuthenticationService authenticationService) {
        this.mfaService = mfaService;
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/setup")
    @Operation(summary = "Setup MFA", description = "Generate MFA secret and QR code for setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(
            @AuthenticationPrincipal UserPrincipal principal) {
        MfaSetupResponse response = mfaService.generateSetup(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Scan the QR code with your authenticator app", response));
    }

    @PostMapping("/enable")
    @Operation(summary = "Enable MFA", description = "Enable MFA after verifying the setup code")
    public ResponseEntity<ApiResponse<Void>> enableMfa(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String secret,
            @Valid @RequestBody MfaVerifyRequest request) {

        boolean success = mfaService.enableMfa(principal.getUserId(), secret, request.getCode());

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("MFA enabled successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid verification code"));
        }
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable MFA", description = "Disable MFA for the current user")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MfaVerifyRequest request) {

        // Don't allow Admin/Support to disable MFA
        if (principal.isAdmin() || principal.isSupport()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Admin and Support users cannot disable MFA"));
        }

        boolean success = mfaService.disableMfa(principal.getUserId(), request.getCode());

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("MFA disabled successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid verification code"));
        }
    }

    @PostMapping("/verify-login")
    @Operation(summary = "Verify MFA for login", description = "Complete login by verifying MFA code")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyMfaLogin(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        try {
            String mfaToken = request.getMfaToken();
            if (mfaToken == null || mfaToken.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("MFA token is required"));
            }

            // Find user ID from MFA token
            Long userId = jwtService.getUserIdFromMfaToken(mfaToken);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or expired MFA token"));
            }

            String deviceInfo = httpRequest.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(httpRequest);

            TokenResponse response = authenticationService.verifyMfaAndLogin(
                    userId, request.getCode(), deviceInfo, ipAddress);

            return ResponseEntity.ok(ApiResponse.success("MFA verified, login successful", response));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
