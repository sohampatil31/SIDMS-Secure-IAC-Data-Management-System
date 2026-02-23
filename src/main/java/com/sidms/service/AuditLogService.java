package com.sidms.service;

import com.sidms.entity.AuditLog;
import com.sidms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for recording audit log entries for all security-relevant events.
 * All entries are persisted to the audit_logs table.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an audit event.
     *
     * @param username  the acting user (can be null for unauthenticated attempts)
     * @param action    descriptive action string
     * @param status    outcome (SUCCESS, FAILURE, DENIED)
     * @param ipAddress client IP address
     * @param details   optional extra context
     */
    public void log(String username, String action, String status,
                    String ipAddress, String details) {
        AuditLog entry = AuditLog.builder()
                .username(username)
                .action(action)
                .status(status)
                .ipAddress(ipAddress)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
    }

    /**
     * Convenience overload that extracts the IP from the HTTP request.
     */
    public void log(String username, String action, String status,
                    HttpServletRequest request, String details) {
        log(username, action, status, extractIp(request), details);
    }

    /**
     * Convenience overload without details.
     */
    public void log(String username, String action, String status,
                    HttpServletRequest request) {
        log(username, action, status, extractIp(request), null);
    }

    /**
     * Retrieve all audit logs (paginated, newest first). Admin use only.
     */
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Retrieve audit logs for a specific user.
     */
    public Page<AuditLog> getLogsByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username, pageable);
    }

    /**
     * Extract the real client IP, considering proxies (X-Forwarded-For).
     */
    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
