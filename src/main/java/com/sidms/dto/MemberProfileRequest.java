package com.sidms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating or updating a MemberProfile.
 * All fields arrive in plaintext and are encrypted at the service layer.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MemberProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    private String resumeUrl;

    private String governmentId;

    /** Username of the manager to assign (optional, set by admin). */
    private String assignedManager;

    /** User ID to link this profile to (optional). */
    private Long userId;
}
