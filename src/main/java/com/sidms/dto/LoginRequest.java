package com.sidms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Step 1 of authentication — username and password.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
