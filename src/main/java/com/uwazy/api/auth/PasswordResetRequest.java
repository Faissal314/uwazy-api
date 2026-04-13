package com.uwazy.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be empty")
    private String email;
}
