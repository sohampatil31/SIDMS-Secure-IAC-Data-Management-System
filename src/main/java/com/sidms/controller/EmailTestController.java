package com.sidms.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary controller for testing SMTP email configuration.
 * Hit GET /api/test-email to send a test email to the configured address.
 * DELETE THIS CONTROLLER once email is confirmed working.
 */
@RestController
public class EmailTestController {

    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@sidms.com}")
    private String mailUsername;

    public EmailTestController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @GetMapping("/api/test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail() {
        log.info("===== EMAIL TEST START =====");
        log.info("SMTP username (from): {}", mailUsername);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(mailUsername);  // send to self
        message.setSubject("SIDMS Email Test");
        message.setText("This is a test email from SIDMS.");

        log.info("Attempting to send test email to: {}", mailUsername);

        try {
            mailSender.send(message);
            log.info("✅ Test email sent successfully to: {}", mailUsername);
            log.info("===== EMAIL TEST SUCCESS =====");
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Test email sent successfully to " + mailUsername
            ));
        } catch (MailException e) {
            log.error("❌ Failed to send test email: {}", e.getMessage());
            log.error("Full stack trace:", e);
            log.error("===== EMAIL TEST FAILED =====");
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAILED",
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown mail error"
            ));
        }
    }
}
