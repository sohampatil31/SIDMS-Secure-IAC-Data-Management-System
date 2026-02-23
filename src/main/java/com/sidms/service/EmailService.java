package com.sidms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails (OTP codes, verification links, etc.).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@sidms.com}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send the OTP code to the user's email address.
     *
     * @param toEmail   recipient email
     * @param otpCode   the one-time password
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("SIDMS — Your Login OTP");
        message.setText("""
                Your one-time password (OTP) for SIDMS login is:

                    %s

                This code expires in 5 minutes.
                Do not share this code with anyone.

                — SIDMS Security Team
                """.formatted(otpCode));

        try {
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    /**
     * Send a verification link email.
     *
     * @param toEmail          recipient email
     * @param verificationLink the full verification URL
     */
    public void sendVerificationEmail(String toEmail, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("SIDMS — Verify Your Email");
        message.setText("""
                Welcome to SIDMS!

                Please verify your email by clicking the link below:

                %s

                This link expires in 24 hours.

                — SIDMS Security Team
                """.formatted(verificationLink));

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }
}
