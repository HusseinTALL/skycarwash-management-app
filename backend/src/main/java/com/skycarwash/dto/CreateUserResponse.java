package com.skycarwash.dto;

public record CreateUserResponse(
        UserResponse user,
        String       temporaryPassword
) {}
