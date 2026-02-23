package com.sidms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Safe projection of User entity — excludes password.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String username;
    private String role;
    private boolean mfaEnabled;
    private boolean enabled;
    private LocalDateTime createdAt;
}
