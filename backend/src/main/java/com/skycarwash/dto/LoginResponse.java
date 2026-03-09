package com.skycarwash.dto;

public record LoginResponse(
        String token,
        String role,
        Long userId,
        String name
) {}
