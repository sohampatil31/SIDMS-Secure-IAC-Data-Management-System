package com.sidms.service;

import com.sidms.dto.UserResponse;
import com.sidms.entity.Role;
import com.sidms.entity.User;
import com.sidms.exception.BadRequestException;
import com.sidms.exception.ResourceNotFoundException;
import com.sidms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user management operations — listing users and assigning roles.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * List all users (admin only).
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get a single user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    /**
     * Assign a role to a user (admin only).
     *
     * @param userId  the user ID
     * @param roleStr the role string (e.g., "ROLE_MANAGER")
     */
    @Transactional
    public UserResponse assignRole(Long userId, String roleStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleStr
                    + ". Valid roles: ROLE_ADMIN, ROLE_MANAGER, ROLE_MEMBER");
        }

        user.setRole(role);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    // ---- Helper ----

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .mfaEnabled(user.isMfaEnabled())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
