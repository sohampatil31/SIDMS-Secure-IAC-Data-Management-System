package com.sidms.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sidms.entity.User;
import com.sidms.entity.VerificationToken;
import com.sidms.exception.BadRequestException;
import com.sidms.exception.ResourceNotFoundException;
import com.sidms.repository.UserRepository;
import com.sidms.repository.VerificationTokenRepository;

/**
 * Handles creation and verification of email verification tokens.
 */
@Service
public class VerificationTokenService {

    private static final Logger log = LoggerFactory.getLogger(VerificationTokenService.class);

    /** Token validity duration in hours. */
    private static final int TOKEN_EXPIRY_HOURS = 24;

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public VerificationTokenService(VerificationTokenRepository tokenRepository,
                                     UserRepository userRepository,
                                     EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Generate a verification token for the given user and print the
     * verification link to the console (simulating an email).
     *
     * @param user the newly registered user
     * @return the generated token string
     */
    public String createToken(User user) {
        String tokenValue = UUID.randomUUID().toString();

        VerificationToken token = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .used(false)
                .build();

        tokenRepository.save(token);

        // Build verification link (points to frontend, which calls backend API)
        String verificationLink = "http://localhost:5173/verify?token=" + tokenValue;

        // Send the actual verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationLink);

        log.info("Verification email sent to '{}' for user '{}'", user.getEmail(), user.getUsername());

        return tokenValue;
    }

    /**
     * Verify the token: check it exists, is not expired, and is not used.
     * On success, enable the user and mark the token as used.
     *
     * @param tokenValue the UUID token from the verification link
     */
    @Transactional
    public void verifyToken(String tokenValue) {
        VerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (token.isUsed()) {
            throw new BadRequestException("This verification link has already been used");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification link has expired. Please register again.");
        }

        // Enable the user account
        User user = token.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);

        log.info("User '{}' email verified and account enabled", user.getUsername());
    }
}
