package com.uwazy.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be empty")
    private String email;
}
