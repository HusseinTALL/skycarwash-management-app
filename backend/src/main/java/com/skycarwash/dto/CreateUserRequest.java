package com.skycarwash.dto;

import com.skycarwash.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20)  String phone,
        @NotNull                   User.Role role
) {}
