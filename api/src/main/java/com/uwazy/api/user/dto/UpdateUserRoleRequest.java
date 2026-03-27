package com.uwazy.api.user.dto;

import com.uwazy.api.user.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotNull(message = "Role cannot be null")
    private Role role;
}
