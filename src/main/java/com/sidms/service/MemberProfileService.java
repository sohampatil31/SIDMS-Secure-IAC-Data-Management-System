package com.sidms.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sidms.dto.MemberProfileRequest;
import com.sidms.dto.MemberProfileResponse;
import com.sidms.entity.MemberProfile;
import com.sidms.entity.Role;
import com.sidms.entity.User;
import com.sidms.exception.BadRequestException;
import com.sidms.exception.ResourceNotFoundException;
import com.sidms.repository.MemberProfileRepository;
import com.sidms.repository.UserRepository;
import com.sidms.util.EncryptionService;

/**
 * Service layer for MemberProfile CRUD operations.
 * Handles field-level encryption/decryption and enforces RBAC ownership rules:
 * <ul>
 *   <li>ADMIN: full access to all profiles</li>
 *   <li>MANAGER: access only to profiles assigned to them</li>
 *   <li>MEMBER: access only to their own profile</li>
 * </ul>
 */
@Service
public class MemberProfileService {

    private final MemberProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public MemberProfileService(MemberProfileRepository profileRepository,
                                UserRepository userRepository,
                                EncryptionService encryptionService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Create a new member profile. Only ADMINs can create profiles.
     */
    @Transactional
    public MemberProfileResponse createProfile(MemberProfileRequest request,
                                                String createdByUsername) {
        if (profileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("A profile with this email already exists");
        }

        MemberProfile profile = MemberProfile.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(encryptionService.encrypt(request.getPhoneNumber()))
                .address(encryptionService.encrypt(request.getAddress()))
                .resumeUrl(encryptionService.encrypt(request.getResumeUrl()))
                .governmentId(encryptionService.encrypt(request.getGovernmentId()))
                .createdBy(createdByUsername)
                .assignedManager(request.getAssignedManager())
                .userId(request.getUserId())
                .build();

        MemberProfile saved = profileRepository.save(profile);
        return toResponse(saved);
    }

    /**
     * Create a profile for the currently logged-in MEMBER.
     * Automatically links the profile to the user's ID.
     * Only one profile per user is allowed.
     */
    @Transactional
    public MemberProfileResponse createMyProfile(MemberProfileRequest request,
                                                  String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (profileRepository.existsByUserId(user.getId())) {
            throw new BadRequestException("Profile already exists");
        }

        MemberProfile profile = MemberProfile.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(encryptionService.encrypt(request.getPhoneNumber()))
                .address(encryptionService.encrypt(request.getAddress()))
                .resumeUrl(encryptionService.encrypt(request.getResumeUrl()))
                .governmentId(encryptionService.encrypt(request.getGovernmentId()))
                .createdBy(username)
                .userId(user.getId())
                .build();

        MemberProfile saved = profileRepository.save(profile);
        return toResponse(saved);
    }

    /**
     * Get the profile for the currently logged-in MEMBER.
     * Throws 404 if no profile exists yet.
     */
    @Transactional(readOnly = true)
    public MemberProfileResponse getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MemberProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        return toResponse(profile);
    }

    /**
     * Retrieve a single profile by ID, enforcing RBAC.
     */
    @Transactional(readOnly = true)
    public MemberProfileResponse getProfileById(Long id, String username, Role role) {
        MemberProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member profile not found with id: " + id));

        enforceAccess(profile, username, role);
        return toResponse(profile);
    }

    /**
     * List profiles based on role:
     * ADMIN → all, MANAGER → assigned, MEMBER → own only.
     */
    @Transactional(readOnly = true)
    public List<MemberProfileResponse> listProfiles(String username, Role role) {
        List<MemberProfile> profiles;

        switch (role) {
            case ROLE_ADMIN -> profiles = profileRepository.findAll();
            case ROLE_MANAGER -> profiles = profileRepository.findByAssignedManager(username);
            case ROLE_MEMBER -> {
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                profiles = profileRepository.findByUserId(user.getId())
                        .map(List::of)
                        .orElse(List.of());
            }
            default -> throw new AccessDeniedException("Insufficient permissions");
        }

        return profiles.stream().map(this::toResponse).toList();
    }

    /**
     * Update a profile, enforcing RBAC.
     */
    @Transactional
    public MemberProfileResponse updateProfile(Long id, MemberProfileRequest request,
                                                String username, Role role) {
        MemberProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member profile not found with id: " + id));

        enforceAccess(profile, username, role);

        profile.setFullName(request.getFullName());
        profile.setEmail(request.getEmail());
        profile.setPhoneNumber(encryptionService.encrypt(request.getPhoneNumber()));
        profile.setAddress(encryptionService.encrypt(request.getAddress()));
        profile.setResumeUrl(encryptionService.encrypt(request.getResumeUrl()));
        profile.setGovernmentId(encryptionService.encrypt(request.getGovernmentId()));

        // Only ADMIN can change assigned manager
        if (role == Role.ROLE_ADMIN && request.getAssignedManager() != null) {
            profile.setAssignedManager(request.getAssignedManager());
        }

        MemberProfile updated = profileRepository.save(profile);
        return toResponse(updated);
    }

    /**
     * Delete a profile. Admin only (enforced at controller via @PreAuthorize).
     */
    @Transactional
    public void deleteProfile(Long id) {
        if (!profileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Member profile not found with id: " + id);
        }
        profileRepository.deleteById(id);
    }

    // ---- Internal helpers ----

    /**
     * Enforce RBAC access rules on a profile.
     */
    private void enforceAccess(MemberProfile profile, String username, Role role) {
        switch (role) {
            case ROLE_ADMIN -> { /* full access */ }
            case ROLE_MANAGER -> {
                if (!username.equals(profile.getAssignedManager())) {
                    throw new AccessDeniedException(
                            "Managers can only access profiles assigned to them");
                }
            }
            case ROLE_MEMBER -> {
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if (profile.getUserId() == null || !profile.getUserId().equals(user.getId())) {
                    throw new AccessDeniedException(
                            "Members can only access their own profile");
                }
            }
            default -> throw new AccessDeniedException("Insufficient permissions");
        }
    }

    /**
     * Map entity to response DTO, decrypting sensitive fields.
     */
    private MemberProfileResponse toResponse(MemberProfile profile) {
        return MemberProfileResponse.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .phoneNumber(encryptionService.decrypt(profile.getPhoneNumber()))
                .address(encryptionService.decrypt(profile.getAddress()))
                .resumeUrl(encryptionService.decrypt(profile.getResumeUrl()))
                .governmentId(encryptionService.decrypt(profile.getGovernmentId()))
                .createdBy(profile.getCreatedBy())
                .assignedManager(profile.getAssignedManager())
                .userId(profile.getUserId())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
