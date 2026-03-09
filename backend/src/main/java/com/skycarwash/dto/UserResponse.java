package com.skycarwash.dto;

import com.skycarwash.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long          id,
        String        name,
        String        phone,
        String        role,
        boolean       active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
