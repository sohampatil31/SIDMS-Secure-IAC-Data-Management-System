package com.sidms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sidms.dto.AuthResponse;
import com.sidms.dto.LoginRequest;
import com.sidms.dto.OtpVerifyRequest;
import com.sidms.dto.RegisterRequest;
import com.sidms.entity.Role;
import com.sidms.entity.User;
import com.sidms.exception.BadRequestException;
import com.sidms.exception.EmailNotVerifiedException;
import com.sidms.exception.UnauthorizedException;
import com.sidms.repository.UserRepository;
import com.sidms.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Handles registration and two-step authentication flow:
 * <ol>
 *   <li>Registration: save user (disabled) → send verification email</li>
 *   <li>Step 1: Validate username + password → generate OTP → send via email</li>
 *   <li>Step 2: Validate OTP → issue JWT token</li>
 * </ol>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       JwtService jwtService,
                       AuditLogService auditLogService,
                       VerificationTokenService verificationTokenService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
    }

    /**
     * Register a new user with enabled = false.
     * Generates a verification token and prints the link to console.
     */
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Role.ROLE_MEMBER)
                .enabled(false)
                .build();

        userRepository.save(user);

        verificationTokenService.createToken(user);

        log.info("User '{}' registered. Awaiting email verification.", user.getUsername());
    }

    /**
     * STEP 1: Validates credentials, generates an OTP, and sends it to the user's email.
     * The OTP is NOT returned in the API response and NOT logged to console.
     *
     * @param request     the login request (username + password)
     * @param httpRequest the HTTP request (for audit logging IP)
     */
    public void login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditLogService.log(request.getUsername(), "LOGIN",
                            "FAILURE", httpRequest, "User not found");
                    return new UnauthorizedException("Invalid username or password");
                });

        if (!user.isEnabled()) {
            auditLogService.log(request.getUsername(), "LOGIN",
                    "FAILURE", httpRequest, "Email not verified");
            throw new EmailNotVerifiedException("Email not verified. Please verify your email before logging in.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.log(request.getUsername(), "LOGIN",
                    "FAILURE", httpRequest, "Invalid password");
            throw new UnauthorizedException("Invalid username or password");
        }

        // Credentials valid → generate OTP (returned raw for email delivery only)
        String otpCode = otpService.generateOtp(user.getId());

        // Send OTP via email — do NOT return it in the response
        emailService.sendOtpEmail(user.getEmail(), otpCode);

        auditLogService.log(request.getUsername(), "LOGIN",
                "SUCCESS", httpRequest, "OTP sent to registered email");

        log.info("Step 1 complete for '{}'. OTP sent via email.", user.getUsername());
    }

    /**
     * STEP 2: Verifies OTP and issues a JWT token.
     *
     * @param request     the OTP verification request
     * @param httpRequest the HTTP request (for audit logging IP)
     * @return AuthResponse containing the JWT token and metadata
     */
    public AuthResponse verifyOtpAndIssueToken(OtpVerifyRequest request,
                                                HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditLogService.log(request.getUsername(), "OTP_VERIFY",
                            "FAILURE", httpRequest, "User not found");
                    return new UnauthorizedException("Invalid username");
                });

        boolean valid = otpService.verifyOtp(user.getId(), request.getOtp());

        if (!valid) {
            auditLogService.log(request.getUsername(), "OTP_VERIFY",
                    "FAILURE", httpRequest, "Invalid or expired OTP");
            throw new BadRequestException("Invalid or expired OTP");
        }

        // OTP valid → issue JWT
        String token = jwtService.generateToken(
                user.getUsername(), user.getRole().name());

        auditLogService.log(request.getUsername(), "OTP_VERIFY",
                "SUCCESS", httpRequest, "JWT issued");

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .username(user.getUsername())
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .build();
    }
}
