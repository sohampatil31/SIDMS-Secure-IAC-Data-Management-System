package com.sidms.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sidms.entity.Otp;
import com.sidms.repository.OtpRepository;

/**
 * Service for generating and verifying one-time passwords (OTPs).
 * <ul>
 *   <li>OTPs are stored as BCrypt hashes — never in plaintext.</li>
 *   <li>OTPs expire after a configurable duration (default 5 minutes).</li>
 *   <li>OTPs are marked as verified (used) after successful validation.</li>
 * </ul>
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${sidms.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${sidms.otp.length:6}")
    private int otpLength;

    public OtpService(OtpRepository otpRepository, PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generate a new OTP for the given user.
     * The raw OTP is returned to the caller (for email delivery)
     * but only the BCrypt hash is persisted in the database.
     *
     * @param userId the user's ID
     * @return the raw OTP code (caller must send it via email, NOT log it)
     */
    @Transactional
    public String generateOtp(Long userId) {
        String rawOtp = generateNumericOtp();

        Otp otp = Otp.builder()
                .userId(userId)
                .otpCode(passwordEncoder.encode(rawOtp))
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .verified(false)
                .build();

        otpRepository.save(otp);
        log.info("OTP generated for userId={} (expires in {} min)", userId, otpExpiryMinutes);
        return rawOtp;
    }

    /**
     * Verify the OTP submitted by the user.
     * Checks: exists, not expired, not already used, hash matches.
     * On success, marks the OTP as verified (used).
     *
     * @param userId  the user's ID
     * @param rawOtp  the OTP code submitted by the user
     * @return true if the OTP is valid
     */
    @Transactional
    public boolean verifyOtp(Long userId, String rawOtp) {
        Optional<Otp> otpOpt = otpRepository
                .findTopByUserIdAndVerifiedFalseOrderByExpiryTimeDesc(userId);

        if (otpOpt.isEmpty()) {
            log.warn("No unverified OTP found for userId={}", userId);
            return false;
        }

        Otp otp = otpOpt.get();

        // Check expiry
        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for userId={}", userId);
            return false;
        }

        // Compare raw input against stored hash
        if (!passwordEncoder.matches(rawOtp, otp.getOtpCode())) {
            log.warn("Invalid OTP submitted for userId={}", userId);
            return false;
        }

        // Mark as verified (used)
        otp.setVerified(true);
        otpRepository.save(otp);
        log.info("OTP verified successfully for userId={}", userId);
        return true;
    }

    /**
     * Generate a numeric OTP of the configured length.
     */
    private String generateNumericOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }
}
