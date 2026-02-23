package com.sidms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for MemberProfile — contains decrypted data.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String resumeUrl;
    private String governmentId;
    private String createdBy;
    private String assignedManager;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
