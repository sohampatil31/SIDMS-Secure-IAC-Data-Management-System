package com.sidms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sidms.dto.MemberProfileRequest;
import com.sidms.dto.MemberProfileResponse;
import com.sidms.entity.Role;
import com.sidms.entity.User;
import com.sidms.exception.ResourceNotFoundException;
import com.sidms.repository.UserRepository;
import com.sidms.service.AuditLogService;
import com.sidms.service.MemberProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Member profile endpoints — CRUD with RBAC enforcement.
 */
@RestController
@RequestMapping("/api/members")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Member Profiles", description = "CRUD operations on member profiles with role-based access")
public class MemberProfileController {

    private final MemberProfileService profileService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public MemberProfileController(MemberProfileService profileService,
                                   UserRepository userRepository,
                                   AuditLogService auditLogService) {
        this.profileService = profileService;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Create a new member profile (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a member profile (Admin only)")
    public ResponseEntity<MemberProfileResponse> createProfile(
            @Valid @RequestBody MemberProfileRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        MemberProfileResponse response = profileService.createProfile(request, auth.getName());
        auditLogService.log(auth.getName(), "CREATE_PROFILE", "SUCCESS",
                httpRequest, "Created profile for: " + request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create own profile (MEMBER only).
     * Username is extracted from the JWT — no user can create a profile for someone else.
     * Returns 400 if a profile already exists for this user.
     */
    @PostMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Create own profile (Member only)",
               description = "Automatically links the profile to the logged-in user. One profile per user.")
    public ResponseEntity<MemberProfileResponse> createMyProfile(
            @Valid @RequestBody MemberProfileRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        MemberProfileResponse response = profileService.createMyProfile(request, auth.getName());
        auditLogService.log(auth.getName(), "CREATE_PROFILE", "SUCCESS",
                httpRequest, "Member created own profile");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get own profile (MEMBER only).
     * Returns 404 if the member has not yet created a profile.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get own profile (Member only)",
               description = "Returns the logged-in member's profile. 404 if no profile exists.")
    public ResponseEntity<MemberProfileResponse> getMyProfile(
            Authentication auth,
            HttpServletRequest httpRequest) {

        MemberProfileResponse response = profileService.getMyProfile(auth.getName());
        auditLogService.log(auth.getName(), "VIEW_PROFILE", "SUCCESS",
                httpRequest, "Member viewed own profile");
        return ResponseEntity.ok(response);
    }

    /**
     * List member profiles based on the caller's role.
     */
    @GetMapping
    @Operation(summary = "List member profiles (role-filtered)")
    public ResponseEntity<List<MemberProfileResponse>> listProfiles(
            Authentication auth,
            HttpServletRequest httpRequest) {

        Role role = getUserRole(auth.getName());
        List<MemberProfileResponse> profiles = profileService.listProfiles(auth.getName(), role);
        auditLogService.log(auth.getName(), "LIST_PROFILES", "SUCCESS", httpRequest);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get a single member profile by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a member profile by ID")
    public ResponseEntity<MemberProfileResponse> getProfile(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        Role role = getUserRole(auth.getName());
        MemberProfileResponse response = profileService.getProfileById(id, auth.getName(), role);
        auditLogService.log(auth.getName(), "VIEW_PROFILE", "SUCCESS",
                httpRequest, "Viewed profile id: " + id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a member profile.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a member profile")
    public ResponseEntity<MemberProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody MemberProfileRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        Role role = getUserRole(auth.getName());
        MemberProfileResponse response = profileService.updateProfile(id, request,
                auth.getName(), role);
        auditLogService.log(auth.getName(), "UPDATE_PROFILE", "SUCCESS",
                httpRequest, "Updated profile id: " + id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a member profile (ADMIN only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a member profile (Admin only)")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest httpRequest) {

        profileService.deleteProfile(id);
        auditLogService.log(auth.getName(), "DELETE_PROFILE", "SUCCESS",
                httpRequest, "Deleted profile id: " + id);
        return ResponseEntity.noContent().build();
    }

    // ---- Helper ----

    private Role getUserRole(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getRole();
    }
}
