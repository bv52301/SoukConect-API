package com.souk.auth.api.controller;

import com.souk.auth.api.dto.*;
import com.souk.auth.security.UserPrincipal;
import com.souk.auth.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/password")
@Tag(name = "Password", description = "Password management endpoints")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @PostMapping("/reset-request")
    @Operation(summary = "Request password reset", description = "Send password reset email to user")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        // Always return success to prevent email enumeration
        passwordService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists with this email, a password reset link has been sent."));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password", description = "Set new password using reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        boolean success = passwordService.resetPassword(request.getToken(), request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired reset token"));
        }
    }

    @PostMapping("/change")
    @Operation(summary = "Change password", description = "Change password for authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PasswordChangeRequest request) {
        boolean success = passwordService.changePassword(
                principal.getUserId(),
                request.getCurrentPassword(),
                request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Current password is incorrect"));
        }
    }
}
