package com.sidms.dto;

import lombok.*;

/**
 * Response returned after successful OTP verification.
 * Contains the JWT token and user role.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String tokenType;
    private String role;
    private String username;
    private long expiresIn;
}
