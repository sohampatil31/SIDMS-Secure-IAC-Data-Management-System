package com.sidms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for assigning a role to a user (admin only).
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RoleAssignRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Role is required")
    private String role;
}
