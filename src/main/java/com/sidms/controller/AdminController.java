package com.sidms.controller;

import com.sidms.dto.RoleAssignRequest;
import com.sidms.dto.UserResponse;
import com.sidms.entity.AuditLog;
import com.sidms.service.AuditLogService;
import com.sidms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints for user management and audit log viewing.
 * All endpoints require ROLE_ADMIN (enforced at both SecurityFilterChain and @PreAuthorize level).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only: user management and audit logs")
public class AdminController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AdminController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    /**
     * List all users.
     */
    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Get a single user by ID.
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Assign a role to a user.
     */
    @PutMapping("/assign-role")
    @Operation(summary = "Assign a role to a user")
    public ResponseEntity<UserResponse> assignRole(
            @Valid @RequestBody RoleAssignRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        UserResponse response = userService.assignRole(request.getUserId(), request.getRole());
        auditLogService.log(auth.getName(), "ASSIGN_ROLE", "SUCCESS",
                httpRequest,
                "Assigned " + request.getRole() + " to userId: " + request.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * View audit logs (paginated).
     */
    @GetMapping("/audit-logs")
    @Operation(summary = "View audit logs (paginated)")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getAllLogs(pageable));
    }
}
