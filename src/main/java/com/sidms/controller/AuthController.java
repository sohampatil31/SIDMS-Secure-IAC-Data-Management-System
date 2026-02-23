package com.sidms.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sidms.dto.AuthResponse;
import com.sidms.dto.LoginRequest;
import com.sidms.dto.OtpVerifyRequest;
import com.sidms.dto.RegisterRequest;
import com.sidms.service.AuthService;
import com.sidms.service.VerificationTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Authentication endpoints — registration, login, OTP verification, and email verification.
 * These endpoints are publicly accessible (no JWT required).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration + email verification + two-step login (OTP → JWT)")
public class AuthController {

    private final AuthService authService;
    private final VerificationTokenService verificationTokenService;

    public AuthController(AuthService authService,
                          VerificationTokenService verificationTokenService) {
        this.authService = authService;
        this.verificationTokenService = verificationTokenService;
    }

    /* ── Registration ─────────────────────────────── */

    /**
     * Register a new user. Account is disabled until email is verified.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Creates user with enabled=false. Prints verification link to console (simulates email).")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful. Please check your email to verify your account."
        ));
    }

    /**
     * Verify email address using the token from the verification link.
     */
    @GetMapping("/verify")
    @Operation(summary = "Verify email",
               description = "Validates the token, enables the user account, and marks the token as used.")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam String token) {

        verificationTokenService.verifyToken(token);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully. You can now log in."
        ));
    }

    /* ── Login (existing) ─────────────────────────── */

    /**
     * STEP 1: Submit username + password.
     * On success, generates an OTP (returned in response for testing;
     * in production, it would be sent via email/SMS).
     */
    @PostMapping("/login")
    @Operation(summary = "Step 1: Login with credentials",
               description = "Validates username and password. Returns OTP on success. No JWT is issued.")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        authService.login(request, httpRequest);

        return ResponseEntity.ok(Map.of(
                "message", "Credentials verified. OTP has been sent via secure channel."
        ));
    }

    /**
     * STEP 2: Submit OTP to receive JWT token.
     */
    @PostMapping("/verify-otp")
    @Operation(summary = "Step 2: Verify OTP",
               description = "Validates the OTP. On success, issues a JWT token with role claims.")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.verifyOtpAndIssueToken(request, httpRequest);
        return ResponseEntity.ok(response);
    }
}
