package com.sidms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Step 2 of authentication — OTP verification.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OtpVerifyRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "OTP code is required")
    private String otp;
}
